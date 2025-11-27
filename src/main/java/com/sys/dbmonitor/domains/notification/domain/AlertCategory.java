/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 카테고리
 */
@Getter
@RequiredArgsConstructor
public enum AlertCategory {
    CPU("CPU", "CPU"),
    MEMORY("MEMORY", "Memory"),
    SESSION("SESSION", "Session"),
    IO("IO", "I/O"),
    STORAGE("STORAGE", "Storage");

    private final String code;
    private final String description;

    public static AlertCategory fromCode(String code) {
        for (AlertCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Invalid AlertCategory code: " + code);
    }
}

