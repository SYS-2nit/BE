package com.sys.dbmonitor.domains.sql.domain;

import com.sys.dbmonitor.domains.sql.util.SqlNullUtils;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;

/**************************************************
 작성자 : 오수경
 *************************************************/

/**
 * SQL 통계 메트릭 타입 Enum
 * 메트릭 이름의 타입 안정성과 재사용성을 제공
 */
public enum MetricType {
    ELAPSED("elapsed", Sql::getElapsedUsDelta, "elapsed_sum"),
    AVG("avg", s -> {
        long exec = SqlNullUtils.nvl(s.getExecutionsDelta());
        return exec == 0 ? 0L : SqlNullUtils.nvl(s.getElapsedUsDelta()) / exec;
    }, "avg_elapsed"),
    WAIT("wait", Sql::getWaitTimeUsDelta, "wait_sum"),
    EXECUTION("execution", Sql::getExecutionsDelta, "exec_sum"),
    BUFFER("buffer", Sql::getBufferGetsDelta, "buffer_sum"),
    DISK("disk", Sql::getDiskReadsDelta, "disk_sum"),
    CPU("cpu", Sql::getCpuUsDelta, "cpu_sum");

    private final String name;
    private final Function<Sql, Long> extractor;
    private final String orderByColumn;

    MetricType(String name, Function<Sql, Long> extractor, String orderByColumn) {
        this.name = name;
        this.extractor = extractor;
        this.orderByColumn = orderByColumn;
    }

    /**
     * 문자열로부터 MetricType 찾기
     * @param name 메트릭 이름
     * @return MetricType (없으면 ELAPSED 반환)
     */
    public static MetricType from(String name) {
        if (name == null || name.isEmpty()) {
            return ELAPSED;
        }
        return Arrays.stream(values())
                .filter(m -> m.name.equalsIgnoreCase(name))
                .findFirst()
                .orElse(ELAPSED);
    }

    /**
     * Sql 객체에서 메트릭 값 추출
     */
    public Long extract(Sql sql) {
        return extractor.apply(sql);
    }

    /**
     * ORDER BY 절에서 사용할 컬럼명 반환
     */
    public String getOrderByColumn() {
        return orderByColumn;
    }

    /**
     * 메트릭 이름 반환
     */
    public String getName() {
        return name;
    }

    /**
     * SqlResponse 정렬용 Comparator 생성
     */
    public Comparator<com.sys.dbmonitor.domains.sql.dto.response.SqlResponse> getComparator() {
        return switch (this) {
            case ELAPSED -> Comparator.comparing(com.sys.dbmonitor.domains.sql.dto.response.SqlResponse::elapsedUsDelta);
            case CPU -> Comparator.comparing(com.sys.dbmonitor.domains.sql.dto.response.SqlResponse::cpuUsDelta);
            case BUFFER -> Comparator.comparing(com.sys.dbmonitor.domains.sql.dto.response.SqlResponse::bufferGetsDelta);
            case DISK -> Comparator.comparing(com.sys.dbmonitor.domains.sql.dto.response.SqlResponse::diskReadsDelta);
            case WAIT -> Comparator.comparing(com.sys.dbmonitor.domains.sql.dto.response.SqlResponse::waitTimeUsDelta);
            case EXECUTION -> Comparator.comparing(com.sys.dbmonitor.domains.sql.dto.response.SqlResponse::executionsDelta);
            case AVG -> Comparator.comparing(com.sys.dbmonitor.domains.sql.dto.response.SqlResponse::avgElapsed);
        };
    }
}

