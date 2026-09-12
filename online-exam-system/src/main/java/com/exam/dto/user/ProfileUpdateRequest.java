package com.exam.dto.user;

import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 修改个人信息。
 *
 * <p>文档要求「个人信息仅能修改和保存」。这里的字段设计成
 * 「只允许改资料，不允许改身份」：{@code username}、{@code userNo}、{@code role}
 * 都不在请求体里——它们是账号标识，改了会牵连登录和成绩归属。
 * 学生改选课走另外的选课接口，不从这里走。</p>
 */
@Data
public class ProfileUpdateRequest {

    @Size(max = 32, message = "姓名过长")
    private String realName;

    /** 1 男 / 2 女 */
    private Integer gender;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 64, message = "学院名称过长")
    private String department;

    /** 职称，仅教师有意义 */
    @Size(max = 32, message = "职称过长")
    private String title;
}
