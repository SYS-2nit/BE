package com.sys.dbmonitor.domains.instance.dto.request;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.global.common.util.PasswordEncryptionUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "타겟 데이터베이스 등록 요청")
public record InstanceCreateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Schema(description = "타겟 DB 이름", example = "Production Oracle DB")
        String name,

        @NotBlank(message = "URL은 필수입니다.")
        @Schema(description = "Oracle JDBC URL", example = "jdbc:oracle:thin:@localhost:1521:ORCL")
        String url,

        @NotBlank(message = "사용자명은 필수입니다.")
        @Schema(description = "사용자명", example = "scott")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Schema(description = "비밀번호", example = "tiger")
        String password,

        @Schema(description = "활성화 여부", example = "true")
        Boolean isActive
) {

        public Instance toEntity(String encryptionKey, Member member) {
                return Instance.builder()
                        .name(this.name)
                        .url(this.url)
                        .username(this.username)
                        .password(PasswordEncryptionUtil.encrypt(this.password, encryptionKey))  // AES 암호화로 저장
                        .isActive(this.isActive != null ? this.isActive : true)
                        .member(member)
                        .build();
        }

}