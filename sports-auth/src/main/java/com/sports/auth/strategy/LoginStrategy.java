package com.sports.auth.strategy;

import com.sports.auth.dto.LoginDTO;
import com.sports.auth.dto.LoginResponseDTO;

/**
 * 登录策略接口
 * 使用策略模式实现多种登录方式
 */
public interface LoginStrategy {

    /**
     * 执行登录
     *
     * @param loginDTO 登录参数
     * @return 登录响应
     */
    LoginResponseDTO login(LoginDTO loginDTO);

    /**
     * 获取登录类型
     *
     * @return 登录类型标识
     */
    String getLoginType();
}
