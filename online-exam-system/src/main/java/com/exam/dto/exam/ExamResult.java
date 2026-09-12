package com.exam.dto.exam;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 交卷后的判分结果，直接回给前端展示。
 */
@Data
public class ExamResult {

    private Long sessionId;
    private Long courseId;
    private String courseName;

    /** 第几次考试 */
    private Integer attemptNo;

    /** 本次得分 */
    private Integer score;

    /** 试卷总分 */
    private Integer totalScore;

    /** 答对题数 */
    private Integer correctCount;

    /** 题目总数 */
    private Integer questionCount;

    /** 正确率，保留两位小数的百分数 */
    private Double accuracy;

    private LocalDateTime startTime;
    private LocalDateTime submitTime;

    /** 状态，见 {@link com.exam.common.SessionStatus} */
    private Integer status;
    private String statusName;

    /**
     * 是否因超时被强制交卷。
     * 超时交卷时学生可能还有题没答完，前端应给出明确提示而不是当作正常提交。
     */
    private Boolean timeout;

    /** 逐题明细，含正确答案与解析，用于考后回顾 */
    private List<ExamResultItem> items;
}
