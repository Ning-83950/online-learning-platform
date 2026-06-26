package com.onlinelearning.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<T>(200, "操作成功", data);
    }

    public static ApiResult<Void> ok() {
        return new ApiResult<Void>(200, "操作成功", null);
    }

    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<T>(500, message, null);
    }
}
