/**************************************************
 작성자 : 오수경
 *************************************************/

package com.sys.dbmonitor.domains.sql.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sys.dbmonitor.domains.sql.domain.QSql;
import com.sys.dbmonitor.domains.sql.domain.Sql;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class SqlRepositoryCustomImpl implements SqlRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public List<Sql> findForGraph(
            Long instanceId,
            String filter,
            LocalDateTime start,
            LocalDateTime end
    ) {

        QSql sql = QSql.sql;

        return queryFactory
                .selectFrom(sql)
                .where(
                        sql.isDeleted.eq(false),
                        instanceId != null ? sql.instanceId.eq(instanceId) : null,
                        filter != null && !filter.isEmpty() ? sql.sqlText.containsIgnoreCase(filter) : null,
                        sql.createdAt.goe(start),
                        sql.createdAt.lt(end)
                )
                .fetch();
    }

    @Override
    public List<Object[]> findAggregatedStats(
            Long instanceId,
            LocalDateTime start,
            LocalDateTime end,
            String orderBy,
            String direction
    ) {
        // ORDER BY 컬럼명 검증 및 매핑 (SQL Injection 방지)
        // MetricType enum을 사용하여 타입 안전성 보장
        com.sys.dbmonitor.domains.sql.domain.MetricType metricType = 
                com.sys.dbmonitor.domains.sql.domain.MetricType.from(orderBy);
        String orderByColumn = metricType.getOrderByColumn();

        // 정렬 방향 검증
        String sortDirection = "DESC".equalsIgnoreCase(direction) ? "DESC" : "ASC";

        // 네이티브 쿼리: DB 레벨에서 GROUP BY, ORDER BY 처리
        // Object[]: [id, instanceId, sqlId, sqlText, elapsedSum, execSum, waitSum, bufferSum, diskSum, cpuSum, avgElapsed]
        // Oracle에서 IS_DELETED는 NUMBER(1)이므로 0을 사용, SQL_TEXT NULL 처리
        // avg_elapsed는 ORDER BY에서 사용하기 위해 SELECT 절에 계산식 추가
        String sql = """
            SELECT 
                MIN(s.ID) AS id,
                MIN(s.INSTANCE_ID) AS instance_id,
                MIN(s.SQL_ID) AS sql_id,
                s.SQL_TEXT AS sql_text,
                COALESCE(SUM(s.ELAPSED_US_DELTA), 0) AS elapsed_sum,
                COALESCE(SUM(s.EXECUTIONS_DELTA), 0) AS exec_sum,
                COALESCE(SUM(s.WAIT_TIME_US_DELTA), 0) AS wait_sum,
                COALESCE(SUM(s.BUFFER_GETS_DELTA), 0) AS buffer_sum,
                COALESCE(SUM(s.DISK_READS_DELTA), 0) AS disk_sum,
                COALESCE(SUM(s.CPU_US_DELTA), 0) AS cpu_sum,
                CASE 
                    WHEN COALESCE(SUM(s.EXECUTIONS_DELTA), 0) = 0 THEN 0
                    ELSE COALESCE(SUM(s.ELAPSED_US_DELTA), 0) / COALESCE(SUM(s.EXECUTIONS_DELTA), 1)
                END AS avg_elapsed
            FROM SQL_DATA s
            WHERE s.IS_DELETED = 0
                AND (:instanceId IS NULL OR s.INSTANCE_ID = :instanceId)
                AND s.CREATED_AT >= :start
                AND s.CREATED_AT < :end
                AND s.SQL_TEXT IS NOT NULL
            GROUP BY s.SQL_TEXT
            ORDER BY """ + " " + orderByColumn + " " + sortDirection;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("instanceId", instanceId);
        query.setParameter("start", start);
        query.setParameter("end", end);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        return results;
    }
}
