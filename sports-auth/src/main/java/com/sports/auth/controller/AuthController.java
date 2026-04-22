package com.sports.auth.controller;

import com.sports.auth.dto.LoginDTO;
import com.sports.auth.dto.LoginResponseDTO;
import com.sports.auth.dto.RegisterDTO;
import com.sports.auth.service.AuthService;
import com.sports.common.entity.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 鉴权控制器
 * 提供用户登录、注册、退出登录等接口
 */
@RestController
@RequestMapping("/")
@Api(tags = "鉴权接口")
public class AuthController {

    // 注入鉴权服务
    @Autowired
    private AuthService authService;

    /**
     * 1. 用户登录接口
     * 支持用户名密码登录和手机号验证码登录两种方式
     *
     * @param loginDTO 登录参数，包含登录类型和对应凭证
     * @return 登录响应，包含JWT令牌和用户信息
     */
    @PostMapping("/login")
    @ApiOperation("用户登录")
    public Result<LoginResponseDTO> login(
            @ApiParam(value = "登录参数", required = true)
            @Validated @RequestBody LoginDTO loginDTO) {
        // 调用鉴权服务执行登录
        LoginResponseDTO response = authService.login(loginDTO);
        // 返回成功响应
        return Result.success(response);
    }

    /**
     * 2. 用户注册接口
     * 新用户注册，需要先获取验证码
     *
     * @param registerDTO 注册参数，包含用户名、密码、手机号、验证码等
     * @return 是否注册成功
     */
    @PostMapping("/register")
    @ApiOperation("用户注册")
    public Result<Boolean> register(
            @ApiParam(value = "注册参数", required = true)
            @Validated @RequestBody RegisterDTO registerDTO) {
        // 调用鉴权服务执行注册
        Boolean result = authService.register(registerDTO);
        // 返回成功响应
        return Result.success(result);
    }

    /**
     * 3. 用户退出登录接口
     * 将当前令牌加入黑名单，使其失效
     *
     * @param token JWT令牌，从请求头Authorization中获取
     * @return 是否退出成功
     */
    @PostMapping("/logout")
    @ApiOperation("退出登录")
    public Result<Boolean> logout(
            @ApiParam(value = "JWT令牌", required = true)
            @RequestHeader(value = "Authorization", required = false) String token) {
        // 调用鉴权服务执行退出登录
        Boolean result = authService.logout(token);
        // 返回成功响应
        return Result.success(result);
    }
}
