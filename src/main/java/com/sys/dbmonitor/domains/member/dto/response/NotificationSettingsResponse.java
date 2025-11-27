/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.member.dto.response;

import com.sys.dbmonitor.domains.member.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 설정 응답")
public record NotificationSettingsResponse(
        @Schema(description = "이메일 주소", example = "user@example.com")
        String email,

        @Schema(description = "Slack 웹훅 URL", example = "https://hooks.slack.com/services/YOUR/WEBHOOK/URL")
        String slackAddress,

        @Schema(description = "주의(WARNING) 알림 채널", example = "email")
        String warningChannel,

        @Schema(description = "위험(DANGER) 알림 채널", example = "email")
        String dangerChannel,

        @Schema(description = "치명(CRITICAL) 알림 채널", example = "slack")
        String criticalChannel
) {
    public static NotificationSettingsResponse from(Member member) {
        return new NotificationSettingsResponse(
                member.getEmail(),
                member.getSlackAddress(),
                member.getWarningChannel(),
                member.getDangerChannel(),
                member.getCriticalChannel()
        );
    }
}

