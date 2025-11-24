package com.sys.dbmonitor.domains.sql.service.query;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.response.SqlDetailResponse;
import com.sys.dbmonitor.domains.sql.repository.SqlRepository;
import com.sys.dbmonitor.domains.sql.util.SqlDateUtils;
import com.sys.dbmonitor.domains.sql.util.SqlNullUtils;
import com.sys.dbmonitor.domains.sql.util.TimeBucketAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import com.sys.dbmonitor.domains.sql.util.SqlStats;

/**
 * SQL 상세 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlDetailService {

    private final SqlRepository sqlRepository;

    /**
     * SQL 상세 탭 데이터 조회
     */
    @Transactional(readOnly = true)
    public SqlDetailResponse getSqlDetail(String sqlId, String startDate, String endDate, Integer intervalMinutes) {
        long startTime = System.currentTimeMillis();
        LocalDateTime startAt = SqlDateUtils.parseStartDate(startDate);
        LocalDateTime endAt = SqlDateUtils.parseEndDate(endDate);
        int interval = SqlDateUtils.getDefaultInterval(intervalMinutes);

        // 모든 행 조회 (해당 SQL ID)
        List<Sql> list = sqlRepository.findBySqlIdAndDateRange(sqlId, startAt, endAt);

        if (list.isEmpty()) {
            long endTime = System.currentTimeMillis();
            log.info("[성능 측정] getSqlDetail - 실행시간: {}ms, 데이터수: 0 (빈 결과)", 
                    endTime - startTime);
            return SqlDetailResponse.empty(sqlId, startDate, endDate);
        }

        // 한 번의 Stream 순회로 모든 누적값 계산 (Collector 사용)
        long streamStartTime = System.nanoTime();
        SqlStats stats = list.stream().collect(
                Collector.of(
                        SqlStats::new,
                        (acc, sql) -> {
                            // 모든 필드 한 번에 계산
                            acc.elapsed += SqlNullUtils.nvl(sql.getElapsedUsDelta());
                            acc.cpu += SqlNullUtils.nvl(sql.getCpuUsDelta());
                            acc.exec += SqlNullUtils.nvl(sql.getExecutionsDelta());
                            acc.buffer += SqlNullUtils.nvl(sql.getBufferGetsDelta());
                            acc.disk += SqlNullUtils.nvl(sql.getDiskReadsDelta());
                            acc.wait += SqlNullUtils.nvl(sql.getWaitTimeUsDelta());
                            acc.waitTime += SqlNullUtils.nvl(sql.getWaitTimeUsDelta());
                            acc.waitUserIo += SqlNullUtils.nvl(sql.getWaitUserIoUsDelta());
                            acc.waitConcurrency += SqlNullUtils.nvl(sql.getWaitConcurrencyUsDelta());
                            acc.waitApplication += SqlNullUtils.nvl(sql.getWaitApplicationUsDelta());
                            acc.waitCluster += SqlNullUtils.nvl(sql.getWaitClusterUsDelta());
                        },
                        (a, b) -> {
                            // 병렬 스트림을 위한 병합 로직
                            SqlStats merged = new SqlStats();
                            merged.elapsed = a.elapsed + b.elapsed;
                            merged.cpu = a.cpu + b.cpu;
                            merged.exec = a.exec + b.exec;
                            merged.buffer = a.buffer + b.buffer;
                            merged.disk = a.disk + b.disk;
                            merged.wait = a.wait + b.wait;
                            merged.waitTime = a.waitTime + b.waitTime;
                            merged.waitUserIo = a.waitUserIo + b.waitUserIo;
                            merged.waitConcurrency = a.waitConcurrency + b.waitConcurrency;
                            merged.waitApplication = a.waitApplication + b.waitApplication;
                            merged.waitCluster = a.waitCluster + b.waitCluster;
                            return merged;
                        }
                )
        );

        long totalElapsed = stats.elapsed;
        long totalCpu = stats.cpu;
        long totalExec = stats.exec;
        long totalBuffer = stats.buffer;
        long totalDisk = stats.disk;
        long totalWait = stats.wait;
        long totalWaitTime = stats.waitTime;
        long totalWaitUserIo = stats.waitUserIo;
        long totalWaitConcurrency = stats.waitConcurrency;
        long totalWaitApplication = stats.waitApplication;
        long totalWaitCluster = stats.waitCluster;

        long avgElapsed = (totalExec == 0 ? 0 : totalElapsed / totalExec);

        // wait_other 계산
        long totalWaitOther = totalWaitTime
                - (totalWaitUserIo + totalWaitConcurrency + totalWaitApplication + totalWaitCluster);
        if (totalWaitOther < 0) totalWaitOther = 0;

        // 시간대 버킷 생성 (트렌드)
        Map<Long, Long> elapsedTrend = aggregateTrend(list, startAt, endAt, interval, Sql::getElapsedUsDelta);
        Map<Long, Long> cpuTrend = aggregateTrend(list, startAt, endAt, interval, Sql::getCpuUsDelta);
        Map<Long, Long> execTrend = aggregateTrend(list, startAt, endAt, interval, Sql::getExecutionsDelta);
        Map<Long, Long> bufferTrend = aggregateTrend(list, startAt, endAt, interval, Sql::getBufferGetsDelta);
        Map<Long, Long> diskTrend = aggregateTrend(list, startAt, endAt, interval, Sql::getDiskReadsDelta);
        Map<Long, Long> waitTrend = aggregateTrend(list, startAt, endAt, interval, Sql::getWaitTimeUsDelta);

        // 순위 및 비중 계산
        long totalElapsedAll = sqlRepository.sumElapsedForRange(startAt, endAt);
        double ratio = (totalElapsed == 0 || totalElapsedAll == 0)
                ? 0
                : (double) totalElapsed / totalElapsedAll;

        int rank = sqlRepository.findRankByElapsed(sqlId, startAt, endAt);

        long endTime = System.currentTimeMillis();
        long streamEndTime = System.nanoTime();
        long streamDuration = (streamEndTime - streamStartTime) / 1_000_000; // 나노초를 밀리초로 변환
        long totalDuration = endTime - startTime;
        
        // 성능 측정 로그 출력
        log.info("========================================");
        log.info("[성능 측정] getSqlDetail");
        log.info("  전체 실행시간: {}ms ({}초)", totalDuration, totalDuration / 1000.0);
        log.info("  Stream 순회시간: {}ms", streamDuration);
        log.info("  데이터수: {}개", list.size());
        log.info("  순회횟수: 1회");
        log.info("  SQL ID: {}", sqlId);
        log.info("  기간: {} ~ {}", startDate, endDate);
        log.info("========================================");

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
                TimeBucketAggregator.convertToTrendPoints(elapsedTrend, startAt, interval),
                TimeBucketAggregator.convertToTrendPoints(cpuTrend, startAt, interval),
                TimeBucketAggregator.convertToTrendPoints(execTrend, startAt, interval),
                TimeBucketAggregator.convertToTrendPoints(bufferTrend, startAt, interval),
                TimeBucketAggregator.convertToTrendPoints(diskTrend, startAt, interval),
                TimeBucketAggregator.convertToTrendPoints(waitTrend, startAt, interval),
                rank,
                ratio
        );
    }

    /**
     * 트렌드 데이터 집계
     */
    private Map<Long, Long> aggregateTrend(
            List<Sql> list,
            LocalDateTime startAt,
            LocalDateTime endAt,
            int interval,
            Function<Sql, Long> getter
    ) {
        return TimeBucketAggregator.aggregateByTimeBucket(
                list,
                startAt,
                endAt,
                interval,
                Sql::getCreatedAt,
                getter
        );
    }
}

