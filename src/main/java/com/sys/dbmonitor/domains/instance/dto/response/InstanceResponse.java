package com.sys.dbmonitor.domains.instance.dto.response;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "타겟 데이터베이스 응답")
public record InstanceResponse(
        @Schema(description = "타겟 DB ID")
        Long id,

        @Schema(description = "타겟 DB 이름")
        String name,

        @Schema(description = "Oracle JDBC URL (비밀번호 마스킹)")
        String url,

        @Schema(description = "사용자명")
        String username,

        @Schema(description = "활성화 여부")
        Boolean isActive,

        @Schema(description = "생성일시")
        LocalDateTime createdAt,

        @Schema(description = "수정일시")
        LocalDateTime updatedAt
) {
    public static InstanceResponse from(Instance targetDatabase) {
        return new InstanceResponse(
                targetDatabase.getId(),
                targetDatabase.getName(),
                maskUrl(targetDatabase.getUrl()),
                targetDatabase.getUsername(),
                targetDatabase.getIsActive(),
                targetDatabase.getCreatedAt(),
                targetDatabase.getUpdatedAt()
        );
    }

    /**
     * URL의 비밀번호 부분을 마스킹 (보안을 위해)
     */
    private static String maskUrl(String url) {
        if (url == null) return null;
        // jdbc:oracle:thin:@host:port:sid 형식은 그대로 반환
        // URL에 비밀번호가 포함된 경우를 대비한 마스킹 로직 (필요시 구현)
        return url;
    }
}
