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
 */
@RestController
@RequestMapping("/sms")
@Api(tags = "短信验证码接口")
public class SmsController {

    @Autowired
    private SmsService smsService;

    @PostMapping("/send")
    @ApiOperation("发送验证码")
    public Result<Boolean> sendCode(
            @ApiParam(value = "手机号", required = true)
            @RequestParam String phone,
            @ApiParam(value = "验证码类型（login:登录, register:注册）", required = true)
            @RequestParam String type) {
        Boolean result = smsService.sendCode(phone, type);
        return Result.success(result);
    }
}
