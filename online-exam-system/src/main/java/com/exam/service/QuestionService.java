package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.BizException;
import com.exam.common.QuestionType;
import com.exam.dto.question.QuestionOption;
import com.exam.dto.question.QuestionRequest;
import com.exam.entity.Question;
import com.exam.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题库管理（教师端）。
 *
 * <p>对应文档：「点击进入考试课程后，显示该课程的所有考题。教师有权对考试题目进行增删改查。」</p>
 *
 * <p>所有写操作都先确认课程归属，避免越权改别人的题。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final CourseService courseService;

    /** 分页查询题目。教师端传自己的课程 ID，先校验归属 */
    public IPage<Question> page(long current, long size, Long courseId, Integer type, String keyword) {
        courseService.requireOwnedCourse(courseId);
        Page<Question> page = new Page<>(current, size);
        return questionMapper.selectQuestionPage(page, courseId, type, keyword);
    }

    /** 题目详情 */
    public Question detail(Long questionId) {
        Question question = requireExisting(questionId);
        courseService.requireOwnedCourse(question.getCourseId());
        return question;
    }

    /** 某课程的全部题目（含答案，仅教师可见） */
    public List<Question> listByCourse(Long courseId) {
        courseService.requireOwnedCourse(courseId);
        return questionMapper.selectByCourseId(courseId);
    }

    /** 新增题目 */
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long courseId, QuestionRequest request) {
        courseService.requireOwnedCourse(courseId);
        normalizeAndValidate(request);

        Question question = new Question();
        question.setCourseId(courseId);
        applyRequest(question, request);
        if (question.getSortOrder() == null) {
            Integer max = questionMapper.selectMaxSortOrder(courseId);
            question.setSortOrder((max == null ? 0 : max) + 1);
        }
        question.setCreateTime(LocalDateTime.now());
        question.setUpdateTime(LocalDateTime.now());

        questionMapper.insert(question);
        log.info("课程 {} 新增题目: id={}, type={}", courseId, question.getId(),
                QuestionType.nameOf(question.getType()));
        return question.getId();
    }

    /** 修改题目 */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long questionId, QuestionRequest request) {
        Question question = requireExisting(questionId);
        courseService.requireOwnedCourse(question.getCourseId());
        normalizeAndValidate(request);

        applyRequest(question, request);
        question.setUpdateTime(LocalDateTime.now());
        questionMapper.updateById(question);
    }

    /**
     * 删除题目。
     *
     * <p>已经交卷的历史成绩不受影响：判分结果（正确答案、得分）在交卷时
     * 已快照进 answer 表，这里删的只是题库里的题目本身。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long questionId) {
        Question question = requireExisting(questionId);
        courseService.requireOwnedCourse(question.getCourseId());
        questionMapper.deleteById(questionId);
        log.info("删除题目: id={}", questionId);
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    private Question requireExisting(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw BizException.notFound("题目不存在");
        }
        return question;
    }

    private void applyRequest(Question question, QuestionRequest request) {
        question.setType(request.getType());
        question.setContent(request.getContent());
        question.setOptions(request.getOptions());
        question.setAnswer(request.getAnswer());
        question.setScore(request.getScore());
        question.setAnalysis(request.getAnalysis());
        if (request.getSortOrder() != null) {
            question.setSortOrder(request.getSortOrder());
        }
    }

    /**
     * 按题型整理并校验请求。
     *
     * <p>这些规则依赖题型，Bean Validation 的注解表达不了，所以集中在这里。
     * 顺带把答案归一化（去空格、多选去重排序、判断题补默认选项），
     * 保证入库的数据格式统一，判分时不用再考虑各种写法。</p>
     */
    private void normalizeAndValidate(QuestionRequest request) {
        Integer type = request.getType();
        if (!QuestionType.isValid(type)) {
            throw BizException.badRequest("题型取值为 1~4");
        }

        if (type == QuestionType.FILL) {
            validateFill(request);
            return;
        }

        // 判断题的选项固定，教师端不传就自动补上
        if (type == QuestionType.JUDGE) {
            if (request.getOptions() == null || request.getOptions().size() != 2) {
                request.setOptions(Arrays.asList(
                        new QuestionOption(QuestionType.JUDGE_TRUE_OPTION, "正确"),
                        new QuestionOption(QuestionType.JUDGE_FALSE_OPTION, "错误")));
            }
            String answer = trimToNull(request.getAnswer());
            if (!QuestionType.JUDGE_TRUE_OPTION.equals(answer)
                    && !QuestionType.JUDGE_FALSE_OPTION.equals(answer)) {
                throw BizException.badRequest("判断题答案只能是 A（正确）或 B（错误）");
            }
            request.setAnswer(answer);
            return;
        }

        validateChoice(request, type);
    }

    /** 单选 / 多选 */
    private void validateChoice(QuestionRequest request, Integer type) {
        List<QuestionOption> options = request.getOptions();
        if (options == null || options.size() < 2) {
            throw BizException.badRequest("选择题至少需要 2 个选项");
        }

        Set<String> keys = new HashSet<>();
        for (QuestionOption option : options) {
            String key = trimToNull(option.getKey());
            if (key == null) {
                throw BizException.badRequest("选项标识不能为空");
            }
            // 选项标识统一成大写，避免前端传 a 后端存 A 导致答案匹配不上
            option.setKey(key.toUpperCase());
            if (!keys.add(option.getKey())) {
                throw BizException.badRequest("选项标识重复：" + option.getKey());
            }
        }

        String answer = trimToNull(request.getAnswer());
        if (answer == null) {
            throw BizException.badRequest("正确答案不能为空");
        }
        answer = answer.replace(" ", "").replace(",", "").toUpperCase();

        // 答案里的每个字符都必须是已定义的选项
        Set<String> answerKeys = new HashSet<>();
        for (char c : answer.toCharArray()) {
            String key = String.valueOf(c);
            if (!keys.contains(key)) {
                throw BizException.badRequest("答案 " + key + " 不在选项范围内");
            }
            answerKeys.add(key);
        }

        if (type == QuestionType.SINGLE && answerKeys.size() != 1) {
            throw BizException.badRequest("单选题只能有一个正确答案");
        }
        if (type == QuestionType.MULTIPLE && answerKeys.size() < 2) {
            throw BizException.badRequest("多选题至少要有两个正确答案，若只有一个请改用单选题");
        }

        // 多选答案按字母排序后存库，判分时可直接做字符串比较（"CA" 与 "AC" 视为相同）
        List<String> sorted = new ArrayList<>(answerKeys);
        sorted.sort(String::compareTo);
        request.setAnswer(String.join("", sorted));
        request.setOptions(options);
    }

    /** 填空：不校验选项，只要求答案非空 */
    private void validateFill(QuestionRequest request) {
        String answer = trimToNull(request.getAnswer());
        if (answer == null) {
            throw BizException.badRequest("正确答案不能为空");
        }
        // 填空题不需要选项，显式置空，避免前端误传后存进库
        request.setOptions(null);
        request.setAnswer(answer);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 供判分使用：拆出填空题的所有可接受答案 */
    public static List<String> splitFillAnswers(String answer) {
        if (!StringUtils.hasText(answer)) {
            return new ArrayList<>();
        }
        return Arrays.stream(answer.split("\\" + QuestionType.FILL_ANSWER_SEPARATOR))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
