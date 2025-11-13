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

@Service
@RequiredArgsConstructor
public class SqlStatsQueryService {

    private final SqlRepository sqlRepository;

    /* SQL 통계 목록 조회 API */
    @Transactional(readOnly = true)
    public SqlStatsPageResponse getSqlStats(SqlStatsQueryRequest request) {

        // 날짜 처리 수정: endDate 하루 전체 포함
        LocalDateTime start = request.startDate() != null
                ? request.startDate().atStartOfDay()
                : LocalDateTime.now().minusDays(1);

        LocalDateTime end = request.endDate() != null
                ? request.endDate().plusDays(1).atStartOfDay()     // ⭐ 하루 전체 포함시키는 수정
                : LocalDateTime.now();

        // 정렬 설정
        Sort sort = Sort.by(
                Sort.Direction.fromString(
                        request.direction() != null ? request.direction() : "DESC"
                ),
                switch (request.orderBy() == null ? "elapsed" : request.orderBy()) {
                    case "cpu" -> "cpuUsDelta";
                    case "exec" -> "executionsDelta";
                    case "wait" -> "waitTimeUsDelta";
                    case "buffer" -> "bufferGetsDelta";
                    case "disk" -> "diskReadsDelta";
                    default -> "elapsedUsDelta";
                }
        );

        // 페이지 설정
        Pageable pageable = PageRequest.of(
                request.page() != null ? request.page() : 0,
                request.size() != null ? request.size() : 20,
                sort
        );

        Integer minExec = request.minExecCount() != null ? request.minExecCount() : 0;
        Integer maxExec = request.maxExecCount() != null ? request.maxExecCount() : Integer.MAX_VALUE;

        // 기존 JPA Query + 메모리 필터 조합
        Page<Sql> rawPage = sqlRepository.findFilteredSqlStats(
                request.instanceId(),
                request.keyword(),
                start,
                end,
                pageable
        );

        // 실행 횟수 필터 적용 (그래프와 조건 통일)
        List<Sql> filtered = rawPage.getContent().stream()
                .filter(s -> {
                    long exec = s.getExecutionsDelta() != null ? s.getExecutionsDelta() : 0;
                    return exec >= minExec && exec <= maxExec;
                })
                .toList();

        // 필터 후 페이징 재적용
        Page<Sql> finalPage = new PageImpl<>(filtered, pageable, rawPage.getTotalElements());

        return SqlStatsPageResponse.from(finalPage.map(SqlResponse::from));
    }


    /* SQL 그래프 데이터 조회 API */
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
