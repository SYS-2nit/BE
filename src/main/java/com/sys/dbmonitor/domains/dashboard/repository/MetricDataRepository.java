/*
 ******************************************************************
 작성자: 최영준, 배지원
 ******************************************************************
 */
package com.sys.dbmonitor.domains.dashboard.repository;

import com.sys.dbmonitor.domains.dashboard.domain.MetricData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetricDataRepository extends JpaRepository<MetricData, Long>, MetricDataRepositoryCustom {
    /**
     * 인스턴스 ID, 그래프 ID, 시간 단위로 데이터 조회
     */
    List<MetricData> findByInstanceIdAndGraphIdAndIntervalTypeOrderByCollectedAtDesc(
            Long instanceId, Long graphId, String intervalType
    );
}

