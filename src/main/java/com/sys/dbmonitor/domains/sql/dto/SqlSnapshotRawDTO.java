package com.sys.dbmonitor.domains.sql.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

/**************************************************
 작성자 : 오수경
 *************************************************/

/**
 * 수집된 SQL 스냅샷 원시 데이터 DTO
 */
@Getter
@Builder
public class SqlSnapshotRawDTO {
    private String sqlId;
    private Long planHashValue;
    private Long executionsTot;
    private Long elapsedTimeUsTot;
    private Long cpuTimeUsTot;
    private Long waitTimeUsTot;
    private Long bufferGetsTot;
    private Long diskReadsTot;
    private Long userIoWaitUsTot;
    private Long concurrencyWaitUsTot;
    private Long applicationWaitUsTot;
    private Long clusterWaitUsTot;
    private Long plsqlExecUsTot;
    private Long javaExecUsTot;
    private LocalDateTime lastActiveTimeMax;
    private String parsingSchemaNameAny;
    private String moduleAny;
    private String sqlText;
    private String planTextClob;
}

