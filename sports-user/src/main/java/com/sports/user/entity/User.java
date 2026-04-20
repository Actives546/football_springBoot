package com.sports.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sports.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 * 继承基础实体类，包含通用字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends BaseEntity {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 状态（0：禁用，1：启用）
     */
    private Integer status;
}
