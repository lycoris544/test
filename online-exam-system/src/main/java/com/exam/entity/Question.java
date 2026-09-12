package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.common.QuestionOptionListTypeHandler;
import com.exam.dto.question.QuestionOption;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试题目。
 *
 * <p>选项统一存在 {@code options} 字段里（库中为 JSON 文本），
 * 因为不同题型的选项数量不定，拆成独立选项表对本次规模来说属于过度设计。
 * {@link QuestionOptionListTypeHandler} 负责 Java List 与 JSON 文本之间的自动转换。</p>
 */
@Data
@TableName(value = "question", autoResultMap = true)
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属课程 */
    private Long courseId;

    /** 题型，见 {@link com.exam.common.QuestionType} */
    private Integer type;

    /** 题干 */
    private String content;

    /**
     * 选项。仅选择题/判断题使用，填空题为空。
     * 判断题固定为 A=正确、B=错误。
     *
     * <p>两个都不能省：</p>
     * <ul>
     *   <li>{@code autoResultMap = true}：不加，MyBatis-Plus 查询时不会应用 typeHandler，
     *       查出来的 options 会是 null。</li>
     *   <li>typeHandler 用 {@link QuestionOptionListTypeHandler} 而不是 MP 自带的
     *       {@code JacksonTypeHandler}：后者的构造只接收字段声明类型 {@code List.class},
     *       泛型擦除后会反序列化成 {@code List<LinkedHashMap>}，
     *       再按 {@code QuestionOption} 取属性时就抛 ClassCastException。
     *       详见该 handler 的类注释。</li>
     * </ul>
     */
    @TableField(typeHandler = QuestionOptionListTypeHandler.class)
    private List<QuestionOption> options;

    /**
     * 正确答案。
     * <ul>
     *   <li>单选/判断：单个选项 key，如 {@code "A"}</li>
     *   <li>多选：多个 key，如 {@code "AC"}（顺序无关）</li>
     *   <li>填空：可接受的答案，多个之间用 {@code |} 分隔，如 {@code "北京|首都"}</li>
     * </ul>
     */
    private String answer;

    /** 分值 */
    private Integer score;

    /** 题目解析，交卷后展示 */
    private String analysis;

    /** 排序号，决定考试时的题目顺序 */
    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
