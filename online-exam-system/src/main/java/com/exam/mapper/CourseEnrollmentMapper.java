package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.CourseEnrollment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 选课关系 Mapper。
 */
@Mapper
public interface CourseEnrollmentMapper extends BaseMapper<CourseEnrollment> {

    /**
     * 查某学生的选课列表，并计算每门课的考试状态。
     *
     * <p>文档要求「课程列表会保存考试状态。若已参加过该课程考试，旁边显示分数」，
     * 因此这里一并算出：已考次数、最好成绩、是否有进行中的考试。相关子查询都在
     * {@code CourseEnrollmentMapper.xml} 里，SQL 有详细注释。</p>
     */
    List<CourseEnrollment> selectStudentCourses(@Param("studentId") Long studentId);

    /** 查某课程的选课学生（教师端查看选课名单） */
    List<CourseEnrollment> selectCourseStudents(@Param("courseId") Long courseId);
}
