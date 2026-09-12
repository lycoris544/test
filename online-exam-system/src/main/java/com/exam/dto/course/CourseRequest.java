package com.exam.dto.course;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 新增 / 修改课程请求。
 */
@Data
public class CourseRequest {

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 64, message = "课程名称过长")
    private String name;

    @NotBlank(message = "课程编码不能为空")
    @Size(max = 32, message = "课程编码过长")
    private String code;

    @Size(max = 500, message = "课程简介过长")
    private String description;

    @NotNull(message = "考试时长不能为空")
    @Min(value = 1, message = "考试时长至少 1 分钟")
    @Max(value = 600, message = "考试时长最多 600 分钟")
    private Integer durationMinutes;

    /** 是否开放考试：0 关闭 / 1 开放。不传默认关闭，教师准备好题目后再开放 */
    private Integer examEnabled;
}
