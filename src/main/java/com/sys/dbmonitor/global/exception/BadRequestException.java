package com.sys.dbmonitor.global.exception;

public class BadRequestException extends RuntimeException {
    private final ExceptionMessage exceptionMessage;

    public BadRequestException(ExceptionMessage exceptionMessage) {
        super(exceptionMessage.getMessage());
        this.exceptionMessage = exceptionMessage;
    }

    public BadRequestException(ExceptionMessage exceptionMessage, String customMessage) {
        super(customMessage);
        this.exceptionMessage = exceptionMessage;
    }
}
