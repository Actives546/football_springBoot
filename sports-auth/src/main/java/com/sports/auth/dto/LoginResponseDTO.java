package com.sports.auth.dto;

import com.sports.common.dto.UserDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 登录响应DTO
 */
@Data
@ApiModel(description = "登录响应数据")
public class LoginResponseDTO {

    @ApiModelProperty(value = "JWT令牌")
    private String token;

    @ApiModelProperty(value = "令牌类型")
    private String tokenType;

    @ApiModelProperty(value = "过期时间（毫秒）")
    private Long expiresIn;

    @ApiModelProperty(value = "用户信息")
    private UserDTO user;
}
