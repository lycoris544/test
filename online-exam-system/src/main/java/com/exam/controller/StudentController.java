package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.exam.ExamPaper;
import com.exam.dto.exam.ExamResult;
import com.exam.dto.exam.SubmitExamRequest;
import com.exam.entity.Course;
import com.exam.entity.CourseEnrollment;
import com.exam.entity.ExamSession;
import com.exam.security.UserContext;
import com.exam.service.ExamService;
import com.exam.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生端接口。
 *
 * <p>{@code /student/**} 在 {@code SecurityConfig} 里限定为 ROLE_STUDENT。
 * 个人信息走公共的 {@code /profile} 接口。</p>
 *
 * <p>对应文档「考试板块」：选课 → 开考 → 答题 → 交卷判分 → 查看成绩 → 重考。</p>
 */
@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final ExamService examService;

    // ==================================================================
    // 选课
    // ==================================================================

    /** 我的课程及考试状态 */
    @GetMapping("/courses")
    public Result<List<CourseEnrollment>> myCourses() {
        return Result.ok(studentService.myCourses());
    }

    /** 可选课程（尚未选的） */
    @GetMapping("/courses/available")
    public Result<List<Course>> availableCourses(@RequestParam(required = false) String keyword) {
        return Result.ok(studentService.availableCourses(keyword));
    }

    /** 选课 */
    @PostMapping("/courses/{courseId}/enroll")
    public Result<Void> enroll(@PathVariable Long courseId) {
        studentService.enroll(courseId);
        return Result.ok("选课成功", null);
    }

    /** 退选课程 */
    @DeleteMapping("/courses/{courseId}/enroll")
    public Result<Void> drop(@PathVariable Long courseId) {
        studentService.drop(courseId);
        return Result.ok("已退选", null);
    }

    // ==================================================================
    // 考试
    // ==================================================================

    /**
     * 开始考试。
     *
     * <p>已考过的课程需要显式 {@code restart=true}（对应页面上「重考」按钮）；
     * 否则返回提示，避免误点覆盖成绩。</p>
     */
    @PostMapping("/courses/{courseId}/exam/start")
    public Result<ExamPaper> startExam(@PathVariable Long courseId,
                                       @RequestParam(defaultValue = "false") boolean restart) {
        return Result.ok(examService.start(courseId, restart));
    }

    /** 恢复进行中的答卷（页面刷新 / 中途退出后回来） */
    @GetMapping("/courses/{courseId}/exam/current")
    public Result<ExamPaper> currentExam(@PathVariable Long courseId) {
        return Result.ok(examService.currentPaper(courseId));
    }

    /**
     * 交卷。服务端判分后直接返回成绩，前端拿到即可展示。
     */
    @PostMapping("/exam/sessions/{sessionId}/submit")
    public Result<ExamResult> submitExam(@PathVariable Long sessionId,
                                         @RequestBody SubmitExamRequest request) {
        return Result.ok("交卷成功", examService.submit(sessionId, request));
    }

    /** 查看某场考试的成绩详情（含逐题答案与解析） */
    @GetMapping("/exam/sessions/{sessionId}/result")
    public Result<ExamResult> examResult(@PathVariable Long sessionId) {
        return Result.ok(examService.result(sessionId));
    }

    /** 历史考试记录 */
    @GetMapping("/exam/history")
    public Result<PageResult<ExamSession>> examHistory(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long courseId) {
        return Result.ok(PageResult.of(examService.history(current, size, courseId)));
    }

    /** 某门课的历次考试记录 */
    @GetMapping("/courses/{courseId}/attempts")
    public Result<List<ExamSession>> myAttempts(@PathVariable Long courseId) {
        // 学生只能查自己的，Service 里会校验
        return Result.ok(examService.attempts(UserContext.currentUserId(), courseId));
    }
}
