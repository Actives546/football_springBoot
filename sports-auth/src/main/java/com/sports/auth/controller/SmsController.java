package com.sports.auth.controller;

import com.sports.auth.service.SmsService;
import com.sports.common.entity.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信验证码控制器
 * 提供发送验证码接口
 */
@RestController
@RequestMapping("/")
@Api(tags = "短信验证码接口")
public class SmsController {

    // 注入短信验证码服务
    @Autowired
    private SmsService smsService;

    /**
     * 1. 发送验证码接口
     * 支持登录验证码和注册验证码两种类型
     *
     * @param phone 手机号，必须是11位数字
     * @param type  验证码类型，可选值：login-登录，register-注册
     * @return 是否发送成功
     */
    @PostMapping("/send")
    @ApiOperation("发送验证码")
    public Result<Boolean> sendCode(
            @ApiParam(value = "手机号", required = true)
            @RequestParam String phone,
            @ApiParam(value = "验证码类型（login:登录, register:注册）", required = true)
            @RequestParam String type) {
        // 调用短信服务发送验证码
        Boolean result = smsService.sendCode(phone, type);
        // 返回成功响应
        return Result.success(result);
    }
}
