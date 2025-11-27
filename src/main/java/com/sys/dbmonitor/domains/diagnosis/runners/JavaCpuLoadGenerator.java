/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.diagnosis.runners;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Java 기반 CPU 부하 생성기
 * Oracle DB에 CPU 집약적인 SQL을 여러 스레드에서 실행하여 부하 생성
 * 목표: DB_OF_HOST_SHARE_PCT와 CPU_SATURATION_PCT 증가
 */
@Slf4j
public class JavaCpuLoadGenerator {
    
    private final DataSource dataSource;
    private final int durationSec;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ExecutorService executorService;
    
    // CPU 코어 수 확인 (시스템에서 가져오거나 기본값 사용)
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();
    // 동시 실행 스레드 수: 코어 수의 5배 (Spring Batch 우선 실행을 위해 감소)
    private static final int CONCURRENT_THREADS = Math.max(CPU_CORES * 5, 10);
    
    public JavaCpuLoadGenerator(DataSource dataSource, int durationSec) {
        this.dataSource = dataSource;
        this.durationSec = durationSec;
        this.executorService = Executors.newFixedThreadPool(CONCURRENT_THREADS);
    }
    
    /**
     * CPU 부하 생성 시작
     */
    public void start() {
        log.info("[JavaCpuLoad] 부하 생성 시작 - duration: {}초, 동시 스레드: {}", durationSec, CONCURRENT_THREADS);
        log.info("[JavaCpuLoad] 목표: DB_OF_HOST_SHARE_PCT > 70%, CPU_SATURATION_PCT > 15%");
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSec * 1000L);
        
        // 여러 스레드에서 CPU 집약적인 SQL 실행
        List<Future<?>> futures = new ArrayList<>();
        
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadId = i + 1;
            Future<?> future = executorService.submit(() -> {
                try {
                    generateCpuLoad(threadId, endTime);
                } catch (Exception e) {
                    log.error("[JavaCpuLoad] 스레드 {} 실행 중 오류: {}", threadId, e.getMessage(), e);
                }
            });
            futures.add(future);
        }
        
        // 모든 스레드가 종료될 때까지 대기
        try {
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("[JavaCpuLoad] 부하 생성 중 오류: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("[JavaCpuLoad] 부하 생성 완료 - duration: {}초", durationSec);
    }
    
    /**
     * CPU 집약적인 부하 생성 (개별 스레드)
     */
    private void generateCpuLoad(int threadId, long endTime) {
        String threadName = "CpuLoad-" + threadId;
        Thread.currentThread().setName(threadName);
        
        // 부하 생성 스레드는 낮은 우선순위로 설정하여 배치 작업이 우선 실행되도록
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY + 1);
        
        log.debug("[JavaCpuLoad] 스레드 {} 시작 (우선순위: 낮음)", threadId);
        
        int iteration = 0;
        while (running.get() && System.currentTimeMillis() < endTime) {
            iteration++;
            
            try {
                // CPU 집약적인 SQL 쿼리 실행 (Spring Batch 우선 실행을 위해 빈도 감소)
                // 1. 복잡한 계산 쿼리 (반복 횟수 감소) - 각 쿼리마다 Connection 열고 닫기
                for (int j = 0; j < 2 && System.currentTimeMillis() < endTime; j++) {
                    try (Connection conn = dataSource.getConnection()) {
                        executeCpuIntensiveQuery(conn, iteration * 100 + j);
                    }
                }
                
                // 2. 추가 부하를 위한 반복 쿼리 (반복 횟수 감소) - 각 쿼리마다 Connection 열고 닫기
                for (int i = 0; i < 8 && System.currentTimeMillis() < endTime; i++) {
                    try (Connection conn = dataSource.getConnection()) {
                        executeCpuIntensiveQuery(conn, iteration * 1000 + i);
                    }
                }
                
                // 쿼리 실행 간 대기 시간 추가 (배치 작업이 연결을 얻을 수 있는 여유 공간 확보)
                Thread.sleep(100);  // 50ms → 100ms로 증가
                
            } catch (Exception e) {
                log.warn("[JavaCpuLoad] 스레드 {} 쿼리 실행 중 오류 (무시하고 계속): {}", threadId, e.getMessage());
                // 오류가 발생해도 계속 실행하여 부하 유지
                try {
                    Thread.sleep(300); // 오류 시 더 긴 대기 후 재시도
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.debug("[JavaCpuLoad] 스레드 {} 종료 - 반복 횟수: {}", threadId, iteration);
    }
    
    /**
     * CPU 집약적인 SQL 쿼리 실행
     * Oracle에서 CPU를 많이 사용하는 복잡한 계산 쿼리
     */
    private void executeCpuIntensiveQuery(Connection conn, int iteration) throws Exception {
        // 방법 1: 복잡한 수학 계산 (ROWNUM을 이용한 반복 계산) - 범위 감소
        String sql1 = """
            SELECT SUM(ROWNUM * ROWNUM * ROWNUM * ROWNUM) 
            FROM (
                SELECT ROWNUM 
                FROM DUAL 
                CONNECT BY ROWNUM <= 10000
            )
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql1);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                rs.getBigDecimal(1);
            }
        }
        
        // 방법 2: 복잡한 CONNECT BY 계산 (Oracle 전용) - 범위 감소
        String sql2 = """
            SELECT SUM(ROWNUM * POWER(ROWNUM, 3) * MOD(ROWNUM, 50) * SIN(ROWNUM))
            FROM (
                SELECT ROWNUM 
                FROM DUAL 
                CONNECT BY ROWNUM <= 8000
            )
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql2);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                rs.getBigDecimal(1);
            }
        } catch (Exception e) {
            // SIN 함수가 없을 수 있으므로 대체 쿼리 사용
            String sql2Alt = """
                SELECT SUM(ROWNUM * POWER(ROWNUM, 3) * MOD(ROWNUM, 50) * (ROWNUM * 2))
                FROM (
                    SELECT ROWNUM 
                    FROM DUAL 
                    CONNECT BY ROWNUM <= 8000
                )
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql2Alt);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    rs.getBigDecimal(1);
                }
            }
        }
        
        // 방법 3: 복잡한 계산 - 범위 감소
        String sql3 = """
            SELECT 
                SUM(ROWNUM * MOD(ROWNUM, 100) * POWER(ROWNUM, 3) * SQRT(ROWNUM))
            FROM (
                SELECT ROWNUM 
                FROM DUAL 
                CONNECT BY ROWNUM <= 6000 
            )
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql3);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                rs.getBigDecimal(1);
            }
        } catch (Exception e) {
            // SQRT 함수가 없을 수 있으므로 대체 쿼리 사용
            String sql3Alt = """
                SELECT 
                    SUM(ROWNUM * MOD(ROWNUM, 100) * POWER(ROWNUM, 3) * (ROWNUM / 2))
                FROM (
                    SELECT ROWNUM 
                    FROM DUAL 
                    CONNECT BY ROWNUM <= 6000
                )
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql3Alt);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    rs.getBigDecimal(1);
                }
            }
        }
        
        // 방법 4: 중첩된 복잡한 계산 (범위 감소)
        String sql4 = """
            SELECT 
                SUM(ROWNUM * POWER(ROWNUM, 2) * MOD(ROWNUM, 200) * 
                    (SELECT COUNT(*) FROM DUAL CONNECT BY ROWNUM <= 50))
            FROM (
                SELECT ROWNUM 
                FROM DUAL 
                CONNECT BY ROWNUM <= 5000
            )
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql4);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                rs.getBigDecimal(1);
            }
        }
    }
    
    /**
     * 부하 생성 중지
     */
    public void stop() {
        running.set(false);
        executorService.shutdownNow();
        log.info("[JavaCpuLoad] 부하 생성 중지 요청");
    }
}

