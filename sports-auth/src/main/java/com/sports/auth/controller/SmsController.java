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
@RequestMapping("/")
@Api(tags = "短信验证码接口")
public class SmsController {

    @Autowired
    private SmsService smsService;

    /**
     * 1. 发送验证码
     * 支持登录验证码和注册验证码两种类型
     *
     * 1.1 验证手机号格式
     * 1.2 检查是否频繁发送（Redis中是否已有未过期的验证码）
     * 1.3 生成随机验证码
     * 1.4 将验证码存入Redis，设置过期时间
     * 1.5 日志记录验证码（实际生产环境应调用短信服务商API）
     *
     * @param phone 手机号
     * @param type  验证码类型（login:登录, register:注册）
     * @return 是否发送成功
     */
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
