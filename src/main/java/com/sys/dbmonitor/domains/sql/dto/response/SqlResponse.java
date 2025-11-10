package com.sys.dbmonitor.domains.sql.dto.response;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "SQL 데이터 응답")
public record SqlResponse(

        @Schema(description = "SQL 데이터 ID")
        Long id,

        @Schema(description = "Instance 식별자")
        Long instanceId,

        @Schema(description = "SQL 관련 필드3")
        String field3,

        @Schema(description = "SQL 관련 필드4")
        String field4,

        @Schema(description = "SQL 관련 필드5")
        String field5,

        @Schema(description = "삭제 여부")
        Boolean isDeleted,

        @Schema(description = "생성일시")
        LocalDateTime createdAt,

        @Schema(description = "수정일시")
        LocalDateTime updatedAt
) {
    /**
     * SqlData 엔티티를 SqlResponse로 변환
     */
    public static SqlResponse from(Sql sqlData) {
        return new SqlResponse(
                sqlData.getId(),
                sqlData.getInstanceId(),
                sqlData.getField3(),
                sqlData.getField4(),
                sqlData.getField5(),
                sqlData.getIsDeleted(),
                sqlData.getCreatedAt(),
                sqlData.getUpdatedAt()
        );
    }
}
