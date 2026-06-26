package com.onlinelearning.config;

import cn.dev33.satoken.exception.NotLoginException;
import com.onlinelearning.common.ApiResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotLoginException.class)
    public ApiResult<Void> handleNotLogin(NotLoginException e) {
        return new ApiResult<Void>(401, "登录已过期，请重新登录", null);
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handle(Exception e) {
        return ApiResult.fail(e.getMessage());
    }
}
