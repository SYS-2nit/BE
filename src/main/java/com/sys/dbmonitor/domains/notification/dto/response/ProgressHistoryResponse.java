package com.sys.dbmonitor.domains.notification.dto.response;

import com.sys.dbmonitor.domains.notification.domain.ProgressHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "알림 처리 이력 응답")
public class ProgressHistoryResponse {

    @Schema(description = "이력 ID", example = "15")
    private final Long id;

    @Schema(description = "이벤트 ID", example = "100")
    private final Long eventId;

    @Schema(description = "이력 내용", example = "장애 원인 분석 진행 중")
    private final String content;

    @Schema(description = "작성자 ID", example = "3")
    private final Long createdBy;

    @Schema(description = "생성 일시")
    private final LocalDateTime createdAt;

    @Schema(description = "수정 일시")
    private final LocalDateTime updatedAt;

    public static ProgressHistoryResponse from(ProgressHistory history) {
        return ProgressHistoryResponse.builder()
            .id(history.getId())
            .eventId(history.getEvent() != null ? history.getEvent().getId() : null)
            .content(history.getContent())
            .createdBy(history.getCreatedBy() != null ? history.getCreatedBy().getId() : null)
            .createdAt(history.getCreatedAt())
            .updatedAt(history.getUpdatedAt())
            .build();
    }
}

