package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.Answer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 作答明细 Mapper。
 */
@Mapper
public interface AnswerMapper extends BaseMapper<Answer> {

    /**
     * 查某场考试的逐题作答，并带出题目内容/选项/解析，供成绩详情页直接渲染。
     *
     * <p>题目若已被教师删除，明细仍要能展示，因此用 LEFT JOIN；
     * 题目正文以交卷时写入的 {@code correct_answer} 等快照字段为准。</p>
     */
    List<Answer> selectBySessionId(@Param("sessionId") Long sessionId);
}
