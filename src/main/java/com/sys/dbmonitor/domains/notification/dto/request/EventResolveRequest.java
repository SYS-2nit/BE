package com.sys.dbmonitor.domains.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 이벤트 해결 요청 DTO
 * - memberId: 해결 처리하는 사용자 ID
 */
@Getter
@Setter
@Schema(description = "이벤트 해결 요청")
public class EventResolveRequest {

    @NotNull(message = "memberId는 필수입니다.")
    @Schema(description = "해결 처리하는 사용자 ID", example = "3", required = true)
    private Long memberId;
}

