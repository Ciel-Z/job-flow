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
     * 处理自定义异常
     */
    @ExceptionHandler(ServiceException.class)
    public Result<String> handleResourceNotFoundException(ServiceException e) {
        log.error("ExceptionHandler {}", e.getMessage(), e);
        return Result.error(-1, e.getMessage());
    }
}
