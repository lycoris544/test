package com.exam.dto.exam;

import com.exam.dto.question.QuestionOption;
import lombok.Data;

import java.util.List;

/**
 * 判分结果中的单题明细。交卷后才下发，因此这里包含正确答案和解析。
 */
@Data
public class ExamResultItem {

    private Long questionId;
    private Integer number;
    private Integer type;
    private String typeName;
    private String content;
    private List<QuestionOption> options;

    /** 学生作答；未作答为 null */
    private String userAnswer;
    /** 学生作答的可读形式，选项 key 会翻译成选项正文 */
    private String userAnswerText;

    private String correctAnswer;
    private String correctAnswerText;

    private Boolean correct;

    private Integer score;
    private Integer fullScore;

    private String analysis;

    /** 题目已被教师从题库删除时为 true，前端提示学生即可 */
    private Boolean questionDeleted;
}
