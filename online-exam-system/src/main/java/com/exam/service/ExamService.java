package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.BizException;
import com.exam.common.QuestionType;
import com.exam.common.SessionStatus;
import com.exam.dto.course.StudentCourseDetail;
import com.exam.dto.exam.ExamPaper;
import com.exam.dto.exam.ExamQuestion;
import com.exam.dto.exam.ExamResult;
import com.exam.dto.exam.ExamResultItem;
import com.exam.dto.exam.SubmitExamRequest;
import com.exam.dto.question.QuestionOption;
import com.exam.entity.Answer;
import com.exam.entity.Course;
import com.exam.entity.CourseEnrollment;
import com.exam.entity.ExamSession;
import com.exam.entity.Question;
import com.exam.entity.User;
import com.exam.mapper.AnswerMapper;
import com.exam.mapper.CourseEnrollmentMapper;
import com.exam.mapper.CourseMapper;
import com.exam.mapper.ExamSessionMapper;
import com.exam.mapper.QuestionMapper;
import com.exam.mapper.UserMapper;
import com.exam.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 考试核心流程：开考 → 答题 → 交卷判分 → 查成绩 / 重考。
 *
 * <p>对应文档「考试板块」：「进入考试后，学生开始答题，最后点击提交试卷，
 * 答案上传并进行批改。服务端保存成绩并返回成绩显示在页面上。课程列表会保存考试状态。
 * 若已参加过该课程考试，旁边显示分数，且不能继续考试，除非点击重考。」</p>
 *
 * <h3>几处关键设计</h3>
 * <ul>
 *   <li><b>答案不下发</b>：试卷用 {@link ExamQuestion} 而不是 Question 实体，
 *       后者带 answer 字段，直接返回等于把答案送给学生。</li>
 *   <li><b>截止时间落库</b>：开考时把 deadline 算好存下来，之后教师改课程时长
 *       不会影响进行中的考试。</li>
 *   <li><b>判分快照</b>：正确答案和得分在交卷时写入 answer 表，
 *       教师之后改题目或删题目都不会改写历史成绩。</li>
 *   <li><b>超时不当作正常提交</b>：超过 deadline 交卷记 {@link SessionStatus#TIMEOUT}，
 *       并在结果里给出明确标记。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamSessionMapper sessionMapper;
    private final AnswerMapper answerMapper;
    private final QuestionMapper questionMapper;
    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final UserMapper userMapper;
    private final CourseService courseService;

    // ==================================================================
    // 学生端
    // ==================================================================

    /**
     * 开始考试。
     *
     * <p>如果存在未交卷的记录，直接返回该场考试继续作答（对应文档里
     * 「中途退出网页后重进仍可返回考试」的思路），而不是新开一场。</p>
     *
     * @param restart 传 true 表示这是「重考」按钮发起的。已考过的学生必须
     *                显式重考，避免误点进入考试把成绩覆盖掉
     */
    @Transactional(rollbackFor = Exception.class)
    public ExamPaper start(Long courseId, boolean restart) {
        UserContext.requireStudent();
        Long studentId = UserContext.currentUserId();

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BizException.notFound("课程不存在");
        }
        requireEnrolled(studentId, courseId);

        if (course.getExamEnabled() == null || course.getExamEnabled() != 1) {
            throw BizException.badRequest("该课程的考试尚未开放");
        }

        List<Question> questions = questionMapper.selectByCourseId(courseId);
        if (questions.isEmpty()) {
            throw BizException.badRequest("该课程还没有题目，无法开始考试");
        }

        List<ExamSession> history = sessionMapper.selectAttempts(studentId, courseId);

        // 有未交卷的：无论是不是重考请求，都继续这一场，避免同一课程并行多场考试
        ExamSession ongoing = history.stream()
                .filter(s -> SessionStatus.ONGOING == s.getStatus())
                .findFirst()
                .orElse(null);
        if (ongoing != null) {
            return buildPaper(ongoing, course, questions);
        }

        // 已考过：必须显式点重考
        boolean finishedBefore = history.stream().anyMatch(s -> SessionStatus.isFinished(s.getStatus()));
        if (finishedBefore && !restart) {
            throw BizException.badRequest("该课程已考过，如需重新考试请点击「重考」");
        }

        ExamSession session = createSession(studentId, course, history.size() + 1);
        log.info("学生 {} 开始考试 courseId={} attemptNo={}", studentId, courseId, session.getAttemptNo());
        return buildPaper(session, course, questions);
    }

    /**
     * 获取进行中的答卷（页面刷新后恢复）。
     *
     * <p>不返回已交卷的记录：成绩详情要走 {@link #result(Long)}，
     * 那里才带正确答案和解析。</p>
     */
    public ExamPaper currentPaper(Long courseId) {
        UserContext.requireStudent();
        Long studentId = UserContext.currentUserId();

        List<ExamSession> history = sessionMapper.selectAttempts(studentId, courseId);
        ExamSession ongoing = history.stream()
                .filter(s -> SessionStatus.ONGOING == s.getStatus())
                .findFirst()
                .orElseThrow(() -> BizException.badRequest("当前没有进行中的考试"));

        Course course = courseMapper.selectById(courseId);
        List<Question> questions = questionMapper.selectByCourseId(courseId);
        return buildPaper(ongoing, course, questions);
    }

    /**
     * 交卷并判分。
     *
     * <p>这是成绩的唯一写入点，因此把超时判断也放在这里：</p>
     * <ul>
     *   <li>已经交过卷 → 直接拒绝（防止重复提交刷分）</li>
     *   <li>提交时间已过 deadline → 仍然接受答案并判分，但状态记
     *       {@link SessionStatus#TIMEOUT}，结果里标记 {@code timeout = true}</li>
     * </ul>
     */
    @Transactional(rollbackFor = Exception.class)
    public ExamResult submit(Long sessionId, SubmitExamRequest request) {
        UserContext.requireStudent();
        Long studentId = UserContext.currentUserId();

        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw BizException.notFound("考试记录不存在");
        }
        if (!session.getStudentId().equals(studentId)) {
            throw BizException.forbidden("无权操作他人的考试记录");
        }
        if (SessionStatus.isFinished(session.getStatus())) {
            throw BizException.badRequest("该场考试已交卷，不能重复提交");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean timeout = session.getDeadline() != null && now.isAfter(session.getDeadline());

        List<Question> questions = questionMapper.selectByCourseId(session.getCourseId());
        if (questions.isEmpty()) {
            // 极端情况：教师把题目全删了。此时判 0 分，但仍要正常结算，
            // 否则这场考试会永远卡在「进行中」
            log.warn("交卷时课程 {} 已无题目，按 0 分结算 sessionId={}", session.getCourseId(), sessionId);
        }

        Map<Long, String> submitted = toAnswerMap(request);
        int totalScore = 0;
        int earnedScore = 0;
        int correctCount = 0;
        List<Answer> records = new ArrayList<>(questions.size());

        for (Question question : questions) {
            GradingService.GradeResult grade =
                    GradingService.grade(question, submitted.get(question.getId()));

            totalScore += grade.getFullScore();
            earnedScore += grade.getScore();
            if (grade.isCorrect()) {
                correctCount++;
            }
            records.add(toAnswerRecord(sessionId, question, grade));
        }

        if (!records.isEmpty()) {
            records.forEach(answerMapper::insert);
        }

        session.setScore(earnedScore);
        session.setTotalScore(totalScore);
        session.setSubmitTime(now);
        session.setStatus(timeout ? SessionStatus.TIMEOUT : SessionStatus.SUBMITTED);
        session.setUpdateTime(now);
        sessionMapper.updateById(session);

        log.info("学生 {} 交卷 sessionId={} 得分 {}/{} 超时={}",
                studentId, sessionId, earnedScore, totalScore, timeout);

        return buildResult(session, questions, records, timeout, correctCount);
    }

    /** 查某场考试的成绩详情（含逐题答案与解析） */
    public ExamResult result(Long sessionId) {
        UserContext.requireStudent();
        Long studentId = UserContext.currentUserId();

        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw BizException.notFound("考试记录不存在");
        }
        if (!session.getStudentId().equals(studentId)) {
            throw BizException.forbidden("无权查看他人的考试记录");
        }
        if (!SessionStatus.isFinished(session.getStatus())) {
            throw BizException.badRequest("该场考试尚未交卷");
        }

        List<Answer> records = answerMapper.selectBySessionId(sessionId);
        int correctCount = (int) records.stream()
                .filter(a -> a.getIsCorrect() != null && a.getIsCorrect() == 1)
                .count();
        return buildResult(session, null, records, SessionStatus.TIMEOUT == session.getStatus(), correctCount);
    }

    /** 学生的历史考试记录 */
    public IPage<ExamSession> history(long current, long size, Long courseId) {
        UserContext.requireStudent();
        Page<ExamSession> page = new Page<>(current, size);
        return sessionMapper.selectStudentHistory(page, UserContext.currentUserId(), courseId);
    }

    /**
     * 某学生在某课程下的全部考试记录。
     * 学生看自己的，教师看自己课程下的。
     */
    public List<ExamSession> attempts(Long studentId, Long courseId) {
        if (UserContext.isStudent() && !UserContext.currentUserId().equals(studentId)) {
            throw BizException.forbidden("无权查看他人的考试记录");
        }
        if (UserContext.isTeacher()) {
            // 教师只能看自己课程的学生
            courseService.requireOwnedCourse(courseId);
        }
        return sessionMapper.selectAttempts(studentId, courseId);
    }

    // ==================================================================
    // 教师端
    // ==================================================================

    /** 教师端「考试情况」：全部考生的成绩列表 */
    public IPage<ExamSession> scorePage(long current, long size, Long courseId, String keyword) {
        UserContext.requireTeacher();
        if (courseId != null) {
            courseService.requireOwnedCourse(courseId);
        }
        Page<ExamSession> page = new Page<>(current, size);
        // teacherId 限定只能看到自己课程的成绩
        return sessionMapper.selectScorePage(page, courseId, UserContext.currentUserId(), keyword);
    }

    /**
     * 教师端查看某考生在某课程下的考试情况。
     * 对应文档：「点击进入某考生的该课程考试情况，若该课程重考多次，展示每一次考试的成绩。」
     */
    public StudentCourseDetail studentCourseDetail(Long studentId, Long courseId) {
        Course course = courseService.requireOwnedCourse(courseId);

        User student = userMapper.selectById(studentId);
        if (student == null) {
            throw BizException.notFound("学生不存在");
        }

        List<ExamSession> attempts = sessionMapper.selectAttempts(studentId, courseId);

        StudentCourseDetail detail = new StudentCourseDetail();
        detail.setStudentId(studentId);
        detail.setStudentName(student.getRealName());
        detail.setStudentNo(student.getUserNo());
        detail.setDepartment(student.getDepartment());
        detail.setCourseId(courseId);
        detail.setCourseName(course.getName());
        detail.setCourseCode(course.getCode());
        detail.setAttemptCount(attempts.size());
        detail.setTotalScore(computePaperTotalScore(courseId));
        detail.setBestScore(attempts.stream()
                .filter(s -> SessionStatus.isFinished(s.getStatus()))
                .map(ExamSession::getScore)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null));
        detail.setAttempts(attempts);
        return detail;
    }

    /** 教师端查看某一场考试的逐题作答 */
    public List<Answer> sessionAnswers(Long sessionId) {
        UserContext.requireTeacher();
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw BizException.notFound("考试记录不存在");
        }
        courseService.requireOwnedCourse(session.getCourseId());
        return answerMapper.selectBySessionId(sessionId);
    }

    // ==================================================================
    // 内部方法
    // ==================================================================

    private ExamSession createSession(Long studentId, Course course, int attemptNo) {
        LocalDateTime now = LocalDateTime.now();
        int minutes = course.getDurationMinutes() == null ? 60 : course.getDurationMinutes();

        ExamSession session = new ExamSession();
        session.setCourseId(course.getId());
        session.setStudentId(studentId);
        session.setAttemptNo(attemptNo);
        session.setStatus(SessionStatus.ONGOING);
        session.setStartTime(now);
        session.setDeadline(now.plusMinutes(minutes));
        session.setCreateTime(now);
        session.setUpdateTime(now);
        sessionMapper.insert(session);
        return session;
    }

    /** 组装试卷（不含答案） */
    private ExamPaper buildPaper(ExamSession session, Course course, List<Question> questions) {
        List<ExamQuestion> items = new ArrayList<>(questions.size());
        int totalScore = 0;
        int number = 1;
        for (Question question : questions) {
            ExamQuestion item = toExamQuestion(question, number++);
            items.add(item);
            totalScore += item.getScore() == null ? 0 : item.getScore();
        }

        ExamPaper paper = new ExamPaper();
        paper.setSessionId(session.getId());
        paper.setCourseId(course.getId());
        paper.setCourseName(course.getName());
        paper.setCourseCode(course.getCode());
        paper.setAttemptNo(session.getAttemptNo());
        paper.setTotalScore(totalScore);
        paper.setQuestionCount(items.size());
        paper.setStartTime(session.getStartTime());
        paper.setDeadline(session.getDeadline());
        paper.setRemainingSeconds(remainingSeconds(session.getDeadline()));
        paper.setQuestions(items);
        return paper;
    }

    private ExamQuestion toExamQuestion(Question question, int number) {
        ExamQuestion item = new ExamQuestion();
        item.setId(question.getId());
        item.setNumber(number);
        item.setType(question.getType());
        item.setTypeName(QuestionType.nameOf(question.getType()));
        item.setContent(question.getContent());
        item.setOptions(question.getOptions());
        item.setScore(question.getScore());
        return item;
    }

    /** 剩余秒数；已过期返回 0，前端据此直接提示已超时 */
    private long remainingSeconds(LocalDateTime deadline) {
        if (deadline == null) {
            return 0L;
        }
        long seconds = Duration.between(LocalDateTime.now(), deadline).getSeconds();
        return Math.max(seconds, 0L);
    }

    /** 把提交的作答列表转成 questionId -> 答案 的映射 */
    private Map<Long, String> toAnswerMap(SubmitExamRequest request) {
        if (request == null || request.getAnswers() == null) {
            return Collections.emptyMap();
        }
        Map<Long, String> map = new HashMap<>();
        for (SubmitExamRequest.SubmitAnswer item : request.getAnswers()) {
            if (item != null && item.getQuestionId() != null) {
                // 同一题重复提交时保留第一条，避免前端出错导致后一条空值覆盖前面的作答
                map.putIfAbsent(item.getQuestionId(), item.getUserAnswer());
            }
        }
        return map;
    }

    private Answer toAnswerRecord(Long sessionId, Question question, GradingService.GradeResult grade) {
        Answer answer = new Answer();
        answer.setSessionId(sessionId);
        answer.setQuestionId(question.getId());
        answer.setUserAnswer(grade.getUserAnswer());
        answer.setCorrectAnswer(grade.getCorrectAnswer());
        answer.setIsCorrect(grade.isCorrect() ? 1 : 0);
        answer.setScore(grade.getScore());
        answer.setFullScore(grade.getFullScore());
        return answer;
    }

    /**
     * 组装判分结果。
     *
     * @param questions     开考前查出的题目，交卷路径传入；查成绩路径传 null，
     *                      此时题目信息从 answer 表的 join 结果里取
     * @param records       作答明细
     * @param timeout       是否超时交卷
     * @param correctCount  答对题数
     */
    private ExamResult buildResult(ExamSession session, List<Question> questions,
                                   List<Answer> records, boolean timeout, int correctCount) {
        // 交卷路径：records 是我们刚构造的，还没有题目正文，
        // 用 questions 补齐；查成绩路径：records 已经带好题目信息
        Map<Long, Question> questionMap = questions == null
                ? Collections.emptyMap()
                : questions.stream().collect(Collectors.toMap(Question::getId, Function.identity()));

        List<ExamResultItem> items = new ArrayList<>(records.size());
        int number = 1;
        for (Answer record : records) {
            Question question = questionMap.get(record.getQuestionId());
            items.add(toResultItem(record, question, number++));
        }

        Course course = courseMapper.selectById(session.getCourseId());

        ExamResult result = new ExamResult();
        result.setSessionId(session.getId());
        result.setCourseId(session.getCourseId());
        result.setCourseName(course == null ? null : course.getName());
        result.setAttemptNo(session.getAttemptNo());
        result.setScore(session.getScore());
        result.setTotalScore(session.getTotalScore());
        result.setCorrectCount(correctCount);
        result.setQuestionCount(items.size());
        result.setAccuracy(accuracy(session.getScore(), session.getTotalScore()));
        result.setStartTime(session.getStartTime());
        result.setSubmitTime(session.getSubmitTime());
        result.setStatus(session.getStatus());
        result.setStatusName(SessionStatus.nameOf(session.getStatus()));
        result.setTimeout(timeout);
        result.setItems(items);
        return result;
    }

    private ExamResultItem toResultItem(Answer record, Question question, int number) {
        // 两条调用路径的题目信息来源不同，这里统一取一次：
        //  - 交卷路径：题目来自 questions，而 record 是刚构造的，没有题干
        //  - 查成绩路径：questions 传的是 null，题目信息来自 selectBySessionId 的 join，
        //    题目被教师删掉时 join 不上，字段全为 null
        Integer type = question != null ? question.getType() : record.getType();
        String content = question != null ? question.getContent() : record.getContent();
        List<QuestionOption> options = question != null ? question.getOptions() : record.getOptions();
        String analysis = question != null ? question.getAnalysis() : record.getAnalysis();

        ExamResultItem item = new ExamResultItem();
        item.setQuestionId(record.getQuestionId());
        item.setNumber(number);
        item.setUserAnswer(record.getUserAnswer());
        item.setCorrectAnswer(record.getCorrectAnswer());
        item.setCorrect(record.getIsCorrect() != null && record.getIsCorrect() == 1);
        item.setScore(record.getScore());
        item.setFullScore(record.getFullScore());

        item.setType(type);
        item.setTypeName(type == null ? null : QuestionType.nameOf(type));
        item.setContent(content);
        item.setOptions(options);
        item.setAnalysis(analysis);
        // 题干为空 = LEFT JOIN 没匹配上，说明题目已被教师删除。
        // 得分和学生的作答都在 answer 表里，不受影响，只是没法再展示题干和选项
        item.setQuestionDeleted(content == null);
        item.setUserAnswerText(describeAnswer(record.getUserAnswer(), type, options));
        item.setCorrectAnswerText(describeAnswer(record.getCorrectAnswer(), type, options));
        return item;
    }

    /**
     * 把选项 key 翻译成可读文本，方便前端直接展示。
     * 例如 {@code "AC"} + 选项列表 → {@code "A. 选项一；C. 选项三"}。
     * 填空题原样返回；题目已删除导致选项缺失时也原样返回。
     */
    private String describeAnswer(String answer, Integer type, List<QuestionOption> options) {
        if (!StringUtils.hasText(answer)) {
            return null;
        }
        if (type == null || !QuestionType.isChoice(type) || options == null || options.isEmpty()) {
            return answer;
        }
        Map<String, String> optionMap = options.stream()
                .filter(o -> o.getKey() != null)
                .collect(Collectors.toMap(QuestionOption::getKey, QuestionOption::getContent, (a, b) -> a));

        List<String> parts = new ArrayList<>();
        for (char c : answer.toCharArray()) {
            String key = String.valueOf(c);
            String content = optionMap.get(key);
            parts.add(content == null ? key : key + ". " + content);
        }
        return String.join("；", parts);
    }

    /** 正确率（百分数，保留两位小数）；总分 0 时返回 0 */
    private Double accuracy(Integer score, Integer total) {
        if (score == null || total == null || total <= 0) {
            return 0.0;
        }
        return Math.round(score * 10000.0 / total) / 100.0;
    }

    private void requireEnrolled(Long studentId, Long courseId) {
        Long count = enrollmentMapper.selectCount(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getStudentId, studentId)
                .eq(CourseEnrollment::getCourseId, courseId));
        if (count == null || count == 0) {
            throw BizException.forbidden("你尚未选择该课程，请先选课");
        }
    }

    /** 课程当前题目总分 */
    private Integer computePaperTotalScore(Long courseId) {
        List<Question> questions = questionMapper.selectByCourseId(courseId);
        return questions.stream()
                .mapToInt(q -> q.getScore() == null ? 0 : q.getScore())
                .sum();
    }
}
