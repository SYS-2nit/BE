/*
******************************************************************
작성자: 배지원
******************************************************************
*/
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
    TEST_NOT_FOUND("테스트를 찾을 수 없습니다"),
    
    // SwingBench 관련
    SCENARIO_NOT_FOUND("시나리오를 찾을 수 없습니다."),

    // Diagnosis 공통
    INVALID_REQUEST("잘못된 요청입니다."),
    INVALID_DURATION("유효하지 않은 지속 시간입니다."),
    INVALID_SCENARIO_IDS("유효한 시나리오가 없습니다."),
    DIAGNOSIS_ALREADY_RUNNING("이미 진단이 실행 중입니다. 먼저 정지하세요."),

    // 공통
    NOT_FOUND("요청한 리소스를 찾을 수 없습니다."),
    DUPLICATE_VALUE("중복된 값입니다."),

    // DB 데이터 정보
    DB_NOT_ACTIVE("활성화 된 DB가 없습니다"),
    
    // DB 연결 관련
    DB_CONNECTION_FAILED("데이터베이스 연결에 실패했습니다."),
    DB_CONNECTION_TEST_FAILED("데이터베이스 연결 테스트에 실패했습니다."),
    DB_INVALID_CREDENTIALS("데이터베이스 인증 정보가 올바르지 않습니다."),
    DB_INVALID_URL("데이터베이스 연결 URL이 올바르지 않습니다."),
    DB_UNSUPPORTED_TYPE("지원하지 않는 데이터베이스 타입입니다."),
    DB_NAME_ALREADY_EXISTS("이미 존재하는 데이터베이스 이름입니다."),
    DB_INFO_NOT_FOUND("데이터베이스 정보를 찾을 수 없습니다."),
    DB_INSTANCE_NOT_FOUND("데이터베이스 인스턴스를 찾을 수 없습니다."),



    ;
    private final String message;
}
