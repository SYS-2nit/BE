package com.sys.dbmonitor.global.common.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class ApiResponse<T> {

    public static final String EMPTY = "";
    public static final int OK_CODE = 200;
    public static final int CREATED_CODE = 201;
    public static final String DEFAULT_MESSAGE = "요청이 성공적으로 처리되었습니다.";

    private int statusCode;
    private String message;
    private T data;

    public static <T> ApiResponse<String> ok() {
        return new ApiResponse<>(OK_CODE, DEFAULT_MESSAGE, null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(OK_CODE, DEFAULT_MESSAGE, data);
    }

    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(OK_CODE, message, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(OK_CODE, message, data);
    }

    public static <T> ApiResponse<T> ok(int statusCode, T data, String message) {
        return new ApiResponse<>(statusCode, message, data);
    }

    public static ApiResponse<String> okWithoutData(int statusCode, String message) {
        return new ApiResponse<>(statusCode, message, EMPTY);
    }

    public static ApiResponse<String> okWithoutData() {
        return new ApiResponse<>(OK_CODE, DEFAULT_MESSAGE, EMPTY);
    }

    // 리팩토링 고민: @ResponseStatus 를 통해 HTTP code를 설정할 수 있기 때문에 응답에서는 code 값이 불필요 (프론트엔드와의 컨벤션 문제)
    public static <T> ApiResponse<String> created() {
        return new ApiResponse<>(CREATED_CODE, DEFAULT_MESSAGE, null);
    }

    private ApiResponse(int statusCode, String message, T data) {
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }
}
