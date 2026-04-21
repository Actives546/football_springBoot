package com.sports.auth.dto;

import com.sports.common.constant.MessageConstant;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(description = "用户名密码登录请求参数")
public class LoginByUsernameDTO {

    @ApiModelProperty(value = "用户名", required = true)
    @NotBlank(message = MessageConstant.USERNAME_NOT_BLANK)
    private String username;

    @ApiModelProperty(value = "密码", required = true)
    @NotBlank(message = MessageConstant.PASSWORD_NOT_BLANK)
    private String password;
}
