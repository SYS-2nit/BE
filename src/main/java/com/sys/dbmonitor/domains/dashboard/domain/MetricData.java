package com.sys.dbmonitor.domains.dashboard.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Entity
@Table(name = "metric_data_test")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MetricData {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "metric_data_seq")
    @SequenceGenerator(name = "metric_data_seq", sequenceName = "SEQ_METRIC_DATA_ID", allocationSize = 1)
    @Column(name = "ID")
    private Long id;
    

    @Column(name = "INSTANCE_ID", nullable = false)
    private Long instanceId;

    @Column(name = "GRAPH_ID", nullable = false)
    private Long graphId;

    @Column(name = "CATEGORY_ID", nullable = false)
    private Long categoryId;

    @Column(name = "COLLECTED_AT", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "INTERVAL_TYPE", nullable = false, length = 10)
    private String intervalType;

    // CPU 관련
    @Column(name = "HOST_CPU_UTIL_PCT", columnDefinition = "NUMBER")
    private Double hostCpuUtilPct;

    @Column(name = "DB_OF_HOST_SHARE_PCT", columnDefinition = "NUMBER")
    private Double dbOfHostSharePct;

    @Column(name = "AAS_TOTAL", columnDefinition = "NUMBER")
    private Double aasTotal;

    // SESSION 관련
    @Column(name = "SESSIONS_LIMIT_UTIL_PCT", columnDefinition = "NUMBER")
    private Double sessionsLimitUtilPct;

    @Column(name = "PROCESSES_USAGE_PCT", columnDefinition = "NUMBER")
    private Double processesUsagePct;

    @Column(name = "SESSIONS_USAGE_PCT", columnDefinition = "NUMBER")
    private Double sessionsUsagePct;

    @Column(name = "OPEN_CURSORS_MAX_SESSION_PCT", columnDefinition = "NUMBER")
    private Double openCursorsMaxSessionPct;

    // I/O 관련
    @Column(name = "SINGLE_BLOCK_READ_LATENCY_MS", columnDefinition = "NUMBER")
    private Double singleBlockReadLatencyMs;

    @Column(name = "DIRECT_PATH_READ_LATENCY_MS", columnDefinition = "NUMBER")
    private Double directPathReadLatencyMs;

    @Column(name = "DIRECT_PATH_WRITE_LATENCY_MS", columnDefinition = "NUMBER")
    private Double directPathWriteLatencyMs;

    @Column(name = "PHYSICAL_READ_MB_PER_SEC", columnDefinition = "NUMBER")
    private Double physicalReadMbPerSec;

    @Column(name = "PHYSICAL_WRITE_MB_PER_SEC", columnDefinition = "NUMBER")
    private Double physicalWriteMbPerSec;

    // Wait Class 관련
    @Column(name = "WAIT_CLASS_AAS_USER_IO", columnDefinition = "NUMBER")
    private Double waitClassAasUserIo;

    @Column(name = "WAIT_CLASS_AAS_COMMIT", columnDefinition = "NUMBER")
    private Double waitClassAasCommit;

    @Column(name = "WAIT_CLASS_AAS_CONCURRENCY", columnDefinition = "NUMBER")
    private Double waitClassAasConcurrency;

    @Column(name = "WAIT_CLASS_AAS_NETWORK", columnDefinition = "NUMBER")
    private Double waitClassAasNetwork;

    @Column(name = "WAIT_CLASS_AAS_OTHER", columnDefinition = "NUMBER")
    private Double waitClassAasOther;

    // MEMORY 관련
    @Column(name = "WORKAREA_SPILL_RATE_PCT", columnDefinition = "NUMBER")
    private Double workareaSpillRatePct;

    @Column(name = "LIBCACHE_RELOAD_PER_S", columnDefinition = "NUMBER")
    private Double libcacheReloadPerS;

    @Column(name = "HARD_PARSES_PER_SEC", columnDefinition = "NUMBER")
    private Double hardParsesPerSec;

    @Column(name = "SPILL_MB_PER_MIN", columnDefinition = "NUMBER")
    private Double spillMbPerMin;

    @Column(name = "SHARED_POOL_FREE_BYTES")
    private Long sharedPoolFreeBytes;

    // STORAGE 관련
    @Column(name = "FRA_USAGE_PCT", columnDefinition = "NUMBER")
    private Double fraUsagePct;

    @Column(name = "SYSTEM_TS_USAGE_PCT", columnDefinition = "NUMBER")
    private Double systemTsUsagePct;

    @Column(name = "SYSAUX_TS_USAGE_PCT", columnDefinition = "NUMBER")
    private Double sysauxTsUsagePct;

    @Column(name = "USERS_TS_USAGE_PCT", columnDefinition = "NUMBER")
    private Double usersTsUsagePct;

    @Column(name = "UNDO_TS_USAGE_PCT", columnDefinition = "NUMBER")
    private Double undoTsUsagePct;

    @Column(name = "TEMP_TS_USAGE_PCT", columnDefinition = "NUMBER")
    private Double tempTsUsagePct;

    // Background Process 관련
    @Column(name = "LGWR_ACTIVE")
    private Integer lgwrActive;

    @Column(name = "DBWR_ACTIVE")
    private Integer dbwrActive;

    @Column(name = "PMON_ACTIVE")
    private Integer pmonActive;

    @Column(name = "SMON_ACTIVE")
    private Integer smonActive;

    @Column(name = "CKPT_ACTIVE")
    private Integer ckptActive;

    @Column(name = "ARCN_ACTIVE")
    private Integer arcnActive;
}

