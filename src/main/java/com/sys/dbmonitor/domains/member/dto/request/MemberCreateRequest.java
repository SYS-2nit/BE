/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "회원 등록 요청")
public record MemberCreateRequest(
        @NotBlank(message = "사용자명은 필수입니다.")
        @Schema(description = "사용자명", example = "john_doe")
        String username,

//        @NotBlank(message = "비밀번호는 필수입니다.")
//        @Schema(description = "비밀번호", example = "password123")
//        String password,

        @NotBlank(message = "이메일은 필수입니다.")
        @Schema(description = "이메일", example = "john@example.com")
        String email,

        @NotBlank(message = "회사명은 필수입니다.")
        @Schema(description = "회사명", example = "ABC Company")
        String company
) {
}
