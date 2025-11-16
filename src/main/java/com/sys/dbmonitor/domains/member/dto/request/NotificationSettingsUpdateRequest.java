package com.sys.dbmonitor.domains.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "알림 설정 업데이트 요청")
public record NotificationSettingsUpdateRequest(
        @Schema(description = "이메일 주소", example = "user@example.com")
        String email,

        @Schema(description = "Slack 웹훅 URL", example = "https://hooks.slack.com/services/YOUR/WEBHOOK/URL")
        String slackAddress,

        @Pattern(regexp = "^(email|slack)$", message = "warningChannel은 'email' 또는 'slack'이어야 합니다.")
        @Schema(description = "주의(WARNING) 알림 채널", example = "email", allowableValues = {"email", "slack"})
        String warningChannel,

        @Pattern(regexp = "^(email|slack)$", message = "dangerChannel은 'email' 또는 'slack'이어야 합니다.")
        @Schema(description = "위험(DANGER) 알림 채널", example = "email", allowableValues = {"email", "slack"})
        String dangerChannel,

        @Pattern(regexp = "^(email|slack)$", message = "criticalChannel은 'email' 또는 'slack'이어야 합니다.")
        @Schema(description = "치명(CRITICAL) 알림 채널", example = "slack", allowableValues = {"email", "slack"})
        String criticalChannel
) {
}

