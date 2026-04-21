package com.sports.common.constant;

public class RedisKeyConstant {

    public static final String SMS_CODE_PREFIX = "sms:code:";
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    public static String getSmsCodeKey(String type, String phone) {
        return SMS_CODE_PREFIX + type + ":" + phone;
    }

    public static String getTokenBlacklistKey(String token) {
        return TOKEN_BLACKLIST_PREFIX + token;
    }
}
