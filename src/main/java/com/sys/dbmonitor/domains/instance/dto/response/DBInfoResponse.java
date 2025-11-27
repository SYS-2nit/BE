/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.instance.dto.response;

import com.sys.dbmonitor.domains.instance.domain.DBInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "DBInfo 응답")
public record DBInfoResponse(
        @Schema(description = "DBInfo ID")
        Long id,

        @Schema(description = "DB 이름")
        String name,

        @Schema(description = "데이터베이스 타입")
        String type,

        @Schema(description = "버전")
        String version,

        @Schema(description = "IP 주소")
        String ip,

        @Schema(description = "포트 번호")
        Integer port,

        @Schema(description = "사용자명")
        String username,

        @Schema(description = "활성화 여부")
        Boolean isActive,

        @Schema(description = "마지막 접속 시간")
        LocalDateTime finalAt,

        @Schema(description = "생성일시")
        LocalDateTime createdAt,

        @Schema(description = "수정일시")
        LocalDateTime updatedAt
) {
    public static DBInfoResponse from(DBInfo dbInfo) {
        return new DBInfoResponse(
                dbInfo.getId(),
                dbInfo.getName(),
                dbInfo.getType(),
                dbInfo.getVersion(),
                dbInfo.getIp(),
                dbInfo.getPort(),
                dbInfo.getUserName(),
                dbInfo.getIsActive(),
                dbInfo.getFinalAt(),
                dbInfo.getCreatedAt(),
                dbInfo.getUpdatedAt()
        );
    }
}

