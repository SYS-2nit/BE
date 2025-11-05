package com.sys.dbmonitor.domains.member.dto.response;

import com.sys.dbmonitor.domains.member.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "회원 응답")
public record MemberResponse(
        @Schema(description = "회원 ID")
        Long id,

        @Schema(description = "사용자명")
        String username,

        @Schema(description = "이메일")
        String email,

        @Schema(description = "회사명")
        String company,

        @Schema(description = "Slack 주소")
        String slackAddress,

        @Schema(description = "경고 채널")
        String warningChannel,

        @Schema(description = "심각 채널")
        String criticalChannel,

        @Schema(description = "생성일시")
        LocalDateTime createdAt,

        @Schema(description = "수정일시")
        LocalDateTime updatedAt
) {
    /**
     * Member 엔티티를 MemberResponse로 변환
     */
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getUsername(),
                member.getEmail(),
                member.getCompany(),
                member.getSlackAddress(),
                member.getWarningChannel(),
                member.getCriticalChannel(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
