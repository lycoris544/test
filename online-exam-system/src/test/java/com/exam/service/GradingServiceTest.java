package com.exam.service;

import com.exam.common.QuestionType;
import com.exam.entity.Question;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 判分逻辑单元测试。
 *
 * <p>{@link GradingService} 被刻意设计成不依赖 Spring 的纯函数，
 * 所以这里不需要起容器、不需要数据库，跑得很快。</p>
 */
class GradingServiceTest {

    private static Question choice(int type, String answer, int score) {
        Question q = new Question();
        q.setType(type);
        q.setAnswer(answer);
        q.setScore(score);
        return q;
    }

    // ==================================================================
    // 单选题
    // ==================================================================

    @Test
    @DisplayName("单选：答案正确得满分")
    void singleCorrect() {
        GradingService.GradeResult r = GradingService.grade(
                choice(QuestionType.SINGLE, "B", 10), "B");
        assertTrue(r.isCorrect());
        assertEquals(10, r.getScore());
        assertEquals(10, r.getFullScore());
    }

    @Test
    @DisplayName("单选：答案错误得 0 分")
    void singleWrong() {
        GradingService.GradeResult r = GradingService.grade(
                choice(QuestionType.SINGLE, "B", 10), "C");
        assertFalse(r.isCorrect());
        assertEquals(0, r.getScore());
    }

    @Test
    @DisplayName("单选：小写答案与带空格的答案都应判对")
    void singleCaseAndSpaceInsensitive() {
        assertTrue(GradingService.grade(choice(QuestionType.SINGLE, "B", 10), "b").isCorrect());
        assertTrue(GradingService.grade(choice(QuestionType.SINGLE, "B", 10), " B ").isCorrect());
    }

    // ==================================================================
    // 多选题
    // ==================================================================

    @Test
    @DisplayName("多选：顺序不影响判分（CA 与 AC 等价）")
    void multipleOrderInsensitive() {
        GradingService.GradeResult r = GradingService.grade(
                choice(QuestionType.MULTIPLE, "AC", 10), "CA");
        assertTrue(r.isCorrect());
        assertEquals(10, r.getScore());
    }

    @Test
    @DisplayName("多选：漏选不得分（不给部分分）")
    void multipleMissingChoiceNoPartialCredit() {
        GradingService.GradeResult r = GradingService.grade(
                choice(QuestionType.MULTIPLE, "ACD", 10), "AC");
        assertFalse(r.isCorrect());
        assertEquals(0, r.getScore());
    }

    @Test
    @DisplayName("多选：多选不得分")
    void multipleExtraChoice() {
        GradingService.GradeResult r = GradingService.grade(
                choice(QuestionType.MULTIPLE, "AC", 10), "ABC");
        assertFalse(r.isCorrect());
        assertEquals(0, r.getScore());
    }

    @Test
    @DisplayName("多选：全对得满分")
    void multipleAllCorrect() {
        GradingService.GradeResult r = GradingService.grade(
                choice(QuestionType.MULTIPLE, "ABC", 10), "CBA");
        assertTrue(r.isCorrect());
        assertEquals(10, r.getScore());
    }

    @Test
    @DisplayName("多选：容错逗号与空格分隔的写法")
    void multipleTolerantToSeparators() {
        assertTrue(GradingService.grade(choice(QuestionType.MULTIPLE, "AC", 10), "A,C").isCorrect());
        assertTrue(GradingService.grade(choice(QuestionType.MULTIPLE, "AC", 10), "A C").isCorrect());
        assertTrue(GradingService.grade(choice(QuestionType.MULTIPLE, "AC", 10), "a,c").isCorrect());
    }

    // ==================================================================
    // 判断题
    // ==================================================================

    @Test
    @DisplayName("判断：A / B 判定正确")
    void judge() {
        assertTrue(GradingService.grade(choice(QuestionType.JUDGE, "A", 5), "A").isCorrect());
        assertTrue(GradingService.grade(choice(QuestionType.JUDGE, "B", 5), "B").isCorrect());
        assertFalse(GradingService.grade(choice(QuestionType.JUDGE, "A", 5), "B").isCorrect());
    }

    // ==================================================================
    // 填空题
    // ==================================================================

    @Test
    @DisplayName("填空：忽略首尾空格")
    void fillTrimsWhitespace() {
        GradingService.GradeResult r = GradingService.grade(
                choice(QuestionType.FILL, "finally", 10), "  finally  ");
        assertTrue(r.isCorrect());
    }

    @Test
    @DisplayName("填空：忽略大小写")
    void fillCaseInsensitive() {
        assertTrue(GradingService.grade(choice(QuestionType.FILL, "finally", 10), "FINALLY").isCorrect());
        assertTrue(GradingService.grade(choice(QuestionType.FILL, "GROup by", 10), "group by").isCorrect());
    }

    @Test
    @DisplayName("填空：命中任一可接受答案即算对")
    void fillMultipleAcceptedAnswers() {
        Question q = choice(QuestionType.FILL, "北京|首都北京", 10);
        assertTrue(GradingService.grade(q, "北京").isCorrect());
        assertTrue(GradingService.grade(q, "首都北京").isCorrect());
        assertTrue(GradingService.grade(q, "  首都北京 ").isCorrect());
        assertFalse(GradingService.grade(q, "上海").isCorrect());
    }

    @Test
    @DisplayName("填空：答案错误不得分")
    void fillWrong() {
        GradingService.GradeResult r = GradingService.grade(
                choice(QuestionType.FILL, "finally", 10), "final");
        assertFalse(r.isCorrect());
        assertEquals(0, r.getScore());
    }

    // ==================================================================
    // 未作答与脏数据
    // ==================================================================

    @Test
    @DisplayName("未作答记 0 分，且 userAnswer 归一化为 null")
    void blankIsZero() {
        GradingService.GradeResult r = GradingService.grade(
                choice(QuestionType.SINGLE, "A", 10), "   ");
        assertFalse(r.isCorrect());
        assertEquals(0, r.getScore());
        assertNull(r.getUserAnswer());
    }

    @Test
    @DisplayName("未作答时，即使正确答案为空也不能判对")
    void blankNeverCorrectEvenIfAnswerBlank() {
        // 这条是防回归：如果判分直接比较两个字符串，"" == "" 会被判成对
        Question q = choice(QuestionType.SINGLE, "", 10);
        GradingService.GradeResult r = GradingService.grade(q, null);
        assertFalse(r.isCorrect());
        assertEquals(0, r.getScore());
    }

    @Test
    @DisplayName("题型为空时判错而不是抛异常")
    void nullTypeIsWrongNotCrash() {
        Question q = choice(QuestionType.SINGLE, "A", 10);
        q.setType(null);
        assertFalse(GradingService.grade(q, "A").isCorrect());
    }

    @Test
    @DisplayName("分值为空时按 0 分处理，不影响其余逻辑")
    void nullScoreHandled() {
        Question q = choice(QuestionType.SINGLE, "A", 10);
        q.setScore(null);
        GradingService.GradeResult r = GradingService.grade(q, "A");
        assertTrue(r.isCorrect());
        assertEquals(0, r.getFullScore());
        assertEquals(0, r.getScore());
    }

    @Test
    @DisplayName("判分结果带回正确答案快照，供 answer 表落库")
    void carriesCorrectAnswerSnapshot() {
        GradingService.GradeResult r = GradingService.grade(
                choice(QuestionType.SINGLE, "B", 10), "A");
        assertEquals("B", r.getCorrectAnswer());
        assertEquals("A", r.getUserAnswer());
    }
}
