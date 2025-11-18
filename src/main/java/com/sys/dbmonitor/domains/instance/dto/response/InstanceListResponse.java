package com.sys.dbmonitor.domains.instance.dto.response;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.dto.InstanceDataDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "DB 인스턴스 목록 응답")
public record InstanceListResponse(
        @Schema(description = "인스턴스 ID")
        Long id,

        @Schema(description = "상태")
        String status,

        @Schema(description = "서버명")
        String serverName,

        @Schema(description = "IP 주소")
        String ip,

        @Schema(description = "포트 번호")
        Integer port,

        @Schema(description = "데이터베이스 이름")
        String databaseName,

        @Schema(description = "SID")
        String sid,

        @Schema(description = "CPU 사용률")
        String cpuUsage,

        @Schema(description = "세션 수")
        String sessionCount,

        @Schema(description = "Active Session 수")
        String activeSessionCount,

        @Schema(description = "Lock Wait")
        String lockWait,

        @Schema(description = "PGA")
        String pga,

        @Schema(description = "SGA")
        String sga,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt
) {

    public static InstanceListResponse from(Instance instance) {
        return from(instance, null);
    }

    public static InstanceListResponse from(Instance instance, InstanceDataDTO instanceData) {
        var dbInfo = instance.getDbInfo();

        String status = "비활성";
        String serverName = null;
        String ip = null;
        Integer port = null;
        String databaseName = null;

        if (dbInfo != null) {
            status = Boolean.TRUE.equals(dbInfo.getIsActive()) ? "정상" : "비활성";
            serverName = dbInfo.getName();
            ip = dbInfo.getIp();
            port = dbInfo.getPort();
            databaseName = dbInfo.getName();
        }

        // InstanceDataDTO가 있으면 해당 값 사용, 없으면 null
        String cpuUsage = instanceData != null ? instanceData.cpuUsage() : null;
        String sessionCount = instanceData != null ? instanceData.sessionCount() : null;
        String activeSessionCount = instanceData != null ? instanceData.activeSessionCount() : null;
        String lockWait = instanceData != null ? instanceData.lockWait() : null;
        String pga = instanceData != null ? instanceData.pga() : null;
        String sga = instanceData != null ? instanceData.sga() : null;

        return new InstanceListResponse(
                instance.getId(),
                status,
                serverName,
                ip,
                port,
                databaseName,
                instance.getSid(),
                cpuUsage,
                sessionCount,
                activeSessionCount,
                lockWait,
                pga,
                sga,
                instance.getCreatedAt()
        );
    }
}


