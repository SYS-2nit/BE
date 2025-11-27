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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**************************************************
 작성자 : 오수경
 *************************************************/

/**
 * SQL 그래프 데이터 조회 서비스
 */
@Slf4j
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
        long queryStartTime = System.currentTimeMillis();
        LocalDateTime start = SqlDateUtils.parseStartDate(date);
        LocalDateTime end = start.plusDays(1);
        int interval = SqlDateUtils.getDefaultInterval(intervalMinutes);

        List<Sql> raw = sqlRepository.findForGraph(
                instanceId,
                null,
                start,
                end
        );
        long queryEndTime = System.currentTimeMillis();
        long queryDuration = queryEndTime - queryStartTime;

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
        List<SqlDailyGraphResponse> result = TimeBucketAggregator.convertToDailyGraphBuckets(
                bucketMap,
                start,
                interval
        );

        log.debug("[쿼리 카운트] getDailySqlGraph - 날짜: {}, 쿼리시간: {}ms, 데이터수: {}", 
                date, queryDuration, raw.size());
        
        return result;
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
     * 최적화: 한 번의 쿼리로 전체 기간 데이터를 조회한 후, Java에서 일별로 그룹핑 처리
     */
    @Transactional(readOnly = true)
    public List<SqlPeriodGraphResponse> getPeriodGraph(
            String startDate,
            String endDate,
            String metric,
            Integer intervalMinutes,
            Long instanceId
    ) {
        long startTime = System.currentTimeMillis();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        long periodDays = ChronoUnit.DAYS.between(start, end) + 1;
        int interval = SqlDateUtils.getDefaultInterval(intervalMinutes);

        // 한 번의 쿼리로 전체 기간 데이터 조회
        LocalDateTime startAt = SqlDateUtils.parseStartDate(startDate);
        LocalDateTime endAt = SqlDateUtils.parseEndDate(endDate);
        
        List<Sql> allData = sqlRepository.findForGraph(
                instanceId,
                null,
                startAt,
                endAt
        );

        // 메트릭 타입 설정
        MetricType metricType = MetricType.from(metric);

        // 일별로 그룹핑
        Map<LocalDate, List<Sql>> dailyGrouped = allData.stream()
                .collect(Collectors.groupingBy(sql -> sql.getCreatedAt().toLocalDate()));

        List<SqlPeriodGraphResponse> result = new ArrayList<>();

        // 일별 데이터 처리
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            List<Sql> dayData = dailyGrouped.getOrDefault(day, new ArrayList<>());
            
            if (dayData.isEmpty()) {
                // 데이터가 없는 날도 빈 버킷으로 채움
                int buckets = (24 * 60) / interval;
                for (int i = 0; i < buckets; i++) {
                    LocalDateTime bucketTime = day.atStartOfDay().plusMinutes((long) i * interval);
                    String timeStr = bucketTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                    String full = day + " " + timeStr;
                    result.add(new SqlPeriodGraphResponse(full, 0L));
                }
            } else {
                // 해당 일의 데이터를 시간대별 버킷으로 집계
                LocalDateTime dayStart = day.atStartOfDay();
                Map<Integer, Long> bucketMap = TimeBucketAggregator.aggregateByHourBucket(
                        dayData,
                        dayStart,
                        interval,
                        Sql::getCreatedAt,
                        metricType::extract
                );

                // 버킷을 응답 형식으로 변환
                List<SqlDailyGraphResponse> dailyGraph = TimeBucketAggregator.convertToDailyGraphBuckets(
                        bucketMap,
                        dayStart,
                        interval
                );

                for (SqlDailyGraphResponse d : dailyGraph) {
                    String full = day + " " + d.time();
                    result.add(new SqlPeriodGraphResponse(full, d.value()));
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 성능 측정 로그 출력
        log.info("========================================");
        log.info("[성능 측정] getPeriodGraph");
        log.info("  실행시간: {}ms ({}초)", duration, duration / 1000.0);
        log.info("  기간: {}일", periodDays);
        log.info("  쿼리수: 1개");
        log.info("  데이터수: {}개", allData.size());
        log.info("  시작일: {}, 종료일: {}", startDate, endDate);
        log.info("========================================");

        return result;
    }
}

