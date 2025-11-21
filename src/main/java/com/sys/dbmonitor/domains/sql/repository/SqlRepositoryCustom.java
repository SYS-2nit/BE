package com.sys.dbmonitor.domains.sql.repository;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import java.time.LocalDateTime;
import java.util.List;

// QueryDSL 메서드 시그니처 정의
public interface SqlRepositoryCustom {
    // 그래프 조회용 QueryDSL 메서드
    List<Sql> findForGraph(
            Long instanceId,
            String filter,
            LocalDateTime start,
            LocalDateTime end
    );

    // 통계 조회용 (페이지네이션 없이 전체 조회)
    List<Sql> findAllForStats(
            Long instanceId,
            LocalDateTime start,
            LocalDateTime end
    );
}
