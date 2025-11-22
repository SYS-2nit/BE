package com.sys.dbmonitor.domains.sql.service.query;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.response.SqlDetailResponse;
import com.sys.dbmonitor.domains.sql.repository.SqlRepository;
import com.sys.dbmonitor.domains.sql.util.SqlDateUtils;
import com.sys.dbmonitor.domains.sql.util.SqlNullUtils;
import com.sys.dbmonitor.domains.sql.util.TimeBucketAggregator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * SQL 상세 조회 서비스
 */
@Service
@RequiredArgsConstructor
public class SqlDetailService {

    private final SqlRepository sqlRepository;

    /**
     * SQL 상세 탭 데이터 조회
     */
    @Transactional(readOnly = true)
    public SqlDetailResponse getSqlDetail(String sqlId, String startDate, String endDate, Integer intervalMinutes) {
        LocalDateTime startAt = SqlDateUtils.parseStartDate(startDate);
        LocalDateTime endAt = SqlDateUtils.parseEndDate(endDate);
        int interval = SqlDateUtils.getDefaultInterval(intervalMinutes);

        // 모든 행 조회 (해당 SQL ID)
        List<Sql> list = sqlRepository.findBySqlIdAndDateRange(sqlId, startAt, endAt);

        if (list.isEmpty()) {
            return SqlDetailResponse.empty(sqlId, startDate, endDate);
        }

        // 누적값 계산
        long totalElapsed = list.stream().mapToLong(s -> SqlNullUtils.nvl(s.getElapsedUsDelta())).sum();
        long totalCpu = list.stream().mapToLong(s -> SqlNullUtils.nvl(s.getCpuUsDelta())).sum();
        long totalExec = list.stream().mapToLong(s -> SqlNullUtils.nvl(s.getExecutionsDelta())).sum();
        long totalBuffer = list.stream().mapToLong(s -> SqlNullUtils.nvl(s.getBufferGetsDelta())).sum();
        long totalDisk = list.stream().mapToLong(s -> SqlNullUtils.nvl(s.getDiskReadsDelta())).sum();
        long totalWait = list.stream().mapToLong(s -> SqlNullUtils.nvl(s.getWaitTimeUsDelta())).sum();
        long totalWaitTime = list.stream().mapToLong(s -> SqlNullUtils.nvl(s.getWaitTimeUsDelta())).sum();
        long totalWaitUserIo = list.stream().mapToLong(s -> SqlNullUtils.nvl(s.getWaitUserIoUsDelta())).sum();
        long totalWaitConcurrency = list.stream().mapToLong(s -> SqlNullUtils.nvl(s.getWaitConcurrencyUsDelta())).sum();
        long totalWaitApplication = list.stream().mapToLong(s -> SqlNullUtils.nvl(s.getWaitApplicationUsDelta())).sum();
        long totalWaitCluster = list.stream().mapToLong(s -> SqlNullUtils.nvl(s.getWaitClusterUsDelta())).sum();

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

