package com.exam.security;

import com.exam.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户读取工具。
 *
 * <p>Controller/Service 通过它拿到 {@link LoginUser}，而不是到处解析请求头。</p>
 */
public final class UserContext {

    private UserContext() {
    }

    /**
     * 取当前登录用户。
     *
     * @throws BizException 未登录时抛出，由全局异常处理转成 401
     */
    public static LoginUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            throw BizException.unauthorized("未登录或登录状态已失效");
        }
        return (LoginUser) authentication.getPrincipal();
    }

    public static Long currentUserId() {
        return current().getUserId();
    }

    public static Integer currentRole() {
        return current().getRole();
    }

    public static boolean isTeacher() {
        return current().isTeacher();
    }

    public static boolean isStudent() {
        return current().isStudent();
    }

    /**
     * 断言当前用户是教师。
     *
     * <p>路径级鉴权（{@code /teacher/**} 需要 ROLE_TEACHER）已经拦掉了大部分越权，
     * 但在 Service 里再断言一次，可以避免将来有人把接口挪到公共路径下时
     * 悄悄绕过角色检查。</p>
     */
    public static void requireTeacher() {
        if (!isTeacher()) {
            throw BizException.forbidden("该操作仅教师可用");
        }
    }

    /** 断言当前用户是学生 */
    public static void requireStudent() {
        if (!isStudent()) {
            throw BizException.forbidden("该操作仅学生可用");
        }
    }
}
