package com.exam.dto.course;

import com.exam.entity.ExamSession;
import lombok.Data;

import java.util.List;

/**
 * 某学生在某课程下的考试情况（教师端点击某考生后查看）。
 */
@Data
public class StudentCourseDetail {

    private Long studentId;
    private String studentName;
    private String studentNo;
    private String department;

    private Long courseId;
    private String courseName;
    private String courseCode;

    /** 考试次数（含重考） */
    private Integer attemptCount;

    /** 最好成绩 */
    private Integer bestScore;

    /** 试卷总分 */
    private Integer totalScore;

    /**
     * 每一次考试的记录，按次数升序。
     * 对应文档：「若该课程重考多次，展示每一次考试的成绩」。
     */
    private List<ExamSession> attempts;
}
