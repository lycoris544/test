package com.exam.dto.exam;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试试卷：开考时返回给学生的完整信息。
 */
@Data
public class ExamPaper {

    private Long sessionId;
    private Long courseId;
    private String courseName;
    private String courseCode;

    /** 第几次考试 */
    private Integer attemptNo;

    /** 试卷总分 */
    private Integer totalScore;

    /** 题目数量 */
    private Integer questionCount;

    /** 开考时间 */
    private LocalDateTime startTime;

    /**
     * 交卷截止时间。前端据此做倒计时。
     * 真实约束在服务端：交卷时若已过期，会按超时处理（见 ExamService#submit）。
     */
    private LocalDateTime deadline;

    /** 剩余秒数，前端倒计时初始值，省得自己算时区 */
    private Long remainingSeconds;

    private List<ExamQuestion> questions;
}
