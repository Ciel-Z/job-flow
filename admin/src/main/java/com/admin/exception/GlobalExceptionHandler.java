package com.admin.exception;

import com.admin.entity.Result;
import com.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义的资源未找到异常
     */
    @ExceptionHandler(ServiceException.class)
    public Result<String> handleResourceNotFoundException(ServiceException ex) {
        log.error("ExceptionHandler e: ", ex);
        return Result.error(-1, ex.getMessage());
    }
}
