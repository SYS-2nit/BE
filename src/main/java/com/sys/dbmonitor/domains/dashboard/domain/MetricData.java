// src/main/java/com/sys/dbmonitor/domains/dashboard/domain/MetricData.java
package com.sys.dbmonitor.domains.dashboard.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MetricData {
    // 공통 식별
    private Long id;
    private Long dbId;
    private Integer categoryId; // 1..6
    private Integer graphId;    // 1..53
    private LocalDateTime collectedAt;

    /* ===== Custom(1) / CPU(2) / Memory(3) / Session(4) / I/O(5) / Storage(6) Superset ===== */

    // --- CPU/Custom 공통 타일 ---
    private Double hostBusyCores;
    private Double hostTotalCores;
    private Double hostCpuUtilPct;
    private Double aasOncpuSessions;
    private Double coreBaselineSessions;
    private Double cpuSaturationPct;
    private Double dbOfHostSharePct;
    private Double runQPerCoreLoadProxy;
    private Double tpsPerSec;
    private Double execsPerSec;
    private Double userCallsPerSec;
    private Double otherProcessesPct;
    private Double loadThreshold;
    private Double loadThresholdMin;
    private Double loadThresholdMax;
    private Double cpuPerCommitMs;
    private Double cpuPerExecMs;
    private Double aasFgSessions;
    private Double aasBgSessions;

    // --- AAS/Wait Class ---
    private Double aasTotal;
    private Double waitClassAasUserIo;
    private Double waitClassAasCommit;
    private Double waitClassAasConcurrency;
    private Double waitClassAasSystemIo;
    private Double waitClassAasNetwork;
    private Double waitClassAasCluster;
    private Double waitClassAasOther;

    // --- I/O 지연/처리량 ---
    private Double singleBlockReadLatencyMs;
    private Double directPathReadLatencyMs;
    private Double directPathWriteLatencyMs;
    private Double physicalReadMbPerSec;
    private Double physicalWriteMbPerSec;

    // --- SGA 압박/히트율 ---
    private Double libraryCacheHitPct;
    private Double dictionaryCacheHitPct;
    private Double hardParseRatioPct;
    private Double bufferCacheHitPct;
    private Double latchHitPct;
    private Double redoBufferWaitPct;

    // --- Memory 풀/용량 ---
    private Double largePoolMb;
    private Double javaPoolMb;
    private Double logBufferMb;
    private Double bufferCacheMb;
    private Double libraryCacheMb;
    private Double dictionaryCacheMb;
    private Double sgaUsedBytes;
    private Double sgaTotalBytes;
    private Double sgaUtilPct;
    private Double sharedPoolFreeBytes;
    private Double sharedPoolBytes;
    private Double sharedPoolFreePct;

    // --- PGA/Workarea ---
    private Double memorySortPct;
    private Double dedicatedSessCnt;
    private Double parallelProcCnt;
    private Double sharedServerProcCnt;
    private Double dispatcherProcCnt;
    private Double jobProcCnt;
    private Double pgaUsedBytes;
    private Double pgaTargetBytes;
    private Double pgaUtilPct;
    private Double workareaSpillExec;
    private Double workareaTotalExec;
    private Double workareaSpillRatePct;
    private Double spillMbPerMin;
    private Double hardParsesPerSec;
    private Double libcacheReloadPerSec;
    private Double bufferMissPct;

    // --- Session 현재 상태/한계 ---
    private Double activeUserSessionsNow;
    private Double inactiveUserSessionsNow;
    private Double totalUserSessionsNow;
    private Double activeUserRatioPct;
    private Double sessionsUsedCurrent;
    private Double sessionsLimit;
    private Double sessionsLimitUtilPct;
    private Double processesCurrent;
    private Double processesLimit;
    private Double processesLimitUtilPct;
    private Double lockWaitTx;
    private Double lockWaitTm;
    private Double lockWaitTotal;
    private Double blockersNow;
    private Double blockedNow;
    private Double aasWaitSessions;

    // --- 로그온/디스커넥트 ---
    private Double logonsPerSec;
    private Double disconnectsPerSec;

    // --- Top SQL by CPU / Shared Pool ---
    private String topSqlByCpuSqlId01, topSqlByCpuSqlId02, topSqlByCpuSqlId03, topSqlByCpuSqlId04, topSqlByCpuSqlId05;
    private Double topSqlByCpuValue01, topSqlByCpuValue02, topSqlByCpuValue03, topSqlByCpuValue04, topSqlByCpuValue05;

    private String topSqlBySharedPoolSqlId01, topSqlBySharedPoolSqlId02, topSqlBySharedPoolSqlId03, topSqlBySharedPoolSqlId04, topSqlBySharedPoolSqlId05;
    private Double topSqlBySharedPoolValue01, topSqlBySharedPoolValue02, topSqlBySharedPoolValue03, topSqlBySharedPoolValue04, topSqlBySharedPoolValue05;

    // --- Top Blocker Sessions ---
    private String topBlockerSessionSid01, topBlockerSessionSid02, topBlockerSessionSid03, topBlockerSessionSid04, topBlockerSessionSid05;
    private Double topBlockerSessionVictims01, topBlockerSessionVictims02, topBlockerSessionVictims03, topBlockerSessionVictims04, topBlockerSessionVictims05;

    // --- I/O/Storage 대시보드(요약 값들) ---
    private Double cacheHitRatioPct;
    private Double avgIoWaitTimeMs;
    private Double physicalReadsPerSec;
    private Double redoSizeMbPerSec;
    private Double parseExecuteRatio;
    private Double directPathIoPerSec;

    private Double physicalReadsDirectPerSec;
    private Double physicalWritesDirectPerSec;
    private Double directIoRatioPct;

    private Double parserRequestPerSec;
    private Double sqlExecutePerSec;
    private Double sqlParseExecuteRatio;

    private Double physicalReadsPerDiffSec;
    private Double logicalReadsPerSec;
    private Double cacheHitRatioDiffPct;
    private Double totalReadsPerSec;

    private Double avgWaitTimeMs;
    private Double p95WaitTimeMs;
    private Double ioWaitsPerSec;
    private Double ioTimePerSecMs;

    private Double redoGenerationMbps;
    private Double redoGenerationMbpsTotal;
    private Double redoGeneration24hAvg;
    private Double logSwitchCount1min;
    private Double logSwitchCount5min;

    private Double dbwrWriteCountPerMin;
    private Double dbwrWriteVolumeMbPerMin;
    private Double dbwrWriteVolumeMbPerMinTotal;
    private Double checkpointNotCompleteCount;

    // --- 데이터파일 Top5 (그래프 44) ---
    private String dataFileName01, dataFileName02, dataFileName03, dataFileName04, dataFileName05;
    private String dataTablespaceName01, dataTablespaceName02, dataTablespaceName03, dataTablespaceName04, dataTablespaceName05;
    private Double dataIoSharePct01, dataIoSharePct02, dataIoSharePct03, dataIoSharePct04, dataIoSharePct05;

    // --- Storage 대시보드/트렌드 ---
    private Double fraUsagePercent;
    private Double fraFreeGb;
    private Double undoUsagePct;
    private Double tempUsagePct;
    private String  maxTsName;
    private Double maxTsUsagePct;
    private Double totalDbUsagePct;

    private Double tempActiveUsageGb;
    private Double tempCurrentSizeGb;
    private Double tempMaxSizeGb;
    private Double tempUsagePercent;
    private Double tempUsagePctOfMax;
    private Double tempPeakUsage24hGb;

    private String systemTablespaceName, sysauxTablespaceName, undotbs1TablespaceName, usersTablespaceName;
    private Double systemUsedPercent, sysauxUsedPercent, undotbs1UsedPercent, usersUsedPercent;

    private String systemTablespaceNameInc, sysauxTablespaceNameInc, undotbs1TablespaceNameInc, usersTablespaceNameInc;
    private Double systemUsedSpaceGbInc, sysauxUsedSpaceGbInc, undotbs1UsedSpaceGbInc, usersUsedSpaceGbInc;

    private Double spaceLimitGb, spaceUsedGb, spaceReclaimableGb, usagePct, hourlyGrowthPct, timeTo95PctHours;

    private Double totalDbUsagePercent;

    // --- 대용량 세그먼트 Top5 (그래프 52) ---
    private String ownerSeg01, ownerSeg02, ownerSeg03, ownerSeg04, ownerSeg05;
    private String tablespaceNameSeg01, tablespaceNameSeg02, tablespaceNameSeg03, tablespaceNameSeg04, tablespaceNameSeg05;
    private Double sizeGbSeg01, sizeGbSeg02, sizeGbSeg03, sizeGbSeg04, sizeGbSeg05;
    private String compressionSeg01, compressionSeg02, compressionSeg03, compressionSeg04, compressionSeg05;
}
