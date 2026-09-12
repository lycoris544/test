package com.exam.service;

import com.exam.common.QuestionType;
import com.exam.entity.Question;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 自动判分。
 *
 * <p>刻意做成纯函数、不依赖 Spring 容器，这样可以直接用单元测试覆盖各种边界，
 * 不必为了测一个比较逻辑去起数据库。</p>
 *
 * <p>各题型评分规则：</p>
 * <table border="1">
 *   <tr><th>题型</th><th>规则</th></tr>
 *   <tr><td>单选</td><td>与答案完全一致得满分</td></tr>
 *   <tr><td>多选</td><td>全部选对才得分，多选/漏选/错选均不得分（不给部分分）</td></tr>
 *   <tr><td>判断</td><td>同单选</td></tr>
 *   <tr><td>填空</td><td>与任一可接受答案匹配即得满分，比较时去首尾空格且忽略大小写</td></tr>
 * </table>
 */
public final class GradingService {

    private GradingService() {
    }

    /**
     * 判定一道题。
     *
     * @param question   题目，需带 type / answer / score
     * @param userAnswer 学生作答，可为 null（表示未作答）
     * @return 判定结果，永不为 null
     */
    public static GradeResult grade(Question question, String userAnswer) {
        int fullScore = question.getScore() == null ? 0 : question.getScore();
        GradeResult result = new GradeResult();
        result.setFullScore(fullScore);
        result.setCorrectAnswer(question.getAnswer());

        // 未作答直接 0 分。注意这里不能走后面的比较逻辑：
        // 若正确答案本身为空串，空答案会被误判为正确
        if (!StringUtils.hasText(userAnswer)) {
            result.setUserAnswer(null);
            result.setCorrect(false);
            result.setScore(0);
            return result;
        }

        String normalizedUser = userAnswer.trim();
        result.setUserAnswer(normalizedUser);

        boolean correct = isCorrect(question.getType(), question.getAnswer(), normalizedUser);
        result.setCorrect(correct);
        result.setScore(correct ? fullScore : 0);
        return result;
    }

    private static boolean isCorrect(Integer type, String correctAnswer, String userAnswer) {
        if (type == null || !StringUtils.hasText(correctAnswer)) {
            return false;
        }
        if (type == QuestionType.FILL) {
            return matchFill(correctAnswer, userAnswer);
        }
        // 单选、多选、判断题都按选项比较。
        // 多选答案在入库时已按字母排序去重（见 QuestionService#validateChoice），
        // 这里同样归一化学生答案后直接比较字符串即可。
        return normalizeChoice(userAnswer).equals(normalizeChoice(correctAnswer));
    }

    /**
     * 选择题答案归一化：去掉空格和常见分隔符、统一大写、字符排序去重。
     *
     * <p>排序这一步是必须的：多选题的答案是「集合」而不是「序列」，
     * 学生选 {@code CA} 和 {@code AC} 表达的是同一个意思。
     * 入库时正确答案已经排过序（见 {@code QuestionService#validateChoice}），
     * 但学生的作答没有，所以两边都要过这个函数才能直接比较。</p>
     */
    private static String normalizeChoice(String answer) {
        String cleaned = answer.replace(" ", "")
                .replace(",", "")
                .replace("，", "")
                .toUpperCase();

        char[] chars = cleaned.toCharArray();
        Arrays.sort(chars);

        // 去重，避免 "AAB" 被判成等于 "AB"
        StringBuilder distinct = new StringBuilder(chars.length);
        char previous = 0;
        for (char c : chars) {
            if (c != previous) {
                distinct.append(c);
                previous = c;
            }
        }
        return distinct.toString();
    }

    /**
     * 填空题匹配：忽略首尾空格与大小写，命中任一可接受答案即算对。
     *
     * <p>不做模糊匹配（比如忽略全角标点、同义词），因为那需要具体的业务约定；
     * 需要更宽松的判定时，教师可以在答案里用 {@code |} 列出所有可接受写法。</p>
     */
    private static boolean matchFill(String correctAnswer, String userAnswer) {
        List<String> accepted = QuestionService.splitFillAnswers(correctAnswer);
        if (accepted.isEmpty()) {
            return false;
        }
        String user = userAnswer.trim();
        return accepted.stream().anyMatch(item -> item.equalsIgnoreCase(user));
    }

    /**
     * 单题判分结果。
     */
    @Data
    public static class GradeResult {

        /** 学生作答（已 trim），未作答为 null */
        private String userAnswer;

        /** 正确答案快照 */
        private String correctAnswer;

        /** 是否答对 */
        private boolean correct;

        /** 本题得分 */
        private int score;

        /** 本题满分 */
        private int fullScore;
    }
}
