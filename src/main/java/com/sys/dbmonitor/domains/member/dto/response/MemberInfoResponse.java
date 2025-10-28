package com.sys.dbmonitor.domains.member.dto.response;


public record MemberInfoResponse(
        String name,
        String email,
        String slackId,
        String company
) {
}