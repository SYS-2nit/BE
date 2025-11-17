package com.sys.dbmonitor.domains.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "알림 테스트 요청")
public record NotificationTestRequest(
        @Schema(description = "테스트할 채널 목록 (없으면 email과 slack 모두 테스트)", 
                example = "[\"email\", \"slack\"]",
                allowableValues = {"email", "slack"})
        List<String> channels
) {
}

