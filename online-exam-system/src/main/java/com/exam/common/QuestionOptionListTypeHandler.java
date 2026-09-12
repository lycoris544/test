package com.exam.common;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.exam.dto.question.QuestionOption;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * {@code question.options} 这个 JSON 列的 TypeHandler。
 *
 * <p><b>为什么不直接用 MyBatis-Plus 自带的 {@link JacksonTypeHandler}</b>：
 * 它只有 {@code JacksonTypeHandler(Class<?>)} 一个构造，MyBatis 实例化它时传进去的是
 * 字段的声明类型 {@code List.class} —— 泛型参数在运行期早已擦除，
 * 于是 Jackson 只能反序列化成 {@code List<LinkedHashMap>}，而不是
 * {@code List<QuestionOption>}。</p>
 *
 * <p>这个错误特别隐蔽：{@code LinkedHashMap} 再序列化回 JSON 时形状和
 * {@code List<QuestionOption>} 一模一样，光看接口返回根本看不出来，
 * 前端拿到的选项也完全正常。只有按 {@code QuestionOption} 去取属性时才会炸 ——
 * 例如交卷后生成「B. DISTINCT」这样的作答描述文本，会在
 * {@code ExamService#describeAnswer} 里抛
 * {@code ClassCastException: LinkedHashMap cannot be cast to QuestionOption}。</p>
 *
 * <p>所以这里用 {@link TypeReference} 把泛型信息保留下来，并提供一个公开无参构造
 * ——MyBatis 的 {@code TypeHandlerRegistry#getInstance} 在找不到 {@code (Class)}
 * 构造时会回退到无参构造，有它就够了。</p>
 *
 * <p>读写都复用 {@link JacksonTypeHandler#getObjectMapper()}，
 * 保证与库里已有的 JSON 文本格式一致。</p>
 */
public class QuestionOptionListTypeHandler extends BaseTypeHandler<List<QuestionOption>> {

    /** 匿名子类是为了让 Jackson 能通过反射拿到 {@code List<QuestionOption>} 这个泛型 */
    private static final TypeReference<List<QuestionOption>> TYPE =
            new TypeReference<List<QuestionOption>>() {
            };

    private static final ObjectMapper MAPPER = JacksonTypeHandler.getObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<QuestionOption> parameter,
                                    JdbcType jdbcType) throws SQLException {
        ps.setString(i, toJson(parameter));
    }

    @Override
    public List<QuestionOption> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<QuestionOption> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<QuestionOption> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private static String toJson(List<QuestionOption> options) {
        try {
            return MAPPER.writerFor(TYPE).writeValueAsString(options);
        } catch (Exception e) {
            throw new IllegalArgumentException("选项序列化失败", e);
        }
    }

    /** 空串和 null 都按「没有选项」处理（填空题就是这种情况），不抛异常 */
    private static List<QuestionOption> parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("选项反序列化失败: " + json, e);
        }
    }
}
