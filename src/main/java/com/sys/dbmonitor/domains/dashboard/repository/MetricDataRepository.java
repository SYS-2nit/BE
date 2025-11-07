package com.sys.dbmonitor.domains.dashboard.repository;

import com.sys.dbmonitor.domains.dashboard.domain.MetricData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetricDataRepository extends JpaRepository<MetricData, Long> {
}



