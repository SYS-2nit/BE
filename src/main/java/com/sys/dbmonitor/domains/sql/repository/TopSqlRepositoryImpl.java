package com.sys.dbmonitor.domains.sql.repository;

import com.sys.dbmonitor.domains.sql.dto.TopSqlRowDTO;
import com.sys.dbmonitor.domains.sql.dto.TopSqlTrendRowDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class TopSqlRepositoryImpl implements TopSqlRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String TOP_SQL_SQL_PATH = "sql/select_top_sql.sql";
    private static final String TOP_SQL_QUERY = loadSql(TOP_SQL_SQL_PATH);

    private static final String TREND_SQL_PATH = "sql/select_top_sql_trend.sql";
    private static final String TOP_SQL_TREND_QUERY = loadSql(TREND_SQL_PATH);

    private static final RowMapper<TopSqlRowDTO> ROW_MAPPER = new RowMapper<>() {
        @Override
        public TopSqlRowDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new TopSqlRowDTO(
                    rs.getString("sql_id"),
                    rs.getLong("plan_hash_value"),
                    rs.getBigDecimal("elapsed_ms_total"),
                    rs.getBigDecimal("wait_ms_total"),
                    rs.getBigDecimal("cpu_ms_total"),
                    rs.getBigDecimal("elapsed_ms_avg"),
                    rs.getLong("executions_total"),
                    rs.getLong("logical_reads_total"),
                    rs.getLong("physical_reads_total"),
                    rs.getString("sql_text"),
                    rs.getBigDecimal("elapsed_ratio"),
                    rs.getBigDecimal("wait_ratio"),
                    rs.getBigDecimal("cpu_ratio"),
                    rs.getBigDecimal("elapsed_avg_ratio"),
                    rs.getBigDecimal("executions_ratio"),
                    rs.getBigDecimal("logical_reads_ratio"),
                    rs.getBigDecimal("physical_reads_ratio")
            );
        }
    };

    @Override
    public List<TopSqlRowDTO> findTopSql(Long instanceId,
                                      LocalDateTime startTs,
                                      LocalDateTime endTs,
                                      String metric,
                                      int topN) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("instanceId", instanceId)
                .addValue("startTs", startTs)
                .addValue("endTs", endTs)
                .addValue("metric", metric)
                .addValue("topN", topN);

        return jdbcTemplate.query(TOP_SQL_QUERY, params, ROW_MAPPER);
    }

    private static final RowMapper<TopSqlTrendRowDTO> TREND_ROW_MAPPER = new RowMapper<>() {
        @Override
        public TopSqlTrendRowDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp ts = rs.getTimestamp("bucket_ts");
            LocalDateTime bucketTs = ts != null ? ts.toLocalDateTime() : null;
            return new TopSqlTrendRowDTO(
                    bucketTs,
                    rs.getBigDecimal("elapsed_ms_total"),
                    rs.getBigDecimal("wait_ms_total"),
                    rs.getBigDecimal("cpu_ms_total"),
                    rs.getBigDecimal("elapsed_ms_avg"),
                    rs.getLong("executions_total"),
                    rs.getLong("logical_reads_total"),
                    rs.getLong("physical_reads_total")
            );
        }
    };

    @Override
    public List<TopSqlTrendRowDTO> findTopSqlTrend(Long instanceId,
                                                   LocalDateTime startTs,
                                                   LocalDateTime endTs,
                                                   String metric,
                                                   int topN) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("instanceId", instanceId)
                .addValue("startTs", startTs)
                .addValue("endTs", endTs)
                .addValue("metric", metric)
                .addValue("topN", topN);

        return jdbcTemplate.query(TOP_SQL_TREND_QUERY, params, TREND_ROW_MAPPER);
    }

    private static String loadSql(String path) {
        try (InputStream is = Objects.requireNonNull(
                TopSqlRepositoryImpl.class.getClassLoader().getResourceAsStream(path),
                "SQL not found: " + path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load SQL: " + path, e);
        }
    }
}