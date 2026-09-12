package com.exam.service;

import com.exam.common.BizException;
import com.exam.common.enums.Role;
import com.exam.dto.user.ProfileResponse;
import com.exam.dto.user.ProfileUpdateRequest;
import com.exam.entity.Course;
import com.exam.entity.CourseEnrollment;
import com.exam.entity.User;
import com.exam.mapper.CourseEnrollmentMapper;
import com.exam.mapper.UserMapper;
import com.exam.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 个人信息。教师端和学生端共用同一个接口，按角色填充差异字段。
 *
 * <p>文档两个界面各有一节「个人信息板块」：</p>
 * <ul>
 *   <li>教师：姓名、性别、手机号、工号、所在学院、教授课程</li>
 *   <li>学生：姓名、性别、手机号、学号、所在学院、所选课程（仅能修改和保存）</li>
 * </ul>
 * <p>字段高度重合，只是最后一项不同，所以合成一个服务，避免两份几乎相同的代码。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserMapper userMapper;
    private final CourseService courseService;
    private final CourseEnrollmentMapper enrollmentMapper;

    /** 查看当前登录用户的个人信息 */
    public ProfileResponse currentProfile() {
        User user = requireCurrentUser();
        return build(user);
    }

    /**
     * 保存个人信息。
     *
     * <p>可改字段见 {@link ProfileUpdateRequest}：只有资料字段，
     * 不含用户名 / 工号学号 / 角色。文档要求的「修改和保存」指的正是这些资料。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ProfileResponse updateProfile(ProfileUpdateRequest request) {
        User user = requireCurrentUser();

        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName().trim());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }
        // 职称只有教师有，学生端传了忽略
        if (user.getRole() != null && user.getRole() == Role.TEACHER && request.getTitle() != null) {
            user.setTitle(request.getTitle());
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户 {} 更新了个人信息", user.getId());
        return build(user);
    }

    // ------------------------------------------------------------------

    private ProfileResponse build(User user) {
        ProfileResponse response = new ProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setGender(user.getGender());
        response.setPhone(user.getPhone());
        response.setUserNo(user.getUserNo());
        response.setDepartment(user.getDepartment());
        response.setRole(user.getRole());
        response.setRoleName(Role.nameOf(user.getRole()));

        if (user.getRole() != null && user.getRole() == Role.TEACHER) {
            response.setTitle(user.getTitle());
            // 「教授课程」：反查该教师名下的课程，而不是在用户表存一串课程名
            response.setTeachingCourses(courseService.listByTeacher(user.getId()));
        } else {
            // 「所选课程」：从选课关系里取
            List<CourseEnrollment> enrollments = enrollmentMapper.selectStudentCourses(user.getId());
            response.setEnrolledCourses(enrollments.stream()
                    .map(this::toCourse)
                    .collect(Collectors.toList()));
        }
        return response;
    }

    private Course toCourse(CourseEnrollment enrollment) {
        Course course = new Course();
        course.setId(enrollment.getCourseId());
        course.setName(enrollment.getCourseName());
        course.setCode(enrollment.getCourseCode());
        course.setTeacherName(enrollment.getTeacherName());
        course.setDurationMinutes(enrollment.getDurationMinutes());
        course.setExamEnabled(enrollment.getExamEnabled());
        course.setQuestionCount(enrollment.getQuestionCount());
        course.setTotalScore(enrollment.getTotalScore());
        return course;
    }

    private User requireCurrentUser() {
        User user = userMapper.selectById(UserContext.currentUserId());
        if (user == null) {
            throw BizException.unauthorized("用户不存在，请重新登录");
        }
        return user;
    }
}
