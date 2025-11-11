package com.sys.dbmonitor.domains.topSql.repository;

import com.sys.dbmonitor.domains.topSql.domain.SqlSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SQL 스냅샷 JPA Repository
 */
@Repository
public interface SqlSnapshotRepository extends JpaRepository<SqlSnapshot, Long> {

    /**
     * 특정 인스턴스의 SQL 스냅샷 조회
     */
    List<SqlSnapshot> findByInstanceId(Long instanceId);

    /**
     * 특정 기간의 SQL 스냅샷 조회
     */
    List<SqlSnapshot> findByInstanceIdAndCreatedAtBetween(Long instanceId, LocalDateTime start, LocalDateTime end);

    /**
     * 특정 SQL ID의 스냅샷 조회
     */
    List<SqlSnapshot> findByInstanceIdAndSqlIdAndPlanHashValue(Long instanceId, String sqlId, Long planHashValue);
}

