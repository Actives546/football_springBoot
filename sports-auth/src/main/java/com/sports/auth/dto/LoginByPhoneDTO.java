package com.sports.auth.dto;

import com.sports.common.constant.MessageConstant;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@ApiModel(description = "手机号验证码登录请求参数")
public class LoginByPhoneDTO {

    @ApiModelProperty(value = "手机号", required = true)
    @NotBlank(message = MessageConstant.PHONE_NOT_BLANK)
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = MessageConstant.PHONE_FORMAT_ERROR)
    private String phone;

    @ApiModelProperty(value = "验证码", required = true)
    @NotBlank(message = MessageConstant.CODE_NOT_BLANK)
    private String code;
}
