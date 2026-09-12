package com.exam.dto.user;

import com.exam.entity.Course;
import lombok.Data;

import java.util.List;

/**
 * 个人信息。教师端和学生端共用，差异部分（教授的课程 / 所选课程）按角色填充。
 */
@Data
public class ProfileResponse {

    private Long id;
    private String username;
    private String realName;
    /** 1 男 / 2 女 */
    private Integer gender;
    private String phone;
    /** 工号（教师）或学号（学生） */
    private String userNo;
    private String department;
    /** 职称，仅教师 */
    private String title;
    /** 1 教师 / 2 学生 */
    private Integer role;
    private String roleName;

    /**
     * 教师：该教师教授的课程（文档中「教授课程」一栏）。
     * 学生端为 null，学生用下面的 enrolledCourses。
     */
    private List<Course> teachingCourses;

    /**
     * 学生：已选课程。
     * 教师端为 null。
     */
    private List<Course> enrolledCourses;
}
