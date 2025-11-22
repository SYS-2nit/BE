package com.sys.dbmonitor.domains.sql.service.query;

import com.sys.dbmonitor.domains.sql.domain.MetricType;
import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.request.SqlDailyGraphRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlGraphRequest;
import com.sys.dbmonitor.domains.sql.dto.response.SqlDailyGraphResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlGraphSeriesResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlPeriodGraphResponse;
import com.sys.dbmonitor.domains.sql.repository.SqlRepository;
import com.sys.dbmonitor.domains.sql.util.SqlDateUtils;
import com.sys.dbmonitor.domains.sql.util.TimeBucketAggregator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL 그래프 데이터 조회 서비스
 */
@Service
@RequiredArgsConstructor
public class SqlGraphService {

    private final SqlRepository sqlRepository;

    /**
     * SQL 그래프 데이터 조회 (기간별)
     */
    @Transactional(readOnly = true)
    public SqlGraphSeriesResponse getSqlGraphData(SqlGraphRequest request) {
        LocalDateTime startAt = SqlDateUtils.parseStartDate(request.startDate());
        LocalDateTime endAt = SqlDateUtils.parseEndDate(request.endDate());
        int interval = SqlDateUtils.getDefaultInterval(request.intervalMinutes());

        // 원본 DB 데이터 조회
        List<Sql> list = sqlRepository.findForGraph(
                request.instanceId(),
                request.filter(),
                startAt,
                endAt
        );

        // 메트릭 타입 설정
        MetricType metricType = MetricType.from(request.metric());

        // 버킷 집계
        Map<Long, Long> bucketMap = TimeBucketAggregator.aggregateByTimeBucket(
                list,
                startAt,
                endAt,
                interval,
                Sql::getCreatedAt,
                metricType::extract
        );

        // 응답 변환
        List<SqlGraphSeriesResponse.Bucket> buckets = TimeBucketAggregator.convertToGraphBuckets(
                bucketMap,
                startAt,
                interval
        );

        return new SqlGraphSeriesResponse(
                request.metric(),
                request.startDate(),
                request.endDate(),
                buckets
        );
    }

    /**
     * 일별 SQL 그래프 조회
     */
    @Transactional(readOnly = true)
    public List<SqlDailyGraphResponse> getDailySqlGraph(
            String date,
            String metric,
            Long instanceId,
            Integer intervalMinutes
    ) {
        LocalDateTime start = SqlDateUtils.parseStartDate(date);
        LocalDateTime end = start.plusDays(1);
        int interval = SqlDateUtils.getDefaultInterval(intervalMinutes);

        List<Sql> raw = sqlRepository.findForGraph(
                instanceId,
                null,
                start,
                end
        );

        // 메트릭 타입 설정
        MetricType metricType = MetricType.from(metric);

        // 시간대별 버킷 집계
        Map<Integer, Long> bucketMap = TimeBucketAggregator.aggregateByHourBucket(
                raw,
                start,
                interval,
                Sql::getCreatedAt,
                metricType::extract
        );

        // 응답 변환
        return TimeBucketAggregator.convertToDailyGraphBuckets(
                bucketMap,
                start,
                interval
        );
    }

    /**
     * 일별 SQL 그래프 조회 (오버로드)
     */
    public List<SqlDailyGraphResponse> getDailySqlGraph(SqlDailyGraphRequest req) {
        return getDailySqlGraph(
                req.date(),
                req.metric(),
                req.instanceId(),
                req.intervalMinutes()
        );
    }

    /**
     * 기간별 그래프 조회
     */
    @Transactional(readOnly = true)
    public List<SqlPeriodGraphResponse> getPeriodGraph(
            String startDate,
            String endDate,
            String metric,
            Integer intervalMinutes,
            Long instanceId
    ) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        int interval = SqlDateUtils.getDefaultInterval(intervalMinutes);

        List<SqlPeriodGraphResponse> result = new ArrayList<>();

        // 하루씩 반복
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            SqlDailyGraphRequest req = new SqlDailyGraphRequest(
                    day.toString(),
                    metric,
                    instanceId,
                    interval
            );

            List<SqlDailyGraphResponse> dailyGraph = getDailySqlGraph(req);

            for (SqlDailyGraphResponse d : dailyGraph) {
                String full = day + " " + d.time();
                result.add(new SqlPeriodGraphResponse(full, d.value()));
            }
        }

        return result;
    }
}

