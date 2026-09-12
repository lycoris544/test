package com.exam;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 在线考试系统后端启动类。
 *
 * <p>登录后按角色区分教师端（{@code /api/teacher/**}）与学生端（{@code /api/student/**}），
 * 鉴权由 {@link com.exam.config.SecurityConfig} + JWT 完成。</p>
 *
 * <p>排除 {@link UserDetailsServiceAutoConfiguration}：本项目的认证完全走 JWT
 * （见 {@code JwtAuthenticationFilter}），并不需要 Spring Security 的内存用户。
 * 不排除的话，启动日志里会打印一句 "Using generated security password: ..."，
 * 那是自动配置出来的、实际上谁也用不到的默认账号，容易让人误以为系统有后门。</p>
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan("com.exam.mapper")
public class ExamApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamApplication.class, args);
    }
}