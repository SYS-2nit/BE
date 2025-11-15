package com.sys.dbmonitor.domains.sql.domain;

import com.sys.dbmonitor.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "SQL_DATA")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Sql extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sqlDataSeq")
    @SequenceGenerator(
            name = "sqlDataSeq",
            sequenceName = "SEQ_SQL_DATA",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "INSTANCE_ID", nullable = false)
    private Long instanceId;

    @Column(name = "SQL_ID", length = 13)
    private String sqlId;

    @Column(name = "PLAN_HASH_VALUE")
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

    /** soft delete */
    public void markAsDeleted() {
        super.markAsDeleted();
    }

    /** SQL 데이터 갱신 */
    public void updateFrom(Sql newSql) {
        if (newSql.sqlText != null) this.sqlText = newSql.sqlText;
        if (newSql.cpuUsDelta != null) this.cpuUsDelta = newSql.cpuUsDelta;
        if (newSql.elapsedUsDelta != null) this.elapsedUsDelta = newSql.elapsedUsDelta;
        if (newSql.executionsDelta != null) this.executionsDelta = newSql.executionsDelta;
        setUpdatedAt(java.time.LocalDateTime.now());
    }

    /** 평균 컬럼 계산 */
    public Long getAvgElapsed() {
        if (executionsDelta == null || executionsDelta == 0) {
            return 0L;
        }
        if (elapsedUsDelta == null) {
            return 0L;
        }
        return elapsedUsDelta / executionsDelta;
    }
}
