package com.exam.dto.auth;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 注册请求。
 *
 * <p>文档只写了「登录后用户界面分为教师端和学生端」，没提注册。
 * 但学生要能选课、教师要有课程，账号总得有人建，所以这里开放注册接口，
 * 并允许指定角色。若要收紧为「仅教师可注册教师账号」，把
 * {@code AuthService#register} 里的角色判断改掉即可，注释已标注位置。</p>
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度需在 3~32 之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在 6~32 之间")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotNull(message = "角色不能为空")
    @Min(value = 1, message = "角色取值只能是 1（教师）或 2（学生）")
    @Max(value = 2, message = "角色取值只能是 1（教师）或 2（学生）")
    private Integer role;

    /** 工号或学号 */
    @NotBlank(message = "工号/学号不能为空")
    private String userNo;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 1 男 / 2 女 */
    private Integer gender;

    private String department;

    /** 职称，教师可选填 */
    private String title;
}
