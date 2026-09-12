package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考试课程。一门课程属于一位授课教师，包含若干 {@link Question}。
 */
@Data
@TableName("course")
public class Course implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程名称 */
    private String name;

    /** 课程编码，如 CS101 */
    private String code;

    /** 课程简介 */
    private String description;

    /** 授课教师 ID */
    private Long teacherId;

    /**
     * 考试时长（分钟）。学生开考后服务端记录开始时间，
     * 超过该时长未交卷的会被强制结算（见 {@code ExamService}）。
     */
    private Integer durationMinutes;

    /** 是否开放考试：0 关闭 / 1 开放。关闭后学生无法开考 */
    private Integer examEnabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // ---- 以下为非数据库字段，查询时填充 ----

    /** 授课教师姓名 */
    @TableField(exist = false)
    private String teacherName;

    /** 题目总数 */
    @TableField(exist = false)
    private Long questionCount;

    /** 试卷总分（题目分值之和） */
    @TableField(exist = false)
    private Integer totalScore;

    /** 已选该课程的学生人数 */
    @TableField(exist = false)
    private Long studentCount;
}
