package com.exam.config;

import com.exam.common.Result;
import com.exam.common.enums.Role;
import com.exam.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

/**
 * Spring Security 配置。
 *
 * <p>整体是无状态的 JWT 方案：关掉 session、关掉 CSRF，认证全靠
 * {@link JwtAuthenticationFilter}。授权按路径前缀区分角色——这是文档里
 * 「登录后用户界面分为教师端和学生端」在服务端的落点。</p>
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    /** BCrypt：自带盐值，不存明文也不存 MD5 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                // 前后端分离 + token 鉴权，不需要 session
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .cors().configurationSource(corsConfigurationSource())
                .and()
                // H2 控制台是 iframe 页面，帧选项必须放开，否则打不开
                .headers().frameOptions().sameOrigin()
                .and()
                .authorizeRequests()
                // 登录接口与静态资源放行
                .antMatchers("/auth/login", "/auth/register").permitAll()
                .antMatchers("/h2-console/**").permitAll()
                .antMatchers("/error").permitAll()
                // 按角色隔离两端接口
                .antMatchers("/teacher/**").hasRole(roleName(Role.TEACHER))
                .antMatchers("/student/**").hasRole(roleName(Role.STUDENT))
                // 其余一律要求登录
                .anyRequest().authenticated()
                .and()
                // 认证/授权失败时返回 JSON，而不是 Spring 默认的登录页跳转
                .exceptionHandling()
                .authenticationEntryPoint((request, response, ex) ->
                        writeJson(response, HttpStatus.UNAUTHORIZED, 401, "登录状态已失效，请重新登录"))
                .accessDeniedHandler((request, response, ex) ->
                        writeJson(response, HttpStatus.FORBIDDEN, 403, "无权访问该资源"))
                .and()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** hasRole("TEACHER") 实际要求权限名是 ROLE_TEACHER，这里去掉前缀只传后缀 */
    private String roleName(int role) {
        return Role.authorityOf(role).substring("ROLE_".length());
    }

    private void writeJson(javax.servlet.http.HttpServletResponse response,
                           HttpStatus httpStatus, int code, String message) throws java.io.IOException {
        response.setStatus(httpStatus.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, message)));
    }

    /**
     * 跨域配置。开发阶段前端端口不固定，这里放开常用本地端口。
     * 生产环境请把 allowedOriginPatterns 收敛为实际域名。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
