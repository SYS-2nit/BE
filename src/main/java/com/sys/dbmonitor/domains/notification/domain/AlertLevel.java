/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 심각도 레벨
 * 0: RECOVERY (복구)
 * 1: WARNING (경고)
 * 2: DANGER (위험)
 * 3: CRITICAL (치명)
 */
@Getter
@RequiredArgsConstructor
public enum AlertLevel {
    RECOVERY(0, "복구"),
    WARNING(1, "경고"),
    DANGER(2, "위험"),
    CRITICAL(3, "치명");

    private final int value;
    private final String description;

    public static AlertLevel fromValue(int value) {
        for (AlertLevel level : values()) {
            if (level.value == value) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid AlertLevel value: " + value);
    }
}

