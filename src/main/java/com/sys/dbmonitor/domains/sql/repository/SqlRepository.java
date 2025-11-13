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
public interface SqlRepository extends JpaRepository<Sql, Long> {

    /** soft delete 되지 않은 데이터 전체 조회용 */
    List<Sql> findByIsDeletedFalse();

    /** 통계 필터 조회용 */
    @Query("""
        SELECT s 
        FROM Sql s
        WHERE s.isDeleted = false
          AND (:instanceId IS NULL OR s.instanceId = :instanceId)
          AND (:keyword IS NULL OR LOWER(s.sqlText) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:start IS NULL OR s.createdAt >= :start)
          AND (:end IS NULL OR s.createdAt < :end)
        """)
    Page<Sql> findFilteredSqlStats(Long instanceId,
                                   String keyword,
                                   LocalDateTime start,
                                   LocalDateTime end,
                                   Pageable pageable);
    
    /* 통계 그래프 조회용 */
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

}
