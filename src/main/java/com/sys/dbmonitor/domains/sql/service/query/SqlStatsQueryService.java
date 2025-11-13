package com.sys.dbmonitor.domains.sql.service.query;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.request.SqlGraphRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlStatsQueryRequest;
import com.sys.dbmonitor.domains.sql.dto.response.SqlGraphSeriesResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlStatsPageResponse;
import com.sys.dbmonitor.domains.sql.repository.SqlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SqlStatsQueryService {

    private final SqlRepository sqlRepository;

    /* ============== SQL 통계 목록 조회 API ============== */
    @Transactional(readOnly = true)
    public SqlStatsPageResponse getSqlStats(SqlStatsQueryRequest request) {

        LocalDateTime start = request.startDate() != null
                ? request.startDate().atStartOfDay()
                : LocalDateTime.now().minusDays(1);

        LocalDateTime end = request.endDate() != null
                ? request.endDate().plusDays(1).atStartOfDay()
                : LocalDateTime.now();

        Sort sort = Sort.by(Sort.Direction.fromString(
                request.direction() != null ? request.direction() : "DESC"
        ), switch (request.orderBy() == null ? "elapsed" : request.orderBy()) {
            case "cpu" -> "cpuUsDelta";
            case "exec" -> "executionsDelta";
            default -> "elapsedUsDelta";
        });

        Pageable pageable = PageRequest.of(
                request.page() != null ? request.page() : 0,
                request.size() != null ? request.size() : 20,
                sort
        );

        // 기존 DB 조회
        Page<Sql> result = sqlRepository.findFilteredSqlStats(
                request.instanceId(),
                request.keyword(),
                start,
                end,
                pageable
        );

        //  SQL TEXT 기준 그룹핑
        Map<String, List<Sql>> grouped = result.getContent()
                .stream()
                .collect(Collectors.groupingBy(Sql::getSqlText));

        // 그룹별 합산 / 평균 계산
        List<SqlResponse> groupedList = grouped.entrySet()
                .stream()
                .map(entry -> {
                    List<Sql> list = entry.getValue();

                    long elapsedSum = list.stream().mapToLong(s -> nvl(s.getElapsedUsDelta())).sum();
                    long execSum = list.stream().mapToLong(s -> nvl(s.getExecutionsDelta())).sum();

                    long avgElapsed = execSum == 0 ? 0 : elapsedSum / execSum;

                    long waitSum = list.stream().mapToLong(s -> nvl(s.getWaitTimeUsDelta())).sum();
                    long bufferSum = list.stream().mapToLong(s -> nvl(s.getBufferGetsDelta())).sum();
                    long diskSum = list.stream().mapToLong(s -> nvl(s.getDiskReadsDelta())).sum();
                    long cpuSum = list.stream().mapToLong(s -> nvl(s.getCpuUsDelta())).sum();

                    // 대표 필드 선택
                    Sql base = list.get(0);

                    return new SqlResponse(
                            base.getId(),
                            base.getInstanceId(),
                            base.getSqlId(),
                            base.getSqlText(),
                            elapsedSum,
                            avgElapsed,
                            waitSum,
                            execSum,
                            bufferSum,
                            diskSum,
                            cpuSum
                    );
                })
                .toList();

        // Page 로 다시 변환해서 반환
        Page<SqlResponse> page = new PageImpl<>(
                groupedList,
                pageable,
                groupedList.size()
        );

        return SqlStatsPageResponse.from(page);
    }


    /* ============== SQL 그래프 데이터 조회 API ============== */
    @Transactional(readOnly = true)
    public SqlGraphSeriesResponse getSqlGraphData(SqlGraphRequest request) {

        LocalDate start = LocalDate.parse(request.startDate());
        LocalDate end = LocalDate.parse(request.endDate());

        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = end.plusDays(1).atStartOfDay();

        int interval = request.intervalMinutes() == null ? 30 : request.intervalMinutes();

        // 1) 원본 DB 데이터 조회
        List<Sql> list = sqlRepository.findForGraph(
                request.instanceId(),
                request.keyword(),
                startAt,
                endAt
        );

        // 2) interval 단위 bucket 초기 생성 (빈 버킷은 0으로 유지)
        // --------------------------------------------------------
        Map<Long, Long> bucketMap = new TreeMap<>();

        long totalMinutes = Duration.between(startAt, endAt).toMinutes();
        long bucketCount = (totalMinutes / interval) + 1;

        for (long i = 0; i < bucketCount; i++) {
            bucketMap.put(i, 0L); // 기본값 0 세팅
        }

        // 3) metric getter
        // --------------------------------------------------------
        Function<Sql, Long> metricGetter = switch (request.metric().toLowerCase()) {
            case "elapsed" -> s -> nvl(s.getElapsedUsDelta());
            case "avg" -> s -> nvl(s.getAvgElapsed());
            case "wait" -> s -> nvl(s.getWaitTimeUsDelta());
            case "execution" -> s -> nvl(s.getExecutionsDelta());
            case "buffer" -> s -> nvl(s.getBufferGetsDelta());
            case "disk" -> s -> nvl(s.getDiskReadsDelta());
            case "cpu" -> s -> nvl(s.getCpuUsDelta());
            default -> s -> 0L;
        };

        // 4) Row → bucket 매핑해서 누적 계산
        // --------------------------------------------------------
        for (Sql s : list) {

            long minutes = Duration.between(startAt, s.getCreatedAt()).toMinutes();

            if (minutes < 0) continue;

            long bucketIndex = minutes / interval;

            if (bucketMap.containsKey(bucketIndex)) {
                bucketMap.put(bucketIndex, bucketMap.get(bucketIndex) + metricGetter.apply(s));
            }
        }

        // 5) bucket → 응답 변환
        // --------------------------------------------------------
        List<SqlGraphSeriesResponse.Bucket> buckets = new ArrayList<>();

        for (Map.Entry<Long, Long> entry : bucketMap.entrySet()) {

            long bucketIndex = entry.getKey();
            long sumValue = entry.getValue();

            LocalDateTime labelTime = startAt.plusMinutes(bucketIndex * interval);

            // 날짜 포맷팅
            String label = labelTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            buckets.add(new SqlGraphSeriesResponse.Bucket(label, sumValue));
        }

        return new SqlGraphSeriesResponse(
                request.metric(),
                request.startDate(),
                request.endDate(),
                buckets
        );
    }

    /** 사용자 선택 필터(metric)에 따라 값을 뽑아서 반환 */
    private Function<Sql, Long> getMetricGetter(String metric) {

        return switch (metric.toLowerCase()) {

            case "elapsed" -> s -> nvl(s.getElapsedUsDelta());
            case "avg" -> s -> {
                long exec = nvl(s.getExecutionsDelta());
                if (exec == 0) return 0L;
                return nvl(s.getElapsedUsDelta()) / exec;
            };
            case "wait" -> s -> nvl(s.getWaitTimeUsDelta());
            case "execution" -> s -> nvl(s.getExecutionsDelta());
            case "buffer" -> s -> nvl(s.getBufferGetsDelta());
            case "disk" -> s -> nvl(s.getDiskReadsDelta());
            case "cpu" -> s -> nvl(s.getCpuUsDelta());

            default -> s -> 0L;
        };
    }

    private Long nvl(Long v) {
        return v == null ? 0L : v;
    }

}
