package com.newworld.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一响应体
 */
@Schema(description = "统一返回结果")
public class Result<T> {

    @Schema(description = "状态码")
    private Integer code;

    @Schema(description = "消息")
    private String msg;

    @Schema(description = "数据")
    private T data;

    private Result() {}

    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "操作成功";
        return r;
    }

    public static <T> Result<T> success(String msg) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = msg;
        return r;
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "操作成功";
        r.data = data;
        return r;
    }

    public static <T> Result<T> success(String msg, T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = msg;
        r.data = data;
        return r;
    }

    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.code = 500;
        r.msg = msg;
        return r;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
