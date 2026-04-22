package com.sports.auth.service;

import com.sports.auth.dto.LoginDTO;
import com.sports.auth.dto.LoginResponseDTO;
import com.sports.auth.dto.RegisterDTO;

public interface AuthService {

    /**
     * 1. 用户登录
     * 支持用户名密码登录和手机号验证码登录两种方式
     *
     * @param loginDTO 登录参数
     * @return 登录响应
     */
    LoginResponseDTO login(LoginDTO loginDTO);

    /**
     * 2. 用户注册
     *
     * @param registerDTO 注册参数
     * @return 是否注册成功
     */
    Boolean register(RegisterDTO registerDTO);

    /**
     * 3. 用户退出登录
     *
     * @param token JWT令牌
     * @return 是否退出成功
     */
    Boolean logout(String token);
}
