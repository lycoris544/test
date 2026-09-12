package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.BizException;
import com.exam.dto.course.CourseRequest;
import com.exam.entity.Course;
import com.exam.entity.CourseEnrollment;
import com.exam.entity.Question;
import com.exam.mapper.CourseEnrollmentMapper;
import com.exam.mapper.CourseMapper;
import com.exam.mapper.QuestionMapper;
import com.exam.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程管理（教师端）。
 *
 * <p>核心规则：教师只能操作自己的课程。每个写操作都先过 {@link #requireOwnedCourse}，
 * 防止教师 A 通过改 URL 里的 courseId 去改教师 B 的题库。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final QuestionMapper questionMapper;
    private final CourseEnrollmentMapper enrollmentMapper;

    /**
     * 分页查询课程。
     *
     * @param teacherId 非空时只查该教师的课程；教师端传自己的 ID，学生端传 null 看全部
     */
    public IPage<Course> page(long current, long size, Long teacherId, String keyword, Integer examEnabled) {
        Page<Course> page = new Page<>(current, size);
        return courseMapper.selectCoursePage(page, teacherId, keyword, examEnabled);
    }

    /** 课程详情。学生也能看，因此不校验归属 */
    public Course detail(Long courseId) {
        Course course = courseMapper.selectCourseDetail(courseId);
        if (course == null) {
            throw BizException.notFound("课程不存在");
        }
        return course;
    }

    /** 当前教师在教的全部课程（用于「个人信息」里的教授课程一栏） */
    public List<Course> listByTeacher(Long teacherId) {
        Page<Course> page = new Page<>(1, 200);
        return courseMapper.selectCoursePage(page, teacherId, null, null).getRecords();
    }

    /** 新增课程，授课教师为当前登录教师 */
    @Transactional(rollbackFor = Exception.class)
    public Long create(CourseRequest request) {
        UserContext.requireTeacher();

        Long sameCode = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .eq(Course::getCode, request.getCode()));
        if (sameCode != null && sameCode > 0) {
            throw BizException.badRequest("课程编码已存在：" + request.getCode());
        }

        Course course = new Course();
        course.setName(request.getName());
        course.setCode(request.getCode());
        course.setDescription(request.getDescription());
        course.setDurationMinutes(request.getDurationMinutes());
        // 不传默认关闭考试，教师录完题目后再手动开放
        course.setExamEnabled(request.getExamEnabled() == null ? 0 : request.getExamEnabled());
        course.setTeacherId(UserContext.currentUserId());
        course.setCreateTime(LocalDateTime.now());
        course.setUpdateTime(LocalDateTime.now());

        courseMapper.insert(course);
        log.info("教师 {} 新建课程: {}", UserContext.currentUserId(), course.getName());
        return course.getId();
    }

    /** 修改课程 */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long courseId, CourseRequest request) {
        Course course = requireOwnedCourse(courseId);

        Long sameCode = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .eq(Course::getCode, request.getCode())
                .ne(Course::getId, courseId));
        if (sameCode != null && sameCode > 0) {
            throw BizException.badRequest("课程编码已被其他课程占用：" + request.getCode());
        }

        course.setName(request.getName());
        course.setCode(request.getCode());
        course.setDescription(request.getDescription());
        course.setDurationMinutes(request.getDurationMinutes());
        if (request.getExamEnabled() != null) {
            course.setExamEnabled(request.getExamEnabled());
        }
        course.setUpdateTime(LocalDateTime.now());
        courseMapper.updateById(course);
    }

    /** 开放 / 关闭考试 */
    @Transactional(rollbackFor = Exception.class)
    public void updateExamEnabled(Long courseId, Integer examEnabled) {
        Course course = requireOwnedCourse(courseId);
        course.setExamEnabled(examEnabled == null ? 0 : examEnabled);
        course.setUpdateTime(LocalDateTime.now());
        courseMapper.updateById(course);
    }

    /**
     * 删除课程。
     *
     * <p>已有学生选课或已产生考试成绩时拒绝删除。这里没有做级联删除：
     * 成绩是考核结果，不能因为教师删课程就消失。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long courseId) {
        Course course = requireOwnedCourse(courseId);

        Long enrolled = enrollmentMapper.selectCount(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getCourseId, courseId));
        if (enrolled != null && enrolled > 0) {
            throw BizException.badRequest(
                    "该课程已有 " + enrolled + " 名学生选课，无法删除。如需停止考试请改为「关闭考试」");
        }

        // 没有学生选课时，题目可以一并清掉
        questionMapper.delete(new LambdaQueryWrapper<Question>().eq(Question::getCourseId, courseId));
        courseMapper.deleteById(course.getId());
        log.info("教师 {} 删除课程: {}", UserContext.currentUserId(), course.getName());
    }

    /** 查看某课程的选课名单 */
    public List<CourseEnrollment> listStudents(Long courseId) {
        requireOwnedCourse(courseId);
        return enrollmentMapper.selectCourseStudents(courseId);
    }

    /**
     * 校验课程存在且归当前教师所有。
     *
     * <p>公开给 Controller 使用：像「查看课程统计」「查看某考生成绩」这类接口，
     * 本身不需要返回课程数据，但必须先过这道权限检查。</p>
     *
     * @return 课程实体，省得调用方再查一次
     */
    public Course requireOwnedCourse(Long courseId) {
        UserContext.requireTeacher();
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BizException.notFound("课程不存在");
        }
        if (!course.getTeacherId().equals(UserContext.currentUserId())) {
            // 注意这里返回 403 而不是 404：课程确实存在，只是不属于你
            throw BizException.forbidden("无权操作其他教师的课程");
        }
        return course;
    }
}
