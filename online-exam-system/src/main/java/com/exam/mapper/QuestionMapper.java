package com.exam.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.exam.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题目 Mapper。
 *
 * <p><b>本 Mapper 的查询一律走 MyBatis-Plus 内置方法，不要手写 SQL。</b>
 * {@code question.options} 是 JSON 列，靠
 * {@link com.exam.common.QuestionOptionListTypeHandler} 反序列化，
 * 而这个 typeHandler 只挂在 MyBatis-Plus 依据 {@code @TableField} 自动生成的那张
 * 结果映射上（见 {@link Question} 上 {@code autoResultMap} 的说明）。
 * 手写 SQL——{@code @Select} 注解、或 XML 里的 {@code resultType}——用的是 MyBatis
 * 默认自动映射，挂不上这个 typeHandler，查出来的 {@code options} 恒为 {@code null}。
 * 试卷因此下发给学生时没有任何选项，单选、多选题直接没法作答。</p>
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 按课程分页查询题目（教师端题库管理，不做任何剪裁）。
     */
    default IPage<Question> selectQuestionPage(IPage<Question> page,
                                               Long courseId,
                                               Integer type,
                                               String keyword) {
        return selectPage(page, new LambdaQueryWrapper<Question>()
                .eq(courseId != null, Question::getCourseId, courseId)
                .eq(type != null, Question::getType, type)
                .like(StringUtils.isNotBlank(keyword), Question::getContent, keyword)
                .orderByAsc(Question::getSortOrder)
                .orderByAsc(Question::getId));
    }

    /** 取某课程的全部题目，按排序号升序——考试组卷和总分计算都用它 */
    default List<Question> selectByCourseId(Long courseId) {
        return selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .orderByAsc(Question::getSortOrder)
                .orderByAsc(Question::getId));
    }

    /** 取某课程当前最大排序号，新增题目时排到最后 */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM question WHERE course_id = #{courseId}")
    Integer selectMaxSortOrder(@Param("courseId") Long courseId);
}
