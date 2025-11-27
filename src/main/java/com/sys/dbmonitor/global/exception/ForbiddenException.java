/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.global.exception;

import lombok.Getter;

@Getter
public class ForbiddenException extends RuntimeException {
    private final ExceptionMessage exceptionMessage;

    public ForbiddenException(ExceptionMessage exceptionMessage) {
        super(exceptionMessage.getMessage());
        this.exceptionMessage = exceptionMessage;
    }
}


