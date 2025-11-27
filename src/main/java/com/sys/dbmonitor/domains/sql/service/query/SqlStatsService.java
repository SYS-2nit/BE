/**************************************************
 작성자 : 오수경
 *************************************************/

package com.sys.dbmonitor.domains.sql.service.query;

import com.sys.dbmonitor.domains.sql.domain.MetricType;
import com.sys.dbmonitor.domains.sql.dto.AggregatedSqlStats;
import com.sys.dbmonitor.domains.sql.dto.request.SqlStatsQueryRequest;
import com.sys.dbmonitor.domains.sql.dto.response.SqlResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlStatsPageResponse;
import com.sys.dbmonitor.domains.sql.repository.SqlRepository;
import com.sys.dbmonitor.domains.sql.util.SqlDateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SQL 통계 목록 조회 서비스
 */
@Service
@RequiredArgsConstructor
public class SqlStatsService {

    private final SqlRepository sqlRepository;

    /**
     * SQL 통계 목록 조회
     */
    @Transactional(readOnly = true)
    public SqlStatsPageResponse getSqlStats(SqlStatsQueryRequest request) {
        LocalDateTime start = SqlDateUtils.parseStartDate(request.startDate());
        LocalDateTime end = SqlDateUtils.parseEndDate(request.endDate());

        // 정렬 기준 및 방향 설정
        MetricType metricType = MetricType.from(request.orderBy());
        String direction = request.direction() != null ? request.direction() : "DESC";

        // DB 레벨에서 GROUP BY, ORDER BY 처리하여 필요한 데이터만 조회
        List<Object[]> aggregatedResults = sqlRepository.findAggregatedStats(
                request.instanceId(),
                start,
                end,
                metricType.getName(),
                direction
        );

        // Object[]를 DTO로 변환 후 SqlResponse로 변환
        List<SqlResponse> groupedList = aggregatedResults.stream()
                .map(AggregatedSqlStats::from)
                .map(AggregatedSqlStats::toSqlResponse)
                .collect(Collectors.toList());

        // 전체 데이터 반환 (DB에서 이미 정렬되어 있음)
        int total = groupedList.size();
        
        // Page 객체 생성
        Pageable pageable = PageRequest.of(0, total > 0 ? total : 1);
        Page<SqlResponse> pageResult = new PageImpl<>(
                groupedList,
                pageable,
                total
        );

        return SqlStatsPageResponse.from(pageResult);
    }
}

