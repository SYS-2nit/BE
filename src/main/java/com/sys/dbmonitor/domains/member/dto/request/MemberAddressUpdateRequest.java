/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 주소 정보 업데이트 요청 (email로 회원 찾기, null 값은 기존 값 유지)")
public record MemberAddressUpdateRequest(
        @Schema(description = "이메일 주소", example = "john@example.com")
        String email,

        @Schema(description = "Slack 주소", example = "https://hooks.slack.com/services/...")
        String slackAddress,

        @Schema(description = "경고 채널 ", example = "#email")
        String warningChannel,

        @Schema(description = "심각 채널", example = "#slack")
        String criticalChannel
) {
}
