package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 课程 Mapper。
 *
 * <p>列表页需要带出「教师姓名 / 题目数 / 总分」等聚合字段，单表 BaseMapper 做不到，
 * 因此这部分走 {@code CourseMapper.xml}。</p>
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * 分页查询课程，附带教师姓名、题目数、总分。
     *
     * @param teacherId 非空时只查该教师的课程
     */
    IPage<Course> selectCoursePage(IPage<Course> page,
                                   @Param("teacherId") Long teacherId,
                                   @Param("keyword") String keyword,
                                   @Param("examEnabled") Integer examEnabled);

    /** 按 ID 查课程详情（含教师姓名、题目数、总分） */
    Course selectCourseDetail(@Param("id") Long id);
}
