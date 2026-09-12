package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 选课关系。学生「所选课程」由这张表维护。
 */
@Data
@TableName("course_enrollment")
public class CourseEnrollment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long studentId;

    private LocalDateTime createTime;

    // ---- 非数据库字段，列表查询时填充 ----

    /** 课程名 */
    @TableField(exist = false)
    private String courseName;

    /** 课程编码 */
    @TableField(exist = false)
    private String courseCode;

    /** 授课教师姓名 */
    @TableField(exist = false)
    private String teacherName;

    /** 考试时长（分钟） */
    @TableField(exist = false)
    private Integer durationMinutes;

    /** 是否开放考试 */
    @TableField(exist = false)
    private Integer examEnabled;

    /** 题目数量 */
    @TableField(exist = false)
    private Long questionCount;

    /** 试卷总分 */
    @TableField(exist = false)
    private Integer totalScore;

    /**
     * 学生的考试状态，取值见 {@link com.exam.common.SessionStatus}；
     * 从未参加过为 null。
     */
    @TableField(exist = false)
    private Integer examStatus;

    /**
     * 展示用分数：已参加过则给出最好成绩，否则为 null。
     * 文档要求「若已参加过该课程考试，旁边显示分数」。
     */
    @TableField(exist = false)
    private Integer lastScore;

    /** 该课程学生已考次数（含重考）。由 Service 依据 ongoingCount + finishedCount 补算 */
    @TableField(exist = false)
    private Integer attemptCount;

    /** 开考后尚未交卷的记录数。SQL 算出，用于推导上面的 attemptCount */
    @TableField(exist = false)
    private Integer ongoingCount;

    /** 已结束（已交卷/超时）的记录数。SQL 算出 */
    @TableField(exist = false)
    private Integer finishedCount;

    /** 是否允许直接开考：未参加过，或上一场已结束 */
    @TableField(exist = false)
    private Boolean canStart;

    // ---- 以下字段仅在「教师端查看某课程选课名单」时填充 ----

    /** 学生姓名 */
    @TableField(exist = false)
    private String studentName;

    /** 学号 */
    @TableField(exist = false)
    private String studentNo;

    /** 学生所在学院 */
    @TableField(exist = false)
    private String department;
}
