package com.exam.dto.auth;

import lombok.Builder;
import lombok.Data;

/**
 * 登录结果。前端拿到 {@code token} 后，后续请求放到
 * {@code Authorization: Bearer <token>} 头里。
 */
@Data
@Builder
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private String realName;
    /** 1 教师 / 2 学生 */
    private Integer role;
    /** 角色的中文名，前端可直接显示 */
    private String roleName;
    /** token 有效期（毫秒） */
    private Long expiresIn;
}
