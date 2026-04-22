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
 */
@RestController
@RequestMapping("/")
@Api(tags = "鉴权接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 1. 用户登录
     * 支持用户名密码登录和手机号验证码登录两种方式
     */
    @PostMapping("/login")
    @ApiOperation("用户登录")
    public Result<LoginResponseDTO> login(
            @ApiParam(value = "登录参数", required = true)
            @Validated @RequestBody LoginDTO loginDTO) {
        LoginResponseDTO response = authService.login(loginDTO);
        return Result.success(response);
    }

    /**
     * 2. 用户注册
     */
    @PostMapping("/register")
    @ApiOperation("用户注册")
    public Result<Boolean> register(
            @ApiParam(value = "注册参数", required = true)
            @Validated @RequestBody RegisterDTO registerDTO) {
        Boolean result = authService.register(registerDTO);
        return Result.success(result);
    }

    /**
     * 3. 用户退出登录
     */
    @PostMapping("/logout")
    @ApiOperation("退出登录")
    public Result<Boolean> logout(
            @ApiParam(value = "JWT令牌", required = true)
            @RequestHeader(value = "Authorization", required = false) String token) {
        Boolean result = authService.logout(token);
        return Result.success(result);
    }
}
