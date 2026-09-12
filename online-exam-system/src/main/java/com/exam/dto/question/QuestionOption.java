package com.exam.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 选择题的一个选项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOption {

    /** 选项标识，如 A / B / C / D */
    @NotBlank(message = "选项标识不能为空")
    private String key;

    /** 选项正文 */
    @NotBlank(message = "选项内容不能为空")
    private String content;
}
