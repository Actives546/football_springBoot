package com.sports.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SmsTypeEnum {

    LOGIN("login", "登录"),
    REGISTER("register", "注册");

    private final String code;
    private final String description;

    public static SmsTypeEnum getByCode(String code) {
        for (SmsTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
