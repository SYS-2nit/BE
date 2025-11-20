package com.sys.dbmonitor.domains.sql.service.query;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.request.SqlCompareRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlDailyGraphRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlGraphRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlStatsQueryRequest;
import com.sys.dbmonitor.domains.sql.dto.response.*;
import com.sys.dbmonitor.domains.sql.repository.SqlRepository;
import jakarta.validation.Valid;
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


    /** SQL 통계 목록 조회 API */
    @Transactional(readOnly = true)
    public SqlStatsPageResponse getSqlStats(SqlStatsQueryRequest request) {

        LocalDateTime start = request.startDate() != null
                ? request.startDate().atStartOfDay()
                : LocalDateTime.now().minusDays(1);

        LocalDateTime end = request.endDate() != null
                ? request.endDate().plusDays(1).atStartOfDay()
                : LocalDateTime.now();

        // 정렬 기준 및 방향 설정
        String orderBy = request.orderBy() != null ? request.orderBy() : "elapsed";
        Sort.Direction direction = Sort.Direction.fromString(request.direction() != null ? request.direction() : "DESC");

        // 전체 데이터 조회 (페이지네이션 없이)
        List<Sql> allSqls = sqlRepository.findAllForStats(
                request.instanceId(),
                start,
                end
        );

        // SQL TEXT 기준 그룹핑
        Map<String, List<Sql>> grouped = allSqls.stream()
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
                .collect(Collectors.toList());

        // 정렬 적용 (orderBy와 direction에 따라)
        Comparator<SqlResponse> comparator = getComparator(orderBy, direction);
        groupedList.sort(comparator);

        // 전체 데이터 반환 (클라이언트 사이드 페이지네이션)
        int total = groupedList.size();
        
        // Page 객체 생성 (전체 데이터를 content로 설정)
        Pageable pageable = PageRequest.of(0, total > 0 ? total : 1);
        Page<SqlResponse> pageResult = new PageImpl<>(
                groupedList,
                pageable,
                total
        );

        return SqlStatsPageResponse.from(pageResult);
    }


    /** SQL 그래프 데이터 조회 API */
    @Transactional(readOnly = true)
    public SqlGraphSeriesResponse getSqlGraphData(SqlGraphRequest request) {

        LocalDate start = LocalDate.parse(request.startDate());
        LocalDate end = LocalDate.parse(request.endDate());

        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = end.plusDays(1).atStartOfDay();

        int interval = (request.intervalMinutes() == null || request.intervalMinutes() <= 0)
                ? 30
                : request.intervalMinutes();


        // 1) 원본 DB 데이터 조회
        List<Sql> list = sqlRepository.findForGraph(
                request.instanceId(),
                request.filter(),
                startAt,
                endAt
        );

        // interval 단위 bucket 초기 생성 (빈 버킷은 0으로 유지)
        Map<Long, Long> bucketMap = new TreeMap<>();

        long totalMinutes = Duration.between(startAt, endAt).toMinutes();
        long bucketCount = (totalMinutes / interval) + 1;

        for (long i = 0; i < bucketCount; i++) {
            bucketMap.put(i, 0L); // 기본값 0 세팅
        }

        // metric getter
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

        // Row → bucket 매핑해서 누적 계산
        for (Sql s : list) {

            long minutes = Duration.between(startAt, s.getCreatedAt()).toMinutes();

            if (minutes < 0) continue;

            long bucketIndex = minutes / interval;

            if (bucketMap.containsKey(bucketIndex)) {
                bucketMap.put(bucketIndex, bucketMap.get(bucketIndex) + metricGetter.apply(s));
            }
        }

        // bucket → 응답 변환
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

    /** 정렬 기준에 따른 Comparator 생성 */
    private Comparator<SqlResponse> getComparator(String orderBy, Sort.Direction direction) {
        Comparator<SqlResponse> baseComparator = switch (orderBy.toLowerCase()) {
            case "elapsed" -> Comparator.comparing(SqlResponse::elapsedUsDelta);
            case "cpu" -> Comparator.comparing(SqlResponse::cpuUsDelta);
            case "buffer" -> Comparator.comparing(SqlResponse::bufferGetsDelta);
            case "disk" -> Comparator.comparing(SqlResponse::diskReadsDelta);
            case "wait" -> Comparator.comparing(SqlResponse::waitTimeUsDelta);
            case "execution" -> Comparator.comparing(SqlResponse::executionsDelta);
            case "avg" -> Comparator.comparing(SqlResponse::avgElapsed);
            default -> Comparator.comparing(SqlResponse::elapsedUsDelta); // 기본값: elapsed
        };

        return direction == Sort.Direction.ASC 
                ? baseComparator 
                : baseComparator.reversed();
    }

    /** SQL 상세 탭 데이터 조회 API */
    @Transactional(readOnly = true)
    public SqlDetailResponse getSqlDetail(String sqlId, String startDate, String endDate, Integer intervalMinutes) {

        // 날짜 파싱
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = end.plusDays(1).atStartOfDay();

        int interval = intervalMinutes == null ? 30 : intervalMinutes;

        // 모든 행 조회 (해당 SQL ID)
        List<Sql> list = sqlRepository.findBySqlIdAndDateRange(sqlId, startAt, endAt);

        if (list.isEmpty()) {
            // throw new RuntimeException("SQL 데이터가 존재하지 않습니다.");
            // FE는 빈 상세 데이터를 받아도 정상 처리해야 하므로
            return SqlDetailResponse.empty(sqlId, startDate, endDate);
        }

        // 누적값 계산
        long totalElapsed = list.stream().mapToLong(s -> nvl(s.getElapsedUsDelta())).sum();
        long totalCpu = list.stream().mapToLong(s -> nvl(s.getCpuUsDelta())).sum();
        long totalExec = list.stream().mapToLong(s -> nvl(s.getExecutionsDelta())).sum();
        long totalBuffer = list.stream().mapToLong(s -> nvl(s.getBufferGetsDelta())).sum();
        long totalDisk = list.stream().mapToLong(s -> nvl(s.getDiskReadsDelta())).sum();
        long totalWait = list.stream().mapToLong(s -> nvl(s.getWaitTimeUsDelta())).sum();
        long totalWaitTime = list.stream().mapToLong(s -> nvl(s.getWaitTimeUsDelta())).sum();
        long totalWaitUserIo = list.stream().mapToLong(s -> nvl(s.getWaitUserIoUsDelta())).sum();
        long totalWaitConcurrency = list.stream().mapToLong(s -> nvl(s.getWaitConcurrencyUsDelta())).sum();
        long totalWaitApplication = list.stream().mapToLong(s -> nvl(s.getWaitApplicationUsDelta())).sum();
        long totalWaitCluster = list.stream().mapToLong(s -> nvl(s.getWaitClusterUsDelta())).sum();

        long avgElapsed = (totalExec == 0 ? 0 : totalElapsed / totalExec);

        // wait_other 계산
        long totalWaitOther = totalWaitTime
                - (totalWaitUserIo + totalWaitConcurrency + totalWaitApplication + totalWaitCluster);
        if (totalWaitOther < 0) totalWaitOther = 0;

        // 시간대 버킷 생성
        Map<Long, Long> elapsedTrend = makeTrend(list, startAt, interval, Sql::getElapsedUsDelta);
        Map<Long, Long> cpuTrend = makeTrend(list, startAt, interval, Sql::getCpuUsDelta);
        Map<Long, Long> execTrend = makeTrend(list, startAt, interval, Sql::getExecutionsDelta);
        Map<Long, Long> bufferTrend = makeTrend(list, startAt, interval, Sql::getBufferGetsDelta);
        Map<Long, Long> diskTrend = makeTrend(list, startAt, interval, Sql::getDiskReadsDelta);
        Map<Long, Long> waitTrend = makeTrend(list, startAt, interval, Sql::getWaitTimeUsDelta);

        // 순위 및 비중 계산
        long totalElapsedAll = sqlRepository.sumElapsedForRange(startAt, endAt);
        double ratio = (totalElapsed == 0 || totalElapsedAll == 0)
                ? 0
                : (double) totalElapsed / totalElapsedAll;

        int rank = sqlRepository.findRankByElapsed(sqlId, startAt, endAt);

        return new SqlDetailResponse(
                list.get(0).getId(),
                list.get(0).getInstanceId(),
                sqlId,
                list.get(0).getSqlText(),

                totalElapsed,
                totalCpu,
                totalExec,
                totalBuffer,
                totalDisk,
                totalWait,
                avgElapsed,
                totalWaitTime,
                totalWaitUserIo,
                totalWaitConcurrency,
                totalWaitApplication,
                totalWaitCluster,
                totalWaitOther,

                convert(elapsedTrend, startAt, interval),
                convert(cpuTrend, startAt, interval),
                convert(execTrend, startAt, interval),
                convert(bufferTrend, startAt, interval),
                convert(diskTrend, startAt, interval),
                convert(waitTrend, startAt, interval),
                rank,
                ratio
        );
    }

    /* ====== Trend 변환 메서드 (상세 탭 그래프용) ====== */
    private List<SqlDetailResponse.TrendPoint> convert(
            Map<Long, Long> trend,
            LocalDateTime startAt,
            int interval
    ) {
        List<SqlDetailResponse.TrendPoint> result = new ArrayList<>();

        for (Map.Entry<Long, Long> entry : trend.entrySet()) {
            long idx = entry.getKey();
            long value = entry.getValue();

            LocalDateTime time = startAt.plusMinutes(idx * interval);
            String label = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            result.add(new SqlDetailResponse.TrendPoint(label, value));
        }

        return result;
    }


    /* ====== 시간대 단위 bucket 집계 ====== */
    private Map<Long, Long> makeTrend(
            List<Sql> list,
            LocalDateTime startAt,
            int interval,
            Function<Sql, Long> getter
    ) {
        Map<Long, Long> trend = new TreeMap<>();

        for (Sql s : list) {
            long minutes = Duration.between(startAt, s.getCreatedAt()).toMinutes();
            if (minutes < 0) continue;

            long bucketIndex = minutes / interval;

            trend.put(bucketIndex,
                    trend.getOrDefault(bucketIndex, 0L) + nvl(getter.apply(s))
            );
        }

        return trend;
    }

    /* ====== Top SQL 비교 ====== */
    public SqlComparePageResponse getSqlCompareStats(@Valid SqlCompareRequest req) {

        // base 구간 (하루 기준)
        LocalDate baseStart = req.baseDate();
        LocalDate baseEnd = req.baseDate();

        // compare 구간 (하루 기준)
        LocalDate compStart = req.compareDate();
        LocalDate compEnd = req.compareDate();


        // 기준 구간 SQL 조회
        SqlStatsQueryRequest baseReq = new SqlStatsQueryRequest(
                req.instanceId(),
                baseStart,
                baseEnd,
                "elapsed",
                "DESC"
        );
        SqlStatsPageResponse base = getSqlStats(baseReq);


        // 비교 구간 SQL 조회
        SqlStatsQueryRequest compareReq = new SqlStatsQueryRequest(
                req.instanceId(),
                compStart,
                compEnd,
                "elapsed",
                "DESC"
        );
        SqlStatsPageResponse compare = getSqlStats(compareReq);


        // 비교 결과 응답
        return new SqlComparePageResponse(
                base.content(),
                compare.content()
        );
    }

    @Transactional(readOnly = true)
    public List<SqlDailyGraphResponse> getDailySqlGraph(
            String date,
            String metric,
            Long instanceId,
            Integer intervalMinutes
    ) {

        LocalDate target = LocalDate.parse(date);
        LocalDateTime start = target.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<Sql> raw = sqlRepository.findForGraph(
                instanceId,
                null,
                start,
                end
        );

        // 시간 버킷팅
        Map<Integer, Long> bucket = new LinkedHashMap<>();
        int buckets = (24 * 60) / intervalMinutes;

        for (int i = 0; i < buckets; i++) {
            bucket.put(i, 0L);
        }

        for (Sql s : raw) {
            int minutes = s.getCreatedAt().getHour() * 60 + s.getCreatedAt().getMinute();
            int idx = minutes / intervalMinutes;

            long metricValue = switch (metric) {
                case "elapsed" -> s.getElapsedUsDelta();
                case "wait" -> s.getWaitTimeUsDelta();
                case "avg" -> s.getAvgElapsed();
                case "execute" -> s.getExecutionsDelta();
                default -> 0L;
            };

            bucket.put(idx, bucket.get(idx) + metricValue);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

        List<SqlDailyGraphResponse> result = new ArrayList<>();

        for (int i = 0; i < buckets; i++) {
            LocalDateTime t = start.plusMinutes((long) i * intervalMinutes);

            result.add(new SqlDailyGraphResponse(
                    t.format(fmt),
                    bucket.get(i)
            ));
        }

        return result;
    }

    public List<SqlDailyGraphResponse> getDailySqlGraph(SqlDailyGraphRequest req) {
        return getDailySqlGraph(
                req.date(),
                req.metric(),
                req.instanceId(),
                req.intervalMinutes()
        );
    }

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

        List<SqlPeriodGraphResponse> result = new ArrayList<>();

        // 하루씩 반복
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {

            SqlDailyGraphRequest req = new SqlDailyGraphRequest(
                    day.toString(),
                    metric,
                    instanceId,
                    intervalMinutes
            );

            // 오버로드된 함수 호출
            List<SqlDailyGraphResponse> dailyGraph = getDailySqlGraph(req);

            for (SqlDailyGraphResponse d : dailyGraph) {
                String full = day + " " + d.time();
                result.add(new SqlPeriodGraphResponse(full, d.value()));
            }
        }

        return result;
    }

}
