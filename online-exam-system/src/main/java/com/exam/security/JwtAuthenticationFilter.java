package com.exam.security;

import com.exam.common.enums.Role;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 认证过滤器。
 *
 * <p>从请求头取 token，校验通过就把 {@link LoginUser} 放进 SecurityContext。
 * 这里只做「认证」（你是谁），不做「授权」（你能不能访问）——授权交给
 * {@link com.exam.config.SecurityConfig} 的路径规则。</p>
 *
 * <p>token 缺失或非法时不在此处报错，直接放行。后续若访问受保护路径，
 * Spring Security 会因为匿名身份而返回 401；若访问的是公开接口则正常放行。</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final String header;
    private final String prefix;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   @Value("${jwt.header}") String header,
                                   @Value("${jwt.prefix}") String prefix) {
        this.jwtUtil = jwtUtil;
        this.header = header;
        this.prefix = prefix;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String token = resolveToken(request);
        // SecurityContext 里已有认证信息就不再重复解析（同一次请求可能多次进入过滤器链）
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = jwtUtil.parse(token);
            if (claims != null) {
                Long userId = Long.valueOf(claims.getSubject());
                String username = claims.get("username", String.class);
                Integer role = claims.get("role", Integer.class);

                LoginUser loginUser = new LoginUser(userId, username, role);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        chain.doFilter(request, response);
    }

    /** 从 Authorization 头里剥掉前缀，取出裸 token */
    private String resolveToken(HttpServletRequest request) {
        String value = request.getHeader(header);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (StringUtils.hasText(prefix) && value.startsWith(prefix)) {
            return value.substring(prefix.length()).trim();
        }
        return value.trim();
    }
}
