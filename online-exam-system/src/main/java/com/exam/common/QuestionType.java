package com.exam.common;

/**
 * 题型。
 *
 * <p>文档只写了「考试题目」，没有限定题型，这里实现常见的四种，全部可自动判分：</p>
 * <ul>
 *   <li>{@link #SINGLE} 单选题：选项唯一，答对得满分</li>
 *   <li>{@link #MULTIPLE} 多选题：全部选对才得分（选错或漏选都不得分）</li>
 *   <li>{@link #JUDGE} 判断题：选项固定为「正确 / 错误」</li>
 *   <li>{@link #FILL} 填空题：人工阅卷成本高，这里用字符串精确匹配自动判分，
 *       多个可接受答案用 {@link #FILL_ANSWER_SEPARATOR} 分隔</li>
 * </ul>
 */
public final class QuestionType {

    public static final int SINGLE = 1;
    public static final int MULTIPLE = 2;
    public static final int JUDGE = 3;
    public static final int FILL = 4;

    /** 判断题的固定选项 */
    public static final String JUDGE_TRUE_OPTION = "A";
    public static final String JUDGE_FALSE_OPTION = "B";

    /** 填空题多个正确答案之间的分隔符，如 {@code "北京|首都北京"} */
    public static final String FILL_ANSWER_SEPARATOR = "|";

    private QuestionType() {
    }

    public static String nameOf(Integer type) {
        if (type == null) {
            return "未知";
        }
        switch (type) {
            case SINGLE:
                return "单选题";
            case MULTIPLE:
                return "多选题";
            case JUDGE:
                return "判断题";
            case FILL:
                return "填空题";
            default:
                return "未知";
        }
    }

    public static boolean isValid(Integer type) {
        return type != null && type >= SINGLE && type <= FILL;
    }

    /** 选择题（含判断题）靠选项答题，填空题靠文本答题 */
    public static boolean isChoice(Integer type) {
        return type != null && (type == SINGLE || type == MULTIPLE || type == JUDGE);
    }
}
