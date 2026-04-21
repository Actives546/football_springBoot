package com.sports.common.exception;

import com.sports.common.constant.HttpStatusConstant;
import lombok.Getter;

@Getter
public class GatewayException extends RuntimeException {

    private final Integer code;

    public GatewayException(String message) {
        super(message);
        this.code = HttpStatusConstant.INTERNAL_SERVER_ERROR;
    }

    public GatewayException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public static GatewayException tokenMissing() {
        return new GatewayException(HttpStatusConstant.UNAUTHORIZED, "缺少令牌");
    }

    public static GatewayException tokenInvalid() {
        return new GatewayException(HttpStatusConstant.UNAUTHORIZED, "令牌无效");
    }

    public static GatewayException tokenExpired() {
        return new GatewayException(HttpStatusConstant.UNAUTHORIZED, "令牌已过期");
    }

    public static GatewayException secretNotConfigured() {
        return new GatewayException(HttpStatusConstant.INTERNAL_SERVER_ERROR, "JWT密钥未配置");
    }

    public static GatewayException ipNotAllowed() {
        return new GatewayException(HttpStatusConstant.FORBIDDEN, "IP地址不允许访问");
    }
}
