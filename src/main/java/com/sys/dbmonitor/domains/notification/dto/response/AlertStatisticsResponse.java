package com.sys.dbmonitor.domains.notification.dto.response;

import lombok.Builder;

/**
 * 알림 통계 응답 DTO
 * 카테고리별 알림 개수와 어제 대비 증감 정보를 포함합니다.
 */
@Builder
public record AlertStatisticsResponse(
    Long normal,           // 정상 개수
    Long normalChange,     // 어제 대비 증감
    Long warning,          // 주의 개수
    Long warningChange,    // 어제 대비 증감
    Long danger,           // 위험 개수
    Long dangerChange,     // 어제 대비 증감
    Long critical,         // 치명 개수
    Long criticalChange    // 어제 대비 증감
) {}

