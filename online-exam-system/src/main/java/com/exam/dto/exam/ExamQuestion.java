package com.exam.dto.exam;

import com.exam.dto.question.QuestionOption;
import lombok.Data;

import java.util.List;

/**
 * 考试中的一道题（发给学生的版本）。
 *
 * <p>这个类刻意不含 {@code answer} 和 {@code analysis} 字段。
 * 若直接把 {@link com.exam.entity.Question} 实体返回给前端，
 * 正确答案会随 JSON 一起下发，学生开一次开发者工具就拿到答案了。
 * 需要展示答案的场景（交卷后回顾、教师端管理）用另一个 DTO。</p>
 */
@Data
public class ExamQuestion {

    private Long id;
    /** 题号，从 1 开始，由后端按顺序编好，前端直接展示 */
    private Integer number;
    /** 题型 */
    private Integer type;
    /** 题型中文名 */
    private String typeName;
    private String content;
    private List<QuestionOption> options;
    /** 本题满分 */
    private Integer score;
}
