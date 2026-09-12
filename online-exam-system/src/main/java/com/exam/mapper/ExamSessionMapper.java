package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.entity.ExamSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 考试记录 Mapper。
 */
@Mapper
public interface ExamSessionMapper extends BaseMapper<ExamSession> {

    /**
     * 教师端「考试情况」：分页查询成绩，可按课程、学生姓名/学号过滤。
     * 只返回已结束（已交卷/超时）的记录，进行中的不计入成绩。
     */
    IPage<ExamSession> selectScorePage(IPage<ExamSession> page,
                                       @Param("courseId") Long courseId,
                                       @Param("teacherId") Long teacherId,
                                       @Param("keyword") String keyword);

    /**
     * 某学生在某课程下的全部考试记录，按次数升序——
     * 文档要求「若该课程重考多次，展示每一次考试的成绩」。
     */
    @Select("SELECT * FROM exam_session WHERE student_id = #{studentId} AND course_id = #{courseId} "
            + "ORDER BY attempt_no ASC")
    List<ExamSession> selectAttempts(@Param("studentId") Long studentId,
                                     @Param("courseId") Long courseId);

    /** 学生端「考试记录」：全部历史考试，按时间倒序 */
    IPage<ExamSession> selectStudentHistory(IPage<ExamSession> page,
                                            @Param("studentId") Long studentId,
                                            @Param("courseId") Long courseId);

    /** 该学生在某课程下的作答次数，用于生成 attempt_no */
    @Select("SELECT COUNT(*) FROM exam_session WHERE student_id = #{studentId} AND course_id = #{courseId}")
    int countAttempts(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    /**
     * 某课程下的全部考试记录（含进行中），教师端统计用。
     * 在 Java 里分组统计而不是写一堆聚合 SQL，是因为统计口径
     * （及格线、平均分算法）放在代码里更容易看懂和调整。
     */
    @Select("SELECT * FROM exam_session WHERE course_id = #{courseId} ORDER BY attempt_no ASC")
    List<ExamSession> selectByCourseId(@Param("courseId") Long courseId);
}
