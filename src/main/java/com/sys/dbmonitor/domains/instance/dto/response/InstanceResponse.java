package com.sys.dbmonitor.domains.instance.dto.response;

import com.sys.dbmonitor.domains.instance.domain.DBInfo;
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
    public static InstanceResponse from(Instance instance) {
        // Instance가 DBInfo를 참조하는 경우
        if (instance.getDbInfo() != null) {
            com.sys.dbmonitor.domains.instance.domain.DBInfo dbInfo = instance.getDbInfo();
            // Instance에 저장된 URL 사용 (없으면 생성)
            String jdbcUrl = instance.getUrl() != null 
                    ? instance.getUrl() 
                    : dbInfo.generateJdbcUrl(
                            instance.getSid(), 
                            instance.getConnectionType() != null ? instance.getConnectionType() : "SID"
                    );
            return new InstanceResponse(
                    instance.getId(),
                    dbInfo.getName(),
                    maskUrl(jdbcUrl),
                    dbInfo.getUserName(),
                    dbInfo.getIsActive(),
                    instance.getCreatedAt(),
                    instance.getUpdatedAt()
            );
        }
        
        // 기존 구조 (하위 호환성)
        return new InstanceResponse(
                instance.getId(),
                null,  // name은 DBInfo에 있음
                instance.getUrl(),  // Instance에 저장된 URL 사용
                null,  // username은 DBInfo에 있음
                null,  // isActive는 DBInfo에 있음
                instance.getCreatedAt(),
                instance.getUpdatedAt()
        );
    }

    public static InstanceResponse from(DBInfo dbInfo) {
        return new InstanceResponse(
                dbInfo.getId(),
                dbInfo.getName(),
                String.format("jdbc:oracle:thin:@%s:%d", dbInfo.getIp(), dbInfo.getPort()),
                dbInfo.getUserName(),
                dbInfo.getIsActive(),
                dbInfo.getCreatedAt(),
                dbInfo.getUpdatedAt()
        );
    }


    private static String maskUrl(String url) {
        if (url == null) return null;
        return url;
    }
}
