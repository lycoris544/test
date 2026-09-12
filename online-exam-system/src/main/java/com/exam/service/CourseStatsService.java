package com.exam.service;

import com.exam.common.SessionStatus;
import com.exam.dto.course.CourseStats;
import com.exam.entity.Course;
import com.exam.entity.CourseEnrollment;
import com.exam.entity.ExamSession;
import com.exam.mapper.CourseEnrollmentMapper;
import com.exam.mapper.ExamSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 教师端「考试情况」的统计。
 *
 * <p>统计口径集中在 {@link #PASS_RATIO} 这一处，改及格线只需要动这里。</p>
 */
@Service
@RequiredArgsConstructor
public class CourseStatsService {

    /** 及格线：满分的一定比例 */
    private static final double PASS_RATIO = 0.6;

    private final ExamSessionMapper sessionMapper;
    private final CourseEnrollmentMapper enrollmentMapper;

    /**
     * 汇总某课程的考试情况。
     *
     * @param course 已确认归属的课程（由调用方做权限校验）
     */
    public CourseStats summarize(Course course) {
        Long courseId = course.getId();

        List<CourseEnrollment> enrollments = enrollmentMapper.selectCourseStudents(courseId);
        List<ExamSession> allSessions = sessionMapper.selectByCourseId(courseId);

        // 只有已结束的考试才算成绩，进行中的不计入平均分
        List<ExamSession> finished = allSessions.stream()
                .filter(s -> SessionStatus.isFinished(s.getStatus()))
                .collect(Collectors.toList());

        // 按学生分组，便于取每人最新一次成绩
        Map<Long, List<ExamSession>> byStudent = finished.stream()
                .collect(Collectors.groupingBy(ExamSession::getStudentId));

        CourseStats stats = new CourseStats();
        stats.setCourseId(courseId);
        stats.setCourseName(course.getName());
        stats.setCourseCode(course.getCode());
        stats.setTotalScore(totalScoreOf(finished));
        stats.setStudentCount(enrollments.size());
        stats.setExaminedCount(byStudent.size());
        stats.setNotExaminedCount(Math.max(enrollments.size() - byStudent.size(), 0));
        stats.setAttemptCount(finished.size());

        List<Integer> scores = finished.stream()
                .map(ExamSession::getScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (scores.isEmpty()) {
            // 无人考试时这几个统计量没有意义，保持 null，前端显示「-」
            stats.setAverageScore(null);
            stats.setHighestScore(null);
            stats.setLowestScore(null);
            stats.setPassRate(null);
        } else {
            double average = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
            stats.setAverageScore(Math.round(average * 10.0) / 10.0);
            stats.setHighestScore(scores.stream().max(Integer::compareTo).orElse(null));
            stats.setLowestScore(scores.stream().min(Integer::compareTo).orElse(null));

            Integer full = stats.getTotalScore();
            if (full != null && full > 0) {
                double passLine = full * PASS_RATIO;
                long passed = scores.stream().filter(s -> s >= passLine).count();
                stats.setPassRate(Math.round(passed * 1000.0 / scores.size()) / 10.0);
            }
        }

        stats.setStudents(buildStudentRows(enrollments, byStudent));
        return stats;
    }

    /**
     * 生成「每个学生一行」的成绩列表。
     *
     * <p>没参加过考试的学生也要出现在列表里——教师需要看到谁还没考，
     * 这类行用 {@link SessionStatus#NOT_TAKEN} 标记，分数留空。</p>
     */
    private List<ExamSession> buildStudentRows(List<CourseEnrollment> enrollments,
                                               Map<Long, List<ExamSession>> byStudent) {
        List<ExamSession> rows = new ArrayList<>(enrollments.size());
        for (CourseEnrollment enrollment : enrollments) {
            Long studentId = enrollment.getStudentId();
            List<ExamSession> sessions = byStudent.get(studentId);

            // latestAttempt 会新建对象返回，不会改动 byStudent 里缓存的实体
            rows.add(latestAttempt(sessions, studentId, enrollment));
        }
        rows.sort(Comparator.comparing(ExamSession::getStudentNo,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return rows;
    }

    private ExamSession latestAttempt(List<ExamSession> sessions, Long studentId,
                                      CourseEnrollment enrollment) {
        if (sessions == null || sessions.isEmpty()) {
            ExamSession row = new ExamSession();
            row.setCourseId(enrollment.getCourseId());
            row.setStudentId(studentId);
            row.setStudentName(enrollment.getStudentName());
            row.setStudentNo(enrollment.getStudentNo());
            row.setDepartment(enrollment.getDepartment());
            row.setAttemptNo(0);
            row.setStatus(SessionStatus.NOT_TAKEN);
            // 该生在这门课的考试次数（这里必为 0，但保留字段便于前端统一渲染）
            row.setScore(null);
            return row;
        }

        // 最新一次：attempt_no 最大的那条。复制一份，避免把统计结果写回数据库实体
        Optional<ExamSession> latest = sessions.stream()
                .max(Comparator.comparing(ExamSession::getAttemptNo,
                        Comparator.nullsFirst(Comparator.naturalOrder())));

        ExamSession source = latest.get();
        ExamSession row = new ExamSession();
        row.setId(source.getId());
        row.setCourseId(source.getCourseId());
        row.setStudentId(source.getStudentId());
        row.setAttemptNo(source.getAttemptNo());
        row.setStatus(source.getStatus());
        row.setStartTime(source.getStartTime());
        row.setSubmitTime(source.getSubmitTime());
        row.setScore(source.getScore());
        row.setTotalScore(source.getTotalScore());
        row.setStudentName(enrollment.getStudentName());
        row.setStudentNo(enrollment.getStudentNo());
        row.setDepartment(enrollment.getDepartment());
        return row;
    }

    /** 试卷总分：取已结束记录里的总分（交卷时快照下来的），没有则取 0 */
    private Integer totalScoreOf(List<ExamSession> finished) {
        return finished.stream()
                .map(ExamSession::getTotalScore)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(0);
    }
}
