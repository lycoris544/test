package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户表：教师和学生共用一张表，靠 {@code role} 区分。
 *
 * <p>文档里教师有「工号 / 所在学院 / 教授课程」，学生有「学号 / 所在学院 / 所选课程」，
 * 字段大部分重合，因此合并为一张表；教师的「教授课程」不落在这里，
 * 而是通过 {@link Course#getTeacherId()} 反查（一门课程只属于一位授课教师），
 * 避免课程名以逗号拼接字符串存库导致无法维护。</p>
 */
@Data
@TableName("sys_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号，全局唯一 */
    private String username;

    /** BCrypt 加密后的密码，绝不返回给前端 */
    @JsonIgnore
    private String password;

    /** 姓名 */
    private String realName;

    /** 性别：1 男 / 2 女 */
    private Integer gender;

    private String phone;

    /** 工号（教师）或学号（学生） */
    private String userNo;

    /** 所在学院 */
    private String department;

    /** 职称，仅教师使用 */
    private String title;

    /** 角色，见 {@link com.exam.common.enums.Role} */
    private Integer role;

    /** 账号状态：0 禁用 / 1 正常 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 非数据库字段：该学生已选课程数，列表页展示用 */
    @TableField(exist = false)
    private Long courseCount;
}
