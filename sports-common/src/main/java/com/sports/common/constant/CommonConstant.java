package com.sports.common.constant;

public class CommonConstant {

    public static final int USER_STATUS_DISABLED = 0;
    public static final int USER_STATUS_ACTIVE = 1;

    public static final int PHONE_LENGTH = 11;
    public static final int VERIFICATION_CODE_LENGTH = 6;
    public static final long VERIFICATION_CODE_EXPIRE_MINUTES = 5;

    public static final int JWT_SECRET_MIN_LENGTH = 32;

    public static final String JWT_DEFAULT_SECRET = "sports-cloud-jwt-default-secret-key-2024-very-long-and-secure";
    public static final long JWT_DEFAULT_EXPIRATION = 86400000L;
    public static final String JWT_DEFAULT_HEADER = "Authorization";
    public static final String JWT_DEFAULT_PREFIX = "Bearer";

    public static final String GENDER_UNKNOWN = "0";
    public static final String GENDER_MALE = "1";
    public static final String GENDER_FEMALE = "2";

    public static final String DEFAULT_NICKNAME_PREFIX = "用户";
}
