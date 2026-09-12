package com.exam.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果。
 *
 * <p>不直接把 MyBatis-Plus 的 {@code IPage} 返回给前端——它序列化后带着
 * {@code orders}、{@code optimizeCountSql} 等内部字段，接口契约会跟着框架版本变。</p>
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private long total;
    /** 当前页码，从 1 开始 */
    private long current;
    /** 每页条数 */
    private long size;
    /** 总页数 */
    private long pages;
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        result.setRecords(page.getRecords());
        return result;
    }

    public static <T> PageResult<T> empty() {
        PageResult<T> result = new PageResult<>();
        result.setTotal(0);
        result.setCurrent(1);
        result.setSize(0);
        result.setPages(0);
        result.setRecords(Collections.emptyList());
        return result;
    }
}
