/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 수정 요청 (username, email, company만 수정 가능)")
public record MemberUpdateRequest(
        @Schema(description = "사용자명", example = "john_doe")
        String username,

        @Schema(description = "이메일", example = "john@example.com")
        String email,

        @Schema(description = "회사명", example = "ABC Company")
        String company
) {
}
