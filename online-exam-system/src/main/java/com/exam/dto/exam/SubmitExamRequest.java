package com.exam.dto.exam;

import lombok.Data;

import java.util.List;

/**
 * 交卷请求。
 *
 * <p>未作答的题目可以不传，服务端按「未作答 = 0 分」处理。</p>
 */
@Data
public class SubmitExamRequest {

    /** 逐题作答 */
    private List<SubmitAnswer> answers;

    @Data
    public static class SubmitAnswer {

        private Long questionId;

        /**
         * 学生作答。
         * 选择题填选项 key，多选题按任意顺序拼接（如 {@code "CA"} 与 {@code "AC"} 等价）；
         * 填空题填原文。
         */
        private String userAnswer;
    }
}
