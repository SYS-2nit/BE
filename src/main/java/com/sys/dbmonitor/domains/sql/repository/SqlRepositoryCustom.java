package com.sys.dbmonitor.domains.sql.repository;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface SqlRepositoryCustom {

    Page<Sql> findFilteredSqlStats(
            Long instanceId,
            String keyword,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );
}
