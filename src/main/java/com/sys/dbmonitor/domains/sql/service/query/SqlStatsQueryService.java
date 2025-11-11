package com.sys.dbmonitor.domains.sql.service.query;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.request.SqlStatsQueryRequest;
import com.sys.dbmonitor.domains.sql.dto.response.SqlResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlStatsPageResponse;
import com.sys.dbmonitor.domains.sql.repository.SqlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SqlStatsQueryService {

    private final SqlRepository sqlRepository;

    @Transactional(readOnly = true)
    public SqlStatsPageResponse getSqlStats(SqlStatsQueryRequest request) {
        LocalDateTime start = request.startDate() != null
                ? request.startDate().atStartOfDay()
                : LocalDateTime.now().minusDays(1);
        LocalDateTime end = request.endDate() != null
                ? request.endDate().atStartOfDay()
                : LocalDateTime.now();

        Sort sort = Sort.by(Sort.Direction.fromString(request.direction() != null ? request.direction() : "DESC"),
                switch (request.orderBy() == null ? "elapsed" : request.orderBy()) {
                    case "cpu" -> "cpuUsDelta";
                    case "exec" -> "executionsDelta";
                    default -> "elapsedUsDelta";
                });

        Pageable pageable = PageRequest.of(
                request.page() != null ? request.page() : 0,
                request.size() != null ? request.size() : 20,
                sort
        );

        Page<Sql> result = sqlRepository.findFilteredSqlStats(
                request.instanceId(),
                request.keyword(),
                start,
                end,
                request.minExecCount(),
                request.maxExecCount(),
                pageable
        );

        return SqlStatsPageResponse.from(result.map(SqlResponse::from));
    }
}
