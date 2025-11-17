package com.sys.dbmonitor.domains.notification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 누적 시간 옵션
 * 메트릭 수집 주기가 1분이므로 최소 누적 시간은 1분
 * 연속으로 임계값을 초과한 횟수가 지정된 시간 동안 지속되어야 알림 발생
 */
@Getter
@RequiredArgsConstructor
public enum DelayTime {
    ONE_MINUTE("1M", 1, "1분"),
    FIVE_MINUTES("5M", 5, "5분"),
    TEN_MINUTES("10M", 10, "10분"),
    ONE_HOUR("1H", 60, "1시간");

    private final String code;
    private final int minutes; // 연속 초과 횟수 (메트릭 수집 주기 1분 기준)
    private final String description;

    public static DelayTime fromCode(String code) {
        for (DelayTime delayTime : values()) {
            if (delayTime.code.equals(code)) {
                return delayTime;
            }
        }
        throw new IllegalArgumentException("Invalid DelayTime code: " + code);
    }
}

