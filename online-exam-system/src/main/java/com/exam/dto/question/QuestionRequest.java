package com.exam.dto.question;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 新增 / 修改题目请求。
 *
 * <p>字段的合法性依赖题型，无法用注解表达，统一放在
 * {@code QuestionService#validate} 里校验（例如多选题答案至少要两个选项 key）。</p>
 */
@Data
public class QuestionRequest {

    @NotNull(message = "题型不能为空")
    @Min(value = 1, message = "题型取值为 1~4")
    @Max(value = 4, message = "题型取值为 1~4")
    private Integer type;

    @NotBlank(message = "题干不能为空")
    @Size(max = 2000, message = "题干过长")
    private String content;

    /** 选项，单选题/多选题/判断题必填；填空题为空 */
    private List<QuestionOption> options;

    /**
     * 正确答案。
     * 选择题填选项 key（多选如 {@code "AC"}）；
     * 填空题填答案原文，多个可接受答案用 {@code |} 分隔。
     */
    @NotBlank(message = "正确答案不能为空")
    private String answer;

    @NotNull(message = "分值不能为空")
    @Min(value = 1, message = "分值至少为 1")
    @Max(value = 100, message = "单题分值最多 100")
    private Integer score;

    @Size(max = 2000, message = "解析过长")
    private String analysis;

    /** 排序号，不传则自动排到该课程最后 */
    private Integer sortOrder;
}
