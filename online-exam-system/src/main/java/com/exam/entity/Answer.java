package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.common.QuestionOptionListTypeHandler;
import com.exam.dto.question.QuestionOption;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 逐题作答明细。交卷时一次性写入，用于成绩详情和错题回顾。
 *
 * <p>{@link #options} 等字段是 {@code exist = false} 的展示字段，库表里没有这些列，
 * 只有 {@code AnswerMapper.xml} 里那条 {@code selectBySessionId} 的 LEFT JOIN
 * 会把题干、选项、解析一起查出来。所以那条查询必须用显式 {@code resultMap}
 * 给 options 挂上 {@link QuestionOptionListTypeHandler} —— 用 {@code resultType}
 * 的话挂不上 typeHandler，options 恒为 null，成绩详情页就没有选项可显示。
 * 注意 {@code autoResultMap} 对这类手写查询不起作用，别指望它。</p>
 */
@Data
@TableName(value = "answer", autoResultMap = true)
public class Answer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属考试记录 */
    private Long sessionId;

    /** 题目 ID */
    private Long questionId;

    /** 学生的作答：选择题为选项 key（多选如 "AC"），填空题为原文 */
    private String userAnswer;

    /** 该题的正确答案快照（判分时的值） */
    private String correctAnswer;

    /** 本题是否答对 */
    private Integer isCorrect;

    /** 本题得分 */
    private Integer score;

    /** 本题满分 */
    private Integer fullScore;

    // ---- 非数据库字段，查成绩详情时填充，方便前端直接渲染 ----

    /** 题干 */
    @TableField(exist = false)
    private String content;

    /** 题型 */
    @TableField(exist = false)
    private Integer type;

    /** 选项；由 selectBySessionId 的 join 填充，不在 answer 表里 */
    @TableField(exist = false, typeHandler = QuestionOptionListTypeHandler.class)
    private List<QuestionOption> options;

    /** 解析 */
    @TableField(exist = false)
    private String analysis;

    /** 题目顺序号 */
    @TableField(exist = false)
    private Integer sortOrder;
}
