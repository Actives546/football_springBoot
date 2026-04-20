package com.sports.auth.service;

import com.sports.auth.dto.LoginByPhoneDTO;
import com.sports.auth.dto.LoginByUsernameDTO;
import com.sports.auth.dto.LoginResponseDTO;
import com.sports.auth.dto.RegisterDTO;

/**
 * 鉴权服务接口
 */
public interface AuthService {

    /**
     * 用户名密码登录
     *
     * @param loginDTO 登录参数
     * @return 登录响应
     */
    LoginResponseDTO loginByUsername(LoginByUsernameDTO loginDTO);

    /**
     * 手机号验证码登录
     *
     * @param loginDTO 登录参数
     * @return 登录响应
     */
    LoginResponseDTO loginByPhone(LoginByPhoneDTO loginDTO);

    /**
     * 注册
     *
     * @param registerDTO 注册参数
     * @return 是否注册成功
     */
    Boolean register(RegisterDTO registerDTO);

    /**
     * 退出登录
     *
     * @param token JWT令牌
     * @return 是否退出成功
     */
    Boolean logout(String token);
}
