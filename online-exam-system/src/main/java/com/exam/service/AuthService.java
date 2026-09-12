package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.BizException;
import com.exam.common.enums.Role;
import com.exam.dto.auth.LoginRequest;
import com.exam.dto.auth.LoginResponse;
import com.exam.dto.auth.RegisterRequest;
import com.exam.entity.User;
import com.exam.mapper.UserMapper;
import com.exam.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 登录 / 注册。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 登录。教师和学生走同一个入口，靠返回体里的 {@code role} 区分跳转到哪个界面，
     * 对应文档「登录后用户界面分为教师端和学生端」。
     */
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));

        // 用户不存在和密码错误返回同一句提示，避免被用来枚举账号是否存在
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw BizException.forbidden("账号已被禁用，请联系管理员");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .roleName(Role.nameOf(user.getRole()))
                .expiresIn(jwtUtil.getExpirationMillis())
                .build();
    }

    /**
     * 注册。
     *
     * <p>当前实现允许自助注册任意角色。若考核要求「教师账号只能由已有教师创建」
     * （类似文档里医院挂号系统对医生账号的规定），把下面的角色校验改成：
     * 传入 role = 教师时，要求当前请求已登录且是教师即可。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        Integer role = request.getRole();
        // 注意：Role.TEACHER 是 int 常量，这里必须先判 null 再用 != 比较，
        // 写成 Role.TEACHER.equals(...) 会触发自动装箱并永远返回 false
        if (role == null || (role != Role.TEACHER && role != Role.STUDENT)) {
            throw BizException.badRequest("角色取值不正确");
        }

        Long sameName = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (sameName != null && sameName > 0) {
            throw BizException.badRequest("用户名已存在");
        }
        if (StringUtils.hasText(request.getUserNo())) {
            Long sameNo = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getUserNo, request.getUserNo()));
            if (sameNo != null && sameNo > 0) {
                throw BizException.badRequest("该工号/学号已被注册");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRole(request.getRole());
        user.setUserNo(request.getUserNo());
        user.setPhone(request.getPhone());
        user.setGender(request.getGender());
        user.setDepartment(request.getDepartment());
        // 职称只对教师有意义，学生端传了也忽略掉
        user.setTitle(role == Role.TEACHER ? request.getTitle() : null);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
        log.info("新用户注册成功: username={}, role={}", user.getUsername(), Role.nameOf(user.getRole()));
        return user.getId();
    }
}
