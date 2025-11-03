package com.sys.dbmonitor.domains.dashboard.dao;

import com.sys.dbmonitor.domains.dashboard.dto.CollectorRawDTO;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
public class CollectorRepositoryImpl implements CollectorRepository {

    /* ====== 의존성: 트랜잭션 연동 커넥션 제공 ====== */
    private final DataSource dataSource;
    public CollectorRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /* ====== PL/SQL 파일 경로(클래스패스) ====== */
    private static final String PL_SQL_PATH = "sql/collect_plsql_v2.sql";

    /* ====== MANIFEST 제거에 따른 고정 순서 매핑(RESULT SET #2~) ======
       - RESULT SET #1: GRAPH_BUNDLE (INST_ID, METRIC_NAME, VALUE_NUM)
       - RESULT SET #2..: 아래 순서대로 표형 데이터셋으로 간주
       - 실제 PL/SQL 반환 순서가 바뀌면 이 배열도 함께 바꿔야 함(계약 기반)
     ================================================================ */
    private static final List<String> FIXED_TABLE_ORDER = List.of(
            "top_sql_cpu_candidates",
            "top_blocker_sessions",
            "top_sql_shared_pool_candidates",
            "tablespace_capacity_all",
            "bgprocess_status",
            "datafile_io_candidates",
            "segment_top_candidates"
    );

    @Override
    public CollectorRawDTO collectSnapshot() {
        final String plsql = loadClasspathSql(PL_SQL_PATH);
        final CollectorRawDTO out = new CollectorRawDTO();

        try (Connection con = DataSourceUtils.getConnection(dataSource);
             CallableStatement cs = con.prepareCall(plsql)) {

            // 바인드 사용 시: cs.setObject(1, lookbackMin) 등 - 현재 PL/SQL에 기본값 NVL 처리 가정
            cs.execute(); // 반드시 execute()

            int rsIdx = 0;
            boolean sawAnyResultSet = false;

            while (true) {
                boolean hasResult = cs.getMoreResults(); // 가져올 다음 결과셋이 있는지 체크
                int updateCount = cs.getUpdateCount(); // -1이면 더 이상 결과 없음

                if (!hasResult) {
                    if (updateCount == -1) break; // 종료
                    continue; // update count면 스킵
                }

                rsIdx++;
                sawAnyResultSet = true;

                try (ResultSet rs = cs.getResultSet()) {
                    if (rsIdx == 1) {
                        // #1: GRAPH_BUNDLE
                        readGraphBundle(rs, out);
                    } else {
                        // #2..: 고정 순서 표형 데이터셋
                        String dataset = tableNameFor(rsIdx);
                        readTable(rs, out, dataset);
                    }
                }
            }

            if (!sawAnyResultSet) {
                throw new SQLException("No implicit result sets returned by collector.");
            }

            return out;

        } catch (SQLException e) {
            throw new IllegalStateException("Collector execution failed", e);
        }
    }

    /* ===========================
       헬퍼: #1 GRAPH_BUNDLE 읽기
       (INST_ID, METRIC_NAME, VALUE_NUM)
       =========================== */
    private void readGraphBundle(ResultSet rs, CollectorRawDTO out) throws SQLException {
        while (rs.next()) {
            int instId = rs.getInt("INST_ID");
            String metric = rs.getString("METRIC_NAME");
            double val = rs.getDouble("VALUE_NUM");
            if (!rs.wasNull()) {
                out.putBundle(instId, metric, val);
            }
        }
    }

    /* ===========================
       헬퍼: #2~ 표형 결과 읽기(제네릭)
       datasetName 기준으로 rows 적재
       =========================== */
    private void readTable(ResultSet rs, CollectorRawDTO out, String datasetName) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>(cols);
            for (int i = 1; i <= cols; i++) {
                String col = md.getColumnLabel(i);
                row.put(col, rs.getObject(i));
            }
            out.addTableRow(datasetName, row);
        }
    }

    /* rsIdx(1부터 시작) → 데이터셋 이름
       - rsIdx==1: GRAPH_BUNDLE
       - rsIdx>=2: 고정 배열에서 매핑, 범위를 벗어나면 UNKNOWN 접미사 부여
     */
    private String tableNameFor(int rsIdx) {
        int idx = rsIdx - 2; // rsIdx=2 → 0번
        if (idx >= 0 && idx < FIXED_TABLE_ORDER.size()) {
            return FIXED_TABLE_ORDER.get(idx);
        }
        return "UNKNOWN_DATASET_" + rsIdx;
    }

    /* ====== 클래스패스에서 SQL 파일 로드 ====== */
    private String loadClasspathSql(String path) {
        try (InputStream is = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(path),
                "SQL not found: " + path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load SQL: " + path, e);
        }
    }
}
