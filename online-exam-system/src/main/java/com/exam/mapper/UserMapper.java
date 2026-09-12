package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper。单表操作直接用 MyBatis-Plus 的 BaseMapper，无需手写 SQL。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
