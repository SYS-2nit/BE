/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.repository;

import com.sys.dbmonitor.domains.notification.domain.ProgressHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressHistoryRepository extends JpaRepository<ProgressHistory, Long> {

    /**
     * 특정 이벤트의 처리 이력 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT ph FROM ProgressHistory ph WHERE ph.event.id = :eventId AND ph.isDeleted = false ORDER BY ph.createdAt DESC")
    List<ProgressHistory> findByEventId(@Param("eventId") Long eventId);

    /**
     * 특정 사용자가 작성한 처리 이력 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT ph FROM ProgressHistory ph WHERE ph.createdBy.id = :createdBy AND ph.isDeleted = false ORDER BY ph.createdAt DESC")
    List<ProgressHistory> findByCreatedBy(@Param("createdBy") Long createdBy);

    /**
     * ID로 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT ph FROM ProgressHistory ph WHERE ph.id = :id AND ph.isDeleted = false")
    Optional<ProgressHistory> findByIdAndNotDeleted(@Param("id") Long id);
}

