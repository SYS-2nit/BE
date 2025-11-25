package com.sys.dbmonitor.domains.diagnosis.runners;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;

/**
 * Java 기반 CPU 부하 생성기 - 독립 프로세스로 실행
 * 명령줄 인자: DB_URL DB_USERNAME DB_PASSWORD DURATION_SEC
 */
@Slf4j
public class JavaCpuLoadMain {
    
    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: JavaCpuLoadMain <DB_URL> <DB_USERNAME> <DB_PASSWORD> <DURATION_SEC>");
            System.exit(1);
        }
        
        String dbUrl = args[0];
        String dbUsername = args[1];
        String dbPassword = args[2];
        int durationSec;
        try {
            durationSec = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid duration: " + args[3]);
            System.exit(1);
            return;
        }
        
        log.info("[JavaCpuLoad] 독립 프로세스로 부하 생성 시작 - duration: {}초", durationSec);
        log.info("[JavaCpuLoad] DB URL: {}", dbUrl);
        
        // DataSource 생성
        DataSource dataSource = createDataSource(dbUrl, dbUsername, dbPassword);
        
        // 부하 생성기 생성 및 실행
        JavaCpuLoadGenerator generator = new JavaCpuLoadGenerator(dataSource, durationSec);
        
        try {
            generator.start();
        } catch (Exception e) {
            log.error("[JavaCpuLoad] 부하 생성 중 오류 발생", e);
            System.exit(1);
        } finally {
            generator.stop();
            if (dataSource instanceof HikariDataSource) {
                ((HikariDataSource) dataSource).close();
            }
        }
        
        log.info("[JavaCpuLoad] 독립 프로세스 종료");
    }
    
    private static DataSource createDataSource(String dbUrl, String dbUsername, String dbPassword) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setPoolName("JavaCpuLoadPool");
        // Spring Batch 우선 실행을 위해 부하 생성기 연결 풀 크기 대폭 감소
        config.setMaximumPoolSize(30);  // 200 → 30으로 감소 (배치 작업이 연결을 얻을 수 있도록)
        config.setMinimumIdle(5);       // 50 → 5로 감소
        // 연결 대기 시간을 늘려 배치 작업이 먼저 연결을 얻을 수 있도록
        config.setConnectionTimeout(60000);  // 30초 → 60초로 증가 (배치 작업 우선)
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        config.setValidationTimeout(5000);
        config.setConnectionTestQuery("SELECT 1 FROM DUAL");
        
        return new HikariDataSource(config);
    }
}

