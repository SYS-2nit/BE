package com.sys.dbmonitor.global.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 동적 Oracle 타겟 데이터소스 팩토리
 * 런타임에 타겟 DB를 등록/제거하고 데이터소스를 동적으로 관리
 */
@Component
public class DynamicDataSourceFactory {

    private static final Logger log = LoggerFactory.getLogger(DynamicDataSourceFactory.class);
    private final Map<Long, HikariDataSource> dataSourceMap = new ConcurrentHashMap<>();

    /**
     * 타겟 DB 데이터소스 생성
     */
    public DataSource createDataSource(Long id, String name, String url, String username, String password) {
        // 기존 데이터소스가 있으면 제거
        removeDataSource(id);

        log.info("[DynamicDataSource] 타겟 DB 데이터소스 생성 시작: id={}, name={}, url={}", id, name, url);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setPoolName("OracleTargetPool-" + id);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        config.setValidationTimeout(5000);
        config.setConnectionTestQuery("SELECT 1 FROM DUAL");

        HikariDataSource dataSource = new HikariDataSource(config);

        // 연결 테스트
        try (Connection conn = dataSource.getConnection()) {
            log.info("[DynamicDataSource] 타겟 DB 연결 성공: id={}, name={}", id, name);
        } catch (SQLException e) {
            // 상세한 에러 정보 로깅
            String errorMsg = String.format("Error Code: %d, SQL State: %s, Message: %s",
                    e.getErrorCode(),
                    e.getSQLState() != null ? e.getSQLState() : "N/A",
                    e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            log.error("[DynamicDataSource] 타겟 DB 연결 실패: id={}, name={}, error={}", 
                    id, name, errorMsg, e);
            dataSource.close();
            
            // 에러 메시지가 null이면 상세 정보 포함
            String finalMessage = e.getMessage() != null && !e.getMessage().isEmpty()
                    ? e.getMessage()
                    : String.format("SQL Error Code: %d, SQL State: %s", e.getErrorCode(), e.getSQLState());
            throw new RuntimeException("타겟 DB 연결 실패: " + finalMessage, e);
        }

        dataSourceMap.put(id, dataSource);
        log.info("[DynamicDataSource] 타겟 DB 데이터소스 등록 완료: id={}, name={}", id, name);

        return dataSource;
    }

    /**
     * 타겟 DB 데이터소스 조회
     *
     * @param id 타겟 DB ID
     * @return DataSource (없으면 null)
     */
    public DataSource getDataSource(Long id) {
        return dataSourceMap.get(id);
    }

    /**
     * 타겟 DB 데이터소스 제거
     *
     * @param id 타겟 DB ID
     */
    public void removeDataSource(Long id) {
        if (id == null) {
            log.warn("[DynamicDataSource] ID가 null이므로 데이터소스 제거 불가");
            return;
        }
        
        HikariDataSource dataSource = dataSourceMap.remove(id);
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("[DynamicDataSource] 타겟 DB 데이터소스 제거: id={}", id);
            dataSource.close();
        }
    }

    /**
     * 모든 타겟 DB 데이터소스 제거
     */
    public void removeAllDataSources() {
        log.info("[DynamicDataSource] 모든 타겟 DB 데이터소스 제거 시작: count={}", dataSourceMap.size());
        dataSourceMap.forEach((id, dataSource) -> {
            if (!dataSource.isClosed()) {
                dataSource.close();
            }
        });
        dataSourceMap.clear();
        log.info("[DynamicDataSource] 모든 타겟 DB 데이터소스 제거 완료");
    }

    /**
     * 타겟 DB 데이터소스 존재 여부 확인
     *
     * @param id 타겟 DB ID
     * @return 존재 여부
     */
    public boolean containsDataSource(Long id) {
        return dataSourceMap.containsKey(id);
    }

    /**
     * 등록된 타겟 DB 개수
     *
     * @return 개수
     */
    public int getDataSourceCount() {
        return dataSourceMap.size();
    }
}

