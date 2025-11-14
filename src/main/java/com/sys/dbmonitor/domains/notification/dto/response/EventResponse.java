package com.sys.dbmonitor.domains.notification.dto.response;

import com.sys.dbmonitor.domains.notification.domain.AlertStatus;
import com.sys.dbmonitor.domains.notification.domain.Event;
import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "알림 이벤트 응답")
public class EventResponse {

    @Schema(description = "이벤트 ID", example = "100")
    private final Long id;

    @Schema(description = "알림 규칙 ID", example = "55")
    private final Long alertEventId;

    @Schema(description = "인스턴스 ID", example = "1")
    private final Long instanceId;

    @Schema(description = "정책 생성자(멤버) ID", example = "3")
    private final Long memberId;

    @Schema(description = "알림 상태", example = "PENDING")
    private final AlertStatus status;

    @Schema(description = "심각도", example = "3")
    private final Integer severity;

    @Schema(description = "현재 값", example = "120.0")
    private final Double currentValue;

    @Schema(description = "임계값", example = "100.0")
    private final Double thresholdValue;

    @Schema(description = "임계치 포맷", example = "MBPS")
    private final ThresholdFormat thresholdFormat;

    @Schema(description = "메시지", example = "Redo Log 생성량이 120.00 MB/s로 임계값을 초과했습니다.")
    private final String message;

    @Schema(description = "확인 일시")
    private final LocalDateTime acknowledgedAt;

    @Schema(description = "확인자 ID")
    private final Long acknowledgedBy;

    @Schema(description = "해결 일시")
    private final LocalDateTime resolvedAt;

    @Schema(description = "해결자 ID")
    private final Long resolvedBy;

    @Schema(description = "생성 일시")
    private final LocalDateTime createdAt;

    @Schema(description = "수정 일시")
    private final LocalDateTime updatedAt;

    public static EventResponse from(Event event) {
        return EventResponse.builder()
            .id(event.getId())
            .alertEventId(event.getAlertEvent() != null ? event.getAlertEvent().getId() : null)
            .instanceId(event.getInstance() != null ? event.getInstance().getId() : null)
            .memberId(event.getMember() != null ? event.getMember().getId() : null)
            .status(event.getStatus())
            .severity(event.getSeverity())
            .currentValue(event.getCurrentValue())
            .thresholdValue(event.getThresholdValue())
            .thresholdFormat(event.getThresholdFormat())
            .message(event.getMessage())
            .acknowledgedAt(event.getAcknowledgedAt())
            .acknowledgedBy(event.getAcknowledgedBy() != null ? event.getAcknowledgedBy().getId() : null)
            .resolvedAt(event.getResolvedAt())
            .resolvedBy(event.getResolvedBy() != null ? event.getResolvedBy().getId() : null)
            .createdAt(event.getCreatedAt())
            .updatedAt(event.getUpdatedAt())
            .build();
    }
}

