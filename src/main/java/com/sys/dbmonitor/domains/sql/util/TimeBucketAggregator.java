package com.sys.dbmonitor.domains.sql.util;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.response.SqlDetailResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlGraphSeriesResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/**
 * 시간 버킷 집계 유틸리티
 * SQL 데이터를 시간 단위로 버킷팅하여 집계하는 로직을 제공
 */
public final class TimeBucketAggregator {

    private TimeBucketAggregator() {
        // 유틸리티 클래스는 인스턴스화 불가
    }

    /**
     * 시간 기반 버킷 집계 (기간별 그래프용)
     * @param items 집계할 데이터 리스트
     * @param startAt 시작 시간
     * @param endAt 종료 시간
     * @param intervalMinutes 버킷 간격 (분)
     * @param timeExtractor 시간 추출 함수
     * @param valueExtractor 값 추출 함수
     * @return 버킷 인덱스와 값의 맵
     */
    public static <T> Map<Long, Long> aggregateByTimeBucket(
            List<T> items,
            LocalDateTime startAt,
            LocalDateTime endAt,
            int intervalMinutes,
            Function<T, LocalDateTime> timeExtractor,
            Function<T, Long> valueExtractor
    ) {
        Map<Long, Long> bucketMap = initializeTimeBuckets(startAt, endAt, intervalMinutes);

        for (T item : items) {
            LocalDateTime itemTime = timeExtractor.apply(item);
            long minutes = Duration.between(startAt, itemTime).toMinutes();
            
            if (minutes < 0) continue;

            long bucketIndex = minutes / intervalMinutes;
            
            if (bucketMap.containsKey(bucketIndex)) {
                Long currentValue = bucketMap.get(bucketIndex);
                Long itemValue = valueExtractor.apply(item);
                bucketMap.put(bucketIndex, currentValue + SqlNullUtils.nvl(itemValue));
            }
        }

        return bucketMap;
    }

    /**
     * 시간대 기반 버킷 집계 (일별 그래프용)
     * @param items 집계할 데이터 리스트
     * @param startAt 시작 시간 (하루의 시작)
     * @param intervalMinutes 버킷 간격 (분)
     * @param valueExtractor 값 추출 함수
     * @return 버킷 인덱스(시간대)와 값의 맵
     */
    public static <T> Map<Integer, Long> aggregateByHourBucket(
            List<T> items,
            LocalDateTime startAt,
            int intervalMinutes,
            Function<T, LocalDateTime> timeExtractor,
            Function<T, Long> valueExtractor
    ) {
        int buckets = (24 * 60) / intervalMinutes;
        Map<Integer, Long> bucketMap = new LinkedHashMap<>();

        // 버킷 초기화
        for (int i = 0; i < buckets; i++) {
            bucketMap.put(i, 0L);
        }

        // 데이터 집계
        for (T item : items) {
            LocalDateTime itemTime = timeExtractor.apply(item);
            int minutes = itemTime.getHour() * 60 + itemTime.getMinute();
            int idx = minutes / intervalMinutes;

            if (bucketMap.containsKey(idx)) {
                Long currentValue = bucketMap.get(idx);
                Long itemValue = valueExtractor.apply(item);
                bucketMap.put(idx, currentValue + SqlNullUtils.nvl(itemValue));
            }
        }

        return bucketMap;
    }

    /**
     * 시간 버킷 초기화
     */
    private static Map<Long, Long> initializeTimeBuckets(
            LocalDateTime startAt,
            LocalDateTime endAt,
            int intervalMinutes
    ) {
        Map<Long, Long> bucketMap = new TreeMap<>();
        long totalMinutes = Duration.between(startAt, endAt).toMinutes();
        long bucketCount = (totalMinutes / intervalMinutes) + 1;

        for (long i = 0; i < bucketCount; i++) {
            bucketMap.put(i, 0L);
        }

        return bucketMap;
    }

    /**
     * 버킷 맵을 SqlGraphSeriesResponse.Bucket 리스트로 변환
     */
    public static List<SqlGraphSeriesResponse.Bucket> convertToGraphBuckets(
            Map<Long, Long> bucketMap,
            LocalDateTime startAt,
            int intervalMinutes
    ) {
        List<SqlGraphSeriesResponse.Bucket> buckets = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Map.Entry<Long, Long> entry : bucketMap.entrySet()) {
            long bucketIndex = entry.getKey();
            long sumValue = entry.getValue();

            LocalDateTime labelTime = startAt.plusMinutes(bucketIndex * intervalMinutes);
            String label = labelTime.format(formatter);

            buckets.add(new SqlGraphSeriesResponse.Bucket(label, sumValue));
        }

        return buckets;
    }

    /**
     * 버킷 맵을 SqlDetailResponse.TrendPoint 리스트로 변환
     */
    public static List<SqlDetailResponse.TrendPoint> convertToTrendPoints(
            Map<Long, Long> trend,
            LocalDateTime startAt,
            int interval
    ) {
        List<SqlDetailResponse.TrendPoint> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Map.Entry<Long, Long> entry : trend.entrySet()) {
            long idx = entry.getKey();
            long value = entry.getValue();

            LocalDateTime time = startAt.plusMinutes(idx * interval);
            String label = time.format(formatter);

            result.add(new SqlDetailResponse.TrendPoint(label, value));
        }

        return result;
    }

    /**
     * 시간대 버킷을 일별 그래프 응답으로 변환
     */
    public static List<com.sys.dbmonitor.domains.sql.dto.response.SqlDailyGraphResponse> convertToDailyGraphBuckets(
            Map<Integer, Long> bucketMap,
            LocalDateTime startAt,
            int intervalMinutes
    ) {
        List<com.sys.dbmonitor.domains.sql.dto.response.SqlDailyGraphResponse> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        for (int i = 0; i < bucketMap.size(); i++) {
            LocalDateTime t = startAt.plusMinutes((long) i * intervalMinutes);
            result.add(new com.sys.dbmonitor.domains.sql.dto.response.SqlDailyGraphResponse(
                    t.format(formatter),
                    bucketMap.get(i)
            ));
        }

        return result;
    }
}

