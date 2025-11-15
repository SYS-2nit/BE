package com.sys.dbmonitor.domains.sql.repository;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SqlRepository extends JpaRepository<Sql, Long>, SqlRepositoryCustom {

    /** soft delete 되지 않은 데이터 전체 조회용 */
    List<Sql> findByIsDeletedFalse();

    /** ===== 통계 그래프 조회용 ===== */
    @Query("""
    SELECT s 
    FROM Sql s
    WHERE s.isDeleted = false
      AND (:instanceId IS NULL OR s.instanceId = :instanceId)
      AND (:keyword IS NULL OR LOWER(s.sqlText) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND (:start IS NULL OR s.createdAt >= :start)
      AND (:end IS NULL OR s.createdAt < :end)
    """)
        List<Sql> findForGraph(
                Long instanceId,
                String keyword,
                LocalDateTime start,
                LocalDateTime end
        );

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
}
