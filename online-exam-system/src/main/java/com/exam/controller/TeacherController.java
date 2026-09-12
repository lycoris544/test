package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.course.CourseRequest;
import com.exam.dto.course.CourseStats;
import com.exam.dto.course.StudentCourseDetail;
import com.exam.dto.question.QuestionRequest;
import com.exam.entity.Answer;
import com.exam.entity.Course;
import com.exam.entity.CourseEnrollment;
import com.exam.entity.ExamSession;
import com.exam.entity.Question;
import com.exam.security.UserContext;
import com.exam.service.CourseService;
import com.exam.service.CourseStatsService;
import com.exam.service.ExamService;
import com.exam.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 教师端接口。
 *
 * <p>整个 {@code /teacher/**} 路径在 {@code SecurityConfig} 里已被限定为
 * ROLE_TEACHER，因此这里不再逐方法写权限注解；Service 层还会再校验一次
 * 「课程是否属于当前教师」，防止横向越权。</p>
 *
 * <p>对应文档两个板块：</p>
 * <ul>
 *   <li><b>考试课程列表板块</b>——课程与题目的增删改查</li>
 *   <li><b>考试情况板块</b>——成绩列表与单个考生的历次成绩</li>
 * </ul>
 */
@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final CourseService courseService;
    private final QuestionService questionService;
    private final ExamService examService;
    private final CourseStatsService courseStatsService;

    // ==================================================================
    // 考试课程列表板块
    // ==================================================================

    /** 我的课程列表 */
    @GetMapping("/courses")
    public Result<PageResult<Course>> listCourses(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer examEnabled) {
        return Result.ok(PageResult.of(courseService.page(
                current, size, UserContext.currentUserId(), keyword, examEnabled)));
    }

    /** 课程详情 */
    @GetMapping("/courses/{courseId}")
    public Result<Course> courseDetail(@PathVariable Long courseId) {
        courseService.requireOwnedCourse(courseId);
        return Result.ok(courseService.detail(courseId));
    }

    /** 新建课程 */
    @PostMapping("/courses")
    public Result<Map<String, Long>> createCourse(@Valid @RequestBody CourseRequest request) {
        Long courseId = courseService.create(request);
        return Result.ok("课程创建成功", Collections.singletonMap("courseId", courseId));
    }

    /** 修改课程 */
    @PutMapping("/courses/{courseId}")
    public Result<Void> updateCourse(@PathVariable Long courseId,
                                     @Valid @RequestBody CourseRequest request) {
        courseService.update(courseId, request);
        return Result.ok("保存成功", null);
    }

    /** 开放 / 关闭考试。学生只有在开放状态下才能开考 */
    @PutMapping("/courses/{courseId}/exam-enabled")
    public Result<Void> updateExamEnabled(@PathVariable Long courseId,
                                          @RequestParam Integer examEnabled) {
        courseService.updateExamEnabled(courseId, examEnabled);
        return Result.ok(examEnabled != null && examEnabled == 1 ? "考试已开放" : "考试已关闭", null);
    }

    /** 删除课程。已有学生选课时会被拒绝 */
    @DeleteMapping("/courses/{courseId}")
    public Result<Void> deleteCourse(@PathVariable Long courseId) {
        courseService.delete(courseId);
        return Result.ok("删除成功", null);
    }

    /** 课程的选课名单 */
    @GetMapping("/courses/{courseId}/students")
    public Result<List<CourseEnrollment>> listCourseStudents(@PathVariable Long courseId) {
        return Result.ok(courseService.listStudents(courseId));
    }

    // ==================================================================
    // 题目管理（文档：教师有权对考试题目进行增删改查）
    // ==================================================================

    /** 分页查询某课程的题目 */
    @GetMapping("/courses/{courseId}/questions")
    public Result<PageResult<Question>> listQuestions(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String keyword) {
        return Result.ok(PageResult.of(questionService.page(current, size, courseId, type, keyword)));
    }

    /** 某课程的全部题目（不分页，组卷预览用） */
    @GetMapping("/courses/{courseId}/questions/all")
    public Result<List<Question>> listAllQuestions(@PathVariable Long courseId) {
        return Result.ok(questionService.listByCourse(courseId));
    }

    /** 题目详情 */
    @GetMapping("/questions/{questionId}")
    public Result<Question> questionDetail(@PathVariable Long questionId) {
        return Result.ok(questionService.detail(questionId));
    }

    /** 新增题目 */
    @PostMapping("/courses/{courseId}/questions")
    public Result<Map<String, Long>> createQuestion(@PathVariable Long courseId,
                                                    @Valid @RequestBody QuestionRequest request) {
        Long questionId = questionService.create(courseId, request);
        return Result.ok("题目新增成功", Collections.singletonMap("questionId", questionId));
    }

    /** 修改题目 */
    @PutMapping("/questions/{questionId}")
    public Result<Void> updateQuestion(@PathVariable Long questionId,
                                       @Valid @RequestBody QuestionRequest request) {
        questionService.update(questionId, request);
        return Result.ok("保存成功", null);
    }

    /** 删除题目。已交卷的历史成绩不受影响 */
    @DeleteMapping("/questions/{questionId}")
    public Result<Void> deleteQuestion(@PathVariable Long questionId) {
        questionService.delete(questionId);
        return Result.ok("删除成功", null);
    }

    // ==================================================================
    // 考试情况板块
    // ==================================================================

    /**
     * 所有考生的成绩列表。
     * 文档：「列出所有考生的考试成绩相关信息（姓名、考试科目、成绩、考试时间等）」
     */
    @GetMapping("/scores")
    public Result<PageResult<ExamSession>> listScores(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(PageResult.of(examService.scorePage(current, size, courseId, keyword)));
    }

    /** 某门课的考试情况汇总（平均分、及格率、每个学生的最新成绩） */
    @GetMapping("/courses/{courseId}/stats")
    public Result<CourseStats> courseStats(@PathVariable Long courseId) {
        Course course = courseService.requireOwnedCourse(courseId);
        return Result.ok(courseStatsService.summarize(course));
    }

    /**
     * 某个学生在该课程下的历次考试。
     * 文档：「点击进入某考生的该课程考试情况，若该课程重考多次，展示每一次考试的成绩」
     */
    @GetMapping("/students/{studentId}/courses/{courseId}/attempts")
    public Result<StudentCourseDetail> studentCourseDetail(@PathVariable Long studentId,
                                                           @PathVariable Long courseId) {
        return Result.ok(examService.studentCourseDetail(studentId, courseId));
    }

    /** 某一场考试的逐题作答明细（含正确答案与学生答案） */
    @GetMapping("/sessions/{sessionId}/answers")
    public Result<List<Answer>> sessionAnswers(@PathVariable Long sessionId) {
        return Result.ok(examService.sessionAnswers(sessionId));
    }
}
