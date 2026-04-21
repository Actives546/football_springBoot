package com.sports.common.entity;

import com.sports.common.constant.HttpStatusConstant;
import com.sports.common.constant.MessageConstant;
import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String message;

    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(HttpStatusConstant.SUCCESS);
        result.setMessage(MessageConstant.SUCCESS);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        return error(HttpStatusConstant.INTERNAL_SERVER_ERROR, message);
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
