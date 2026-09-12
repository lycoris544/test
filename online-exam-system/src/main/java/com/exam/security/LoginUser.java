package com.exam.security;

import com.exam.common.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 登录态。放入 Spring Security 的 SecurityContext 中，供 Controller 和 Service 读取。
 *
 * <p>只携带鉴权必需的信息（ID / 登录名 / 角色）。需要完整用户资料时按 ID 回查数据库，
 * 避免把可能已变更的资料长期留在 token 里。</p>
 */
@Getter
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final Integer role;

    public LoginUser(Long userId, String username, Integer role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public boolean isTeacher() {
        return role != null && role == Role.TEACHER;
    }

    public boolean isStudent() {
        return role != null && role == Role.STUDENT;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(Role.authorityOf(role)));
    }

    /** JWT 已校验完毕，这里不参与密码比对，交出空串即可 */
    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
