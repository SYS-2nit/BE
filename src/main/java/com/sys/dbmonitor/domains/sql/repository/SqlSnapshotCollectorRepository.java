/**************************************************
 작성자 : 최온유
 *************************************************/

package com.sys.dbmonitor.domains.sql.repository;

import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.sql.dto.SqlSnapshotRawDTO;
import com.sys.dbmonitor.global.config.DynamicDataSourceFactory;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Clob;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 타겟 DB에서 SQL 스냅샷 수집 Repository
 */
@Repository
@RequiredArgsConstructor
public class SqlSnapshotCollectorRepository {

    private final InstanceRepository instanceRepository;
    private final DynamicDataSourceFactory dynamicDataSourceFactory;

    private static final String COLLECT_SQL_PATH = "sql/collect_top_sql.sql";

    /**
     * 타겟 DB에서 SQL 스냅샷 수집
     * @param instanceId 인스턴스 ID
     * @param lookbackMin 최근 N분 동안의 SQL 조회
     * @param bucketTop 버킷별 Top N
     * @param storeLimit 최종 저장할 SQL 개수
     * @return 수집된 SQL 스냅샷 리스트
     */
    public List<SqlSnapshotRawDTO> collect(Long instanceId, int lookbackMin, int bucketTop, int storeLimit) {
        // 인스턴스 존재 확인
        instanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB를 찾을 수 없습니다: instanceId=" + instanceId));

        // 타겟 DB 데이터소스 가져오기
        DataSource dataSource = dynamicDataSourceFactory.getDataSource(instanceId);
        if (dataSource == null) {
            throw new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB가 연결되지 않았습니다. 먼저 연결해주세요.");
        }

        // SQL 파일 로드
        String sql = loadClasspathSql(COLLECT_SQL_PATH);

        // JdbcTemplate으로 쿼리 실행
        // Oracle은 ?1, ?2, ?3 형식의 위치 기반 바인딩을 사용
        // 하지만 JdbcTemplate은 ?만 지원하므로, SQL을 직접 치환하거나 PreparedStatement 사용
        // 간단하게 SQL 문자열 치환 사용
        sql = sql.replace("?1", String.valueOf(lookbackMin))
                .replace("?2", String.valueOf(bucketTop))
                .replace("?3", String.valueOf(storeLimit));

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        // 결과를 DTO로 변환
        List<SqlSnapshotRawDTO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            SqlSnapshotRawDTO dto = mapToDto(row);
            result.add(dto);
        }

        return result;
    }

    /**
     * ResultSet 행을 DTO로 변환
     */
    private SqlSnapshotRawDTO mapToDto(Map<String, Object> row) {
        return SqlSnapshotRawDTO.builder()
                .sqlId(getString(row, "SQL_ID"))
                .planHashValue(getLong(row, "PLAN_HASH_VALUE"))
                .executionsTot(getLong(row, "EXECUTIONS_TOT"))
                .elapsedTimeUsTot(getLong(row, "ELAPSED_TIME_US_TOT"))
                .cpuTimeUsTot(getLong(row, "CPU_TIME_US_TOT"))
                .waitTimeUsTot(getLong(row, "WAIT_TIME_US_TOT"))
                .bufferGetsTot(getLong(row, "BUFFER_GETS_TOT"))
                .diskReadsTot(getLong(row, "DISK_READS_TOT"))
                .userIoWaitUsTot(getLong(row, "USER_IO_WAIT_US_TOT"))
                .concurrencyWaitUsTot(getLong(row, "CONCURRENCY_WAIT_US_TOT"))
                .applicationWaitUsTot(getLong(row, "APPLICATION_WAIT_US_TOT"))
                .clusterWaitUsTot(getLong(row, "CLUSTER_WAIT_US_TOT"))
                .plsqlExecUsTot(getLong(row, "PLSQL_EXEC_US_TOT"))
                .javaExecUsTot(getLong(row, "JAVA_EXEC_US_TOT"))
                .lastActiveTimeMax(getTimestamp(row, "LAST_ACTIVE_TIME_MAX"))
                .parsingSchemaNameAny(getString(row, "PARSING_SCHEMA_NAME_ANY"))
                .moduleAny(getString(row, "MODULE_ANY"))
                .sqlText(getString(row, "SQL_TEXT"))
                .planTextClob(getClobString(row, "PLAN_TEXT_CLOB"))
                .build();
    }

    /**
     * 클래스패스에서 SQL 파일 로드
     */
    private String loadClasspathSql(String path) {
        try (InputStream is = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(path),
                "SQL not found: " + path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load SQL: " + path, e);
        }
    }

    // 헬퍼 메서드들
    private String getString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLong(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime getTimestamp(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        return null;
    }

    /**
     * CLOB 타입을 String으로 변환
     */
    private String getClobString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Clob) {
            try {
                Clob clob = (Clob) value;
                long length = clob.length();
                if (length == 0) {
                    return null;
                }
                return clob.getSubString(1, (int) length);
            } catch (Exception e) {
                return null;
            }
        }
        return value.toString();
    }
}

