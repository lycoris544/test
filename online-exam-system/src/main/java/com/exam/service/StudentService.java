package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.BizException;
import com.exam.entity.Course;
import com.exam.entity.CourseEnrollment;
import com.exam.entity.ExamSession;
import com.exam.mapper.CourseEnrollmentMapper;
import com.exam.mapper.CourseMapper;
import com.exam.mapper.ExamSessionMapper;
import com.exam.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 学生端选课。
 *
 * <p>个人信息部分见 {@link ProfileService}，两个角色共用同一套接口。</p>
 *
 * <p>文档只提到学生有「所选课程」，没写选课动作由谁发起。这里补上学生自助选课，
 * 否则学生端永远没有课程可以考试。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final ExamSessionMapper sessionMapper;
    private final CourseService courseService;

    /**
     * 我的课程：已选课程 + 考试状态。
     *
     * <p>对应文档：「点击进入考试板块后，显示所选课程列表」
     * 「课程列表会保存考试状态。若已参加过该课程考试，旁边显示分数，
     * 且不能继续考试，除非点击重考」。状态由
     * {@code CourseEnrollmentMapper.xml} 里的 SQL 算出。</p>
     */
    public List<CourseEnrollment> myCourses() {
        UserContext.requireStudent();
        List<CourseEnrollment> courses =
                enrollmentMapper.selectStudentCourses(UserContext.currentUserId());
        // SQL 里分别算了进行中/已结束的记录数，「已考次数」在 Java 里加起来即可，
        // 比再写一个相关子查询更省事也更好读
        for (CourseEnrollment course : courses) {
            int ongoing = course.getOngoingCount() == null ? 0 : course.getOngoingCount();
            int finished = course.getFinishedCount() == null ? 0 : course.getFinishedCount();
            course.setAttemptCount(ongoing + finished);
        }
        return courses;
    }

    /** 还没选的课程，供选课页展示 */
    public List<Course> availableCourses(String keyword) {
        UserContext.requireStudent();
        Long studentId = UserContext.currentUserId();

        List<Course> all = courseService.page(1, 200, null, keyword, null).getRecords();
        List<Long> enrolledIds = enrollmentMapper.selectList(new LambdaQueryWrapper<CourseEnrollment>()
                        .eq(CourseEnrollment::getStudentId, studentId))
                .stream()
                .map(CourseEnrollment::getCourseId)
                .collect(Collectors.toList());

        return all.stream()
                .filter(c -> !enrolledIds.contains(c.getId()))
                .collect(Collectors.toList());
    }

    /** 选课 */
    @Transactional(rollbackFor = Exception.class)
    public void enroll(Long courseId) {
        UserContext.requireStudent();
        Long studentId = UserContext.currentUserId();

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BizException.notFound("课程不存在");
        }

        Long exists = enrollmentMapper.selectCount(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getStudentId, studentId)
                .eq(CourseEnrollment::getCourseId, courseId));
        if (exists != null && exists > 0) {
            throw BizException.badRequest("你已选择该课程");
        }

        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        enrollment.setCreateTime(LocalDateTime.now());
        enrollmentMapper.insert(enrollment);
        log.info("学生 {} 选了课程 {}", studentId, courseId);
    }

    /**
     * 退选课程。
     *
     * <p>已经考过试的课程不允许退选：成绩要留档，退课后教师端就查不到这个学生了，
     * 而文档要求教师能查看所有考生的成绩。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void drop(Long courseId) {
        UserContext.requireStudent();
        Long studentId = UserContext.currentUserId();

        List<ExamSession> sessions = sessionMapper.selectAttempts(studentId, courseId);
        if (!sessions.isEmpty()) {
            throw BizException.badRequest("该课程已有考试记录，无法退选");
        }

        int deleted = enrollmentMapper.delete(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getStudentId, studentId)
                .eq(CourseEnrollment::getCourseId, courseId));
        if (deleted == 0) {
            throw BizException.badRequest("你尚未选择该课程");
        }
        log.info("学生 {} 退选课程 {}", studentId, courseId);
    }
}
