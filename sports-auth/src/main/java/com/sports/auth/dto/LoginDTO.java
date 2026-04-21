package com.sports.auth.dto;

import com.sports.common.constant.MessageConstant;
import com.sports.common.enums.LoginTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@ApiModel(description = "统一登录请求参数")
public class LoginDTO {

    @ApiModelProperty(value = "登录类型：username-用户名密码登录，phone-手机号验证码登录", required = true, example = "username")
    @NotBlank(message = "登录类型不能为空")
    private String loginType;

    @ApiModelProperty(value = "用户名（用户名密码登录时必填）", example = "admin")
    private String username;

    @ApiModelProperty(value = "密码（用户名密码登录时必填）", example = "123456")
    private String password;

    @ApiModelProperty(value = "手机号（手机号验证码登录时必填）", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = MessageConstant.PHONE_FORMAT_ERROR)
    private String phone;

    @ApiModelProperty(value = "验证码（手机号验证码登录时必填）", example = "123456")
    private String code;

    public boolean isUsernameLogin() {
        return LoginTypeEnum.USERNAME.getCode().equals(loginType);
    }

    public boolean isPhoneLogin() {
        return LoginTypeEnum.PHONE.getCode().equals(loginType);
    }
}
