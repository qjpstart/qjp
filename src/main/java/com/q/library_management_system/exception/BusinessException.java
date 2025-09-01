package com.q.library_management_system.exception;

// 自定义业务异常，用于处理各种业务规则校验失败的情况
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}