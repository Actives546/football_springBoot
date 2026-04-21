package com.sports.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoginTypeEnum {

    USERNAME("username", "用户名密码登录"),
    PHONE("phone", "手机号验证码登录");

    private final String code;
    private final String description;

    public static LoginTypeEnum getByCode(String code) {
        for (LoginTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
