package com.sys.dbmonitor.domains.notification.repository;

import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.AlertMetricTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertMetricTemplateRepository extends JpaRepository<AlertMetricTemplate, Long> {

    /**
     * 특정 카테고리의 활성화된 메트릭 템플릿 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT amt FROM AlertMetricTemplate amt WHERE amt.category = :category AND amt.isActive = true AND amt.isDeleted = false ORDER BY amt.metricName ASC")
    List<AlertMetricTemplate> findByCategoryAndActive(@Param("category") AlertCategory category);

    /**
     * 모든 활성화된 메트릭 템플릿 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT amt FROM AlertMetricTemplate amt WHERE amt.isActive = true AND amt.isDeleted = false ORDER BY amt.category ASC, amt.metricName ASC")
    List<AlertMetricTemplate> findAllActive();

    /**
     * 특정 그래프 ID의 메트릭 템플릿 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT amt FROM AlertMetricTemplate amt WHERE amt.graph.id = :graphId AND amt.isDeleted = false")
    List<AlertMetricTemplate> findByGraphId(@Param("graphId") Long graphId);

    /**
     * 특정 메트릭 키의 메트릭 템플릿 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT amt FROM AlertMetricTemplate amt WHERE amt.metricKey = :metricKey AND amt.isDeleted = false")
    Optional<AlertMetricTemplate> findByMetricKey(@Param("metricKey") String metricKey);

    /**
     * ID로 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT amt FROM AlertMetricTemplate amt WHERE amt.id = :id AND amt.isDeleted = false")
    Optional<AlertMetricTemplate> findByIdAndNotDeleted(@Param("id") Long id);
}

