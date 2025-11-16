package com.sys.dbmonitor.domains.sql.repository;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 인터페이스
@Repository
public interface SqlRepository extends JpaRepository<Sql, Long>, SqlRepositoryCustom {

    /** 데이터 전체 조회용 */
    List<Sql> findByIsDeletedFalse();

    /** ===== 통계 상세 탭 조회용 ===== */
    @Query("""
        SELECT s
        FROM Sql s
        WHERE s.isDeleted = false
          AND s.sqlId = :sqlId
          AND s.createdAt >= :start
          AND s.createdAt < :end
        """)
    List<Sql> findBySqlIdAndDateRange(
            String sqlId,
            LocalDateTime start,
            LocalDateTime end
    );

    /** 전체 elapsed 합계 (비중 계산용) */
    @Query("""
        SELECT COALESCE(SUM(s.elapsedUsDelta), 0)
        FROM Sql s
        WHERE s.isDeleted = false
          AND s.createdAt >= :start
          AND s.createdAt < :end
        """)
    Long sumElapsedForRange(
            LocalDateTime start,
            LocalDateTime end
    );

    /** 특정 SQL의 랭킹 조회: elapsed 기준으로 몇 등인지 */
    @Query("""
        SELECT COUNT(s) + 1
        FROM Sql s
        WHERE s.isDeleted = false
          AND s.createdAt >= :start
          AND s.createdAt < :end
          AND s.elapsedUsDelta > (
                SELECT COALESCE(SUM(x.elapsedUsDelta), 0)
                FROM Sql x
                WHERE x.sqlId = :sqlId
                  AND x.createdAt >= :start
                  AND x.createdAt < :end
          )
        """)
    Integer findRankByElapsed(
            String sqlId,
            LocalDateTime start,
            LocalDateTime end
    );

    /** Plan History 조회용: 특정 SQL_ID의 전체 이력 ASC */
    List<Sql> findBySqlIdOrderByCreatedAtAsc(String sqlId);

    /** Plan Change Detail 조회용: 특정 sqlId + planHash 에서 가장 최신 row */
    Optional<Sql> findTopBySqlIdAndPlanHashValueOrderByCreatedAtDesc(
            String sqlId,
            Long planHashValue
    );
}
