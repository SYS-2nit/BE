package com.sys.dbmonitor.domains.sql.repository;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

// QueryDSL 메서드 시그니처 정의
public interface SqlRepositoryCustom {

    // 필터 조회용 QueryDSL 메서드
    Page<Sql> findFilteredSqlStats(
            Long instanceId,
            String keyword,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    // 그래프 조회용 QueryDSL 메서드
    List<Sql> findForGraph(
            Long instanceId,
            String keyword,
            LocalDateTime start,
            LocalDateTime end
    );
}
