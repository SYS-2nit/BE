package com.sys.dbmonitor.domains.sql.domain;

import com.sys.dbmonitor.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "SQL_DATA")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Sql extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instance_id", nullable = false)
    private Long instanceId;

    @Column(name = "sql_id")
    private Long sqlId;

    @Column(name = "plan_hash_value")
    private Long planHashValue;

    @Column(name = "buffer_gets_delta")
    private Long bufferGetsDelta;

    @Column(name = "cpu_us_delta")
    private Long cpuUsDelta;

    @Column(name = "disk_reads_delta")
    private Long diskReadsDelta;

    @Column(name = "elapsed_us_delta")
    private Long elapsedUsDelta;

    @Column(name = "executions_delta")
    private Long executionsDelta;

    @Column(name = "wait_time_us_delta")
    private Long waitTimeUsDelta;

    @Column(name = "wait_user_io_us_delta")
    private Long waitUserIoUsDelta;

    @Column(name = "wait_concurrency_us_delta")
    private Long waitConcurrencyUsDelta;

    @Column(name = "wait_application_us_delta")
    private Long waitApplicationUsDelta;

    @Column(name = "wait_cluster_us_delta")
    private Long waitClusterUsDelta;

    @Column(name = "wait_plsql_us_delta")
    private Long waitPlsqlUsDelta;

    @Column(name = "wait_java_us_delta")
    private Long waitJavaUsDelta;

    @Column(name = "sql_text", length = 4000)
    private String sqlText;

    @Builder
    public Sql(Long instanceId,
               Long sqlId,
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
               String sqlText) {
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
    }

    /** SQL 텍스트 수정 */
    public void updateSqlText(String sqlText) {
        if (sqlText != null) this.sqlText = sqlText;
        touchUpdatedAt();
    }

    /** 소프트 삭제 */
    public void delete() {
        super.markAsDeleted();
        touchUpdatedAt();
    }

    /** 수동 수정 시각 갱신 */
    public void touchUpdatedAt() {
        super.setUpdatedAt(LocalDateTime.now());
    }
}
