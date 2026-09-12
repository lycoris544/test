package com.exam.dto.course;

import com.exam.entity.ExamSession;
import lombok.Data;

import java.util.List;

/**
 * 教师端「考试情况」的课程统计概览。
 */
@Data
public class CourseStats {

    private Long courseId;
    private String courseName;
    private String courseCode;

    /** 试卷总分 */
    private Integer totalScore;

    /** 选课人数 */
    private Integer studentCount;

    /** 参加过考试的人数（至少有一次已结束的考试） */
    private Integer examinedCount;

    /** 尚未参加过考试的人数 */
    private Integer notExaminedCount;

    /** 已结束的考试记录总数（含重考，即考试人次） */
    private Integer attemptCount;

    /** 平均分，保留一位小数；无人考试时为 null */
    private Double averageScore;

    /** 最高分 */
    private Integer highestScore;

    /** 最低分 */
    private Integer lowestScore;

    /**
     * 及格率（百分数，保留一位小数）。
     * 及格线按总分的 60% 计算，四舍五入。
     */
    private Double passRate;

    /**
     * 每个学生的最新一次成绩。
     * 文档要求「点击进入某考生的该课程考试情况，若该课程重考多次，
     * 展示每一次考试的成绩」——列表给最新一次，明细由
     * {@code /teacher/students/{studentId}/courses/{courseId}/attempts} 提供。
     */
    private List<ExamSession> students;
}
