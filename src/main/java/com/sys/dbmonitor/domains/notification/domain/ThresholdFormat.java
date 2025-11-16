package com.sys.dbmonitor.domains.notification.domain;

import lombok.Getter;

/**
 * 임계치 입력 포맷 정의
 */
@Getter
public enum ThresholdFormat {
    PERCENT("%"),
    MS("MS"),
    MBPS("MBPS"),
    COUNT("COUNT");

    private final String code;

    ThresholdFormat(String code) {
        this.code = code;
    }

    public static ThresholdFormat fromCode(String code) {
        for (ThresholdFormat format : values()) {
            if (format.code.equalsIgnoreCase(code)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown ThresholdFormat code: " + code);
    }
}

