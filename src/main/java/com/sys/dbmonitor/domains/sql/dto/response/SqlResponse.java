package com.sys.dbmonitor.domains.sql.dto.response;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Schema(description = "SQL 데이터 응답")
public record SqlResponse(
        @Schema(description = "SQL 데이터 ID") Long id,
        @Schema(description = "Instance ID") Long instanceId,
        @Schema(description = "SQL ID") String sqlId,
        @Schema(description = "SQL 텍스트") String sqlText,
        @Schema(description = "Elapsed Time") Long elapsedUsDelta,
        @Schema(description = "Avg Elapsed") Long avgElapsed,
        @Schema(description = "총 Wait Time") Long waitTimeUsDelta,
        @Schema(description = "Executions Delta") Long executionsDelta,
        @Schema(description = "Buffer Gets Delta") Long bufferGetsDelta,
        @Schema(description = "Disk Reads Delta") Long diskReadsDelta,
        @Schema(description = "CPU 사용량") Long cpuUsDelta
) {
    public static SqlResponse from(Sql sql) {

        Long avgElapsed = 0L;
        if (sql.getExecutionsDelta() != null &&
                sql.getExecutionsDelta() > 0 &&
                sql.getElapsedUsDelta() != null) {

            avgElapsed = sql.getElapsedUsDelta() / sql.getExecutionsDelta();
        }

        return new SqlResponse(
                sql.getId(),
                sql.getInstanceId(),
                sql.getSqlId(),
                sql.getSqlText(),
                sql.getElapsedUsDelta(),
                avgElapsed,
                sql.getWaitTimeUsDelta(),
                sql.getExecutionsDelta(),
                sql.getBufferGetsDelta(),
                sql.getDiskReadsDelta(),
                sql.getCpuUsDelta()
        );
    }

    /** SQL TEXT 기준 GROUP BY 변환 메서드 (집계값 DTO 변환용) */
    private List<SqlDetailResponse.TrendPoint> convert(
            Map<Long, Long> trend,
            LocalDateTime startAt,
            int interval
    ) {
        List<SqlDetailResponse.TrendPoint> result = new List<SqlDetailResponse.TrendPoint>() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<SqlDetailResponse.TrendPoint> iterator() {
                return null;
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return null;
            }

            @Override
            public boolean add(SqlDetailResponse.TrendPoint trendPoint) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(Collection<? extends SqlDetailResponse.TrendPoint> c) {
                return false;
            }

            @Override
            public boolean addAll(int index, Collection<? extends SqlDetailResponse.TrendPoint> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {

            }

            @Override
            public boolean equals(Object o) {
                return false;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public SqlDetailResponse.TrendPoint get(int index) {
                return null;
            }

            @Override
            public SqlDetailResponse.TrendPoint set(int index, SqlDetailResponse.TrendPoint element) {
                return null;
            }

            @Override
            public void add(int index, SqlDetailResponse.TrendPoint element) {

            }

            @Override
            public SqlDetailResponse.TrendPoint remove(int index) {
                return null;
            }

            @Override
            public int indexOf(Object o) {
                return 0;
            }

            @Override
            public int lastIndexOf(Object o) {
                return 0;
            }

            @Override
            public ListIterator<SqlDetailResponse.TrendPoint> listIterator() {
                return null;
            }

            @Override
            public ListIterator<SqlDetailResponse.TrendPoint> listIterator(int index) {
                return null;
            }

            @Override
            public List<SqlDetailResponse.TrendPoint> subList(int fromIndex, int toIndex) {
                return List.of();
            }
        };

        for (Map.Entry<Long, Long> entry : trend.entrySet()) {

            long idx = entry.getKey();
            long value = entry.getValue();

            LocalDateTime time = startAt.plusMinutes(idx * interval);
            String label = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            result.add(new SqlDetailResponse.TrendPoint(label, value));
        }

        return result;
    }

}
