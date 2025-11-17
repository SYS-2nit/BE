package com.sys.dbmonitor.domains.sql.domain;

import com.sys.dbmonitor.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SQL 스냅샷 엔티티
 * 30분마다 수집된 SQL의 델타 값을 저장
 */
@Entity
@Table(name = "SQL_DATA")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SqlSnapshot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "INSTANCE_ID", nullable = false)
    private Long instanceId;

    @Column(name = "SQL_ID", nullable = false, length = 13)
    private String sqlId;

    @Column(name = "PLAN_HASH_VALUE", nullable = false)
    private Long planHashValue;

    @Column(name = "BUFFER_GETS_DELTA")
    private Long bufferGetsDelta;

    @Column(name = "CPU_US_DELTA")
    private Long cpuUsDelta;

    @Column(name = "DISK_READS_DELTA")
    private Long diskReadsDelta;

    @Column(name = "ELAPSED_US_DELTA")
    private Long elapsedUsDelta;

    @Column(name = "EXECUTIONS_DELTA")
    private Long executionsDelta;

    @Column(name = "WAIT_TIME_US_DELTA")
    private Long waitTimeUsDelta;

    @Column(name = "WAIT_USER_IO_US_DELTA")
    private Long waitUserIoUsDelta;

    @Column(name = "WAIT_CONCURRENCY_US_DELTA")
    private Long waitConcurrencyUsDelta;

    @Column(name = "WAIT_APPLICATION_US_DELTA")
    private Long waitApplicationUsDelta;

    @Column(name = "WAIT_CLUSTER_US_DELTA")
    private Long waitClusterUsDelta;

    @Column(name = "WAIT_PLSQL_US_DELTA")
    private Long waitPlsqlUsDelta;

    @Column(name = "WAIT_JAVA_US_DELTA")
    private Long waitJavaUsDelta;

    @Column(name = "SQL_TEXT", length = 4000)
    private String sqlText;

    @Lob
    @Column(name = "PLAN_TEXT_CLOB", columnDefinition = "CLOB")
    private String planTextClob;

    @Builder
    public SqlSnapshot(Long instanceId,
                       LocalDateTime createdAt,
                       String sqlId,
                       Long planHashValue,
                       Long bufferGetsDelta,
                       Long cpuUsDelta,
                       Long diskReadsDelta,
                       Long elapsedUsDelta,
                       Long executionsDelta,
                       Long waitTimeUsDelta,
                       Long waitUserIoUsDelta,
                       Long waitConcurrencyUsDelta,
                       Long waitApplicationUsDelta,
                       Long waitClusterUsDelta,
                       Long waitPlsqlUsDelta,
                       Long waitJavaUsDelta,
                       String sqlText,
                       String planTextClob) {
        this.instanceId = instanceId;
        this.sqlId = sqlId;
        this.planHashValue = planHashValue;
        this.bufferGetsDelta = bufferGetsDelta;
        this.cpuUsDelta = cpuUsDelta;
        this.diskReadsDelta = diskReadsDelta;
        this.elapsedUsDelta = elapsedUsDelta;
        this.executionsDelta = executionsDelta;
        this.waitTimeUsDelta = waitTimeUsDelta;
        this.waitUserIoUsDelta = waitUserIoUsDelta;
        this.waitConcurrencyUsDelta = waitConcurrencyUsDelta;
        this.waitApplicationUsDelta = waitApplicationUsDelta;
        this.waitClusterUsDelta = waitClusterUsDelta;
        this.waitPlsqlUsDelta = waitPlsqlUsDelta;
        this.waitJavaUsDelta = waitJavaUsDelta;
        this.sqlText = sqlText;
        this.planTextClob = planTextClob;
    }
}

