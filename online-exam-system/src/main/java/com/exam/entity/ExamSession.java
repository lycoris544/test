package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 一场考试（答题记录）。
 *
 * <p>同一个学生对同一门课程可以有多条记录——这就是「重考」。
 * 每次重考生成一条新记录，历史成绩全部保留，文档要求
 * 「若该课程重考多次，展示每一次考试的成绩」。</p>
 */
@Data
@TableName("exam_session")
public class ExamSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long studentId;

    /** 第几次考试，从 1 开始 */
    private Integer attemptNo;

    /** 状态，见 {@link com.exam.common.SessionStatus} */
    private Integer status;

    /** 开考时间 */
    private LocalDateTime startTime;

    /**
     * 最晚交卷时间 = 开考时间 + 课程时长。
     * 开考时算好并存库，避免之后教师改课程时长影响进行中的考试。
     */
    private LocalDateTime deadline;

    /** 实际交卷时间 */
    private LocalDateTime submitTime;

    /** 得分 */
    private Integer score;

    /** 试卷总分（交卷时按当时的题目快照计算，防止教师事后改分值导致对不上） */
    private Integer totalScore;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // ---- 非数据库字段，列表查询时填充 ----

    /** 学生姓名 */
    @TableField(exist = false)
    private String studentName;

    /** 学号 */
    @TableField(exist = false)
    private String studentNo;

    /** 学生所在学院 */
    @TableField(exist = false)
    private String department;

    /** 课程名 */
    @TableField(exist = false)
    private String courseName;

    /** 课程编码 */
    @TableField(exist = false)
    private String courseCode;

    /** 本轮考试的逐题作答明细，仅在查看单场成绩时填充 */
    @TableField(exist = false)
    private List<Answer> answers;
}
