package com.sports.auth.service;

/**
 * 短信验证码服务接口
 */
public interface SmsService {

    /**
     * 1. 发送验证码
     * 支持登录验证码和注册验证码两种类型
     *
     * @param phone 手机号
     * @param type  验证码类型（login:登录, register:注册）
     * @return 是否发送成功
     */
    Boolean sendCode(String phone, String type);

    /**
     * 2. 验证验证码
     * 用于登录和注册时验证用户输入的验证码是否正确
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @param type  验证码类型（login:登录, register:注册）
     * @return 是否验证通过
     */
    Boolean validateCode(String phone, String code, String type);
}
