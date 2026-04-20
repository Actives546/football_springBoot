package com.sports.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sports.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 * 继承MyBatis-Plus的BaseMapper，拥有基础CRUD能力
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
