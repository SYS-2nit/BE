/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 이벤트 상태
 * PENDING: 미처리
 * CLOSED: 해결됨
 */
@Getter
@RequiredArgsConstructor
public enum AlertStatus {
    PENDING("PENDING", "미처리"),
    CLOSED("CLOSED", "해결됨");

    private final String code;
    private final String description;

    public static AlertStatus fromCode(String code) {
        for (AlertStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid AlertStatus code: " + code);
    }
}

