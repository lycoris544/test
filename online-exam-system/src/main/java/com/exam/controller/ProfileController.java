package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.user.ProfileResponse;
import com.exam.dto.user.ProfileUpdateRequest;
import com.exam.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 个人信息。教师端和学生端共用。
 *
 * <p>教师端对应文档「个人信息板块：包括教师信息：姓名、性别、手机号、工号、所在学院、教授课程」，
 * 学生端对应「包括学生信息：姓名、性别、手机号、学号、所在学院、所选课程。个人信息仅能修改和保存」。
 * 两端的差异字段由 {@link ProfileService} 按角色填充。</p>
 */
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /** 查看个人信息 */
    @GetMapping
    public Result<ProfileResponse> get() {
        return Result.ok(profileService.currentProfile());
    }

    /** 修改并保存个人信息 */
    @PutMapping
    public Result<ProfileResponse> update(@Valid @RequestBody ProfileUpdateRequest request) {
        return Result.ok("保存成功", profileService.updateProfile(request));
    }
}
