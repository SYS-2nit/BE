package com.sys.dbmonitor.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


//도메인이 늘어나면 각각 도메인에 같은 클래스들을 만들어 사용예정
@RequiredArgsConstructor
@Getter
public enum ExceptionMessage {

    // 인증 관련
    AUTHENTICATION_FAILED("이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_PRINCIPAL_TYPE("유효하지 않은 인증입니다"),
    AUTHENTICATION_MISSING("인증에 실패했습니다."),
    LOGIN_FAILED("로그인에 실패했습니다."),
    TOKEN_REFRESH_FAILED("토큰 갱신에 실패했습니다."),


    ;


    private final String message;
}
