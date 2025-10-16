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

    // 쿠폰 관련
    COUPON_NOT_FOUND("쿠폰을 찾을 수 없습니다."),
    COUPON_SOLD_OUT("쿠폰이 모두 소진되었습니다."),
    COUPON_ALREADY_USED("이미 사용된 쿠폰입니다."),
    COUPON_NOT_USED("사용되지 않은 쿠폰입니다."),
    COUPON_ISSUE_TIMEOUT("쿠폰 발급 요청이 많습니다. 잠시 후 다시 시도해주세요."),
    COUPON_ISSUE_FAILED("쿠폰 발급 중 오류가 발생했습니다."),
    CouponAlreadyUsedException("이미 사용된 쿠폰입니다."),
    CouponExpiredException("만료된 쿠폰입니다."),


    // 테스트 관련
    TEST_NOT_FOUND("테스트를 찾을 수 없습니다")
    ;


    private final String message;
}
