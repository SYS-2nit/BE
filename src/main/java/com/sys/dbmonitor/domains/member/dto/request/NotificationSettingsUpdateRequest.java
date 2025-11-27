/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "알림 설정 업데이트 요청")
public record NotificationSettingsUpdateRequest(
        @Schema(description = "이메일 주소", example = "user@example.com")
        String email,

        @Schema(description = "Slack 웹훅 URL", example = "https://hooks.slack.com/services/YOUR/WEBHOOK/URL")
        String slackAddress,

        @Pattern(regexp = "^(email|slack|all)$", message = "warningChannel은 'email', 'slack', 'all' 중 하나여야 합니다.")
        @Schema(description = "주의(WARNING) 알림 채널 (null=기본값, 'email'=이메일만, 'slack'=Slack만, 'all'=둘 다)", 
                example = "email", allowableValues = {"email", "slack", "all"})
        String warningChannel,

        @Pattern(regexp = "^(email|slack|all)$", message = "dangerChannel은 'email', 'slack', 'all' 중 하나여야 합니다.")
        @Schema(description = "위험(DANGER) 알림 채널 (null=기본값, 'email'=이메일만, 'slack'=Slack만, 'all'=둘 다)", 
                example = "email", allowableValues = {"email", "slack", "all"})
        String dangerChannel,

        @Pattern(regexp = "^(email|slack|all)$", message = "criticalChannel은 'email', 'slack', 'all' 중 하나여야 합니다.")
        @Schema(description = "치명(CRITICAL) 알림 채널 (null=기본값, 'email'=이메일만, 'slack'=Slack만, 'all'=둘 다)", 
                example = "slack", allowableValues = {"email", "slack", "all"})
        String criticalChannel
) {
}

