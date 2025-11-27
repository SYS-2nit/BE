/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 스레드 풀 분리 설정
 * - 배치 작업용 스레드 풀 (높은 우선순위)
 * - 진단 작업용 스레드 풀 (낮은 우선순위)
 * - API 요청은 기본 Tomcat 스레드 풀 사용
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    @Value("${app.thread-pool.batch.core-size:5}")
    private int batchCoreSize;

    @Value("${app.thread-pool.batch.max-size:10}")
    private int batchMaxSize;

    @Value("${app.thread-pool.diagnosis.core-size:2}")
    private int diagnosisCoreSize;

    @Value("${app.thread-pool.diagnosis.max-size:5}")
    private int diagnosisMaxSize;

    /**
     * 배치 작업용 스레드 풀
     * - 최고 우선순위로 설정하여 배치 작업이 최우선 실행되도록 함
     * - 메트릭 수집, SQL 수집, 집계 작업 등에 사용
     */
    @Bean(name = "batchExecutor")
    public ThreadPoolTaskExecutor batchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(batchCoreSize);
        executor.setMaxPoolSize(batchMaxSize);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("batch-");
        executor.setThreadPriority(Thread.MAX_PRIORITY - 1); // 최고 우선순위 (MAX_PRIORITY는 시스템용)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("[ThreadPool] 배치 작업용 스레드 풀 생성 - core: {}, max: {}, queue: 100, priority: 최고", 
                batchCoreSize, batchMaxSize);
        return executor;
    }

    /**
     * 진단 작업용 스레드 풀 (Java 기반 진단만)
     * - 낮은 우선순위로 설정하여 다른 작업에 영향을 최소화
     * - Java 기반 진단 시나리오에만 사용 (SwingBench는 별도 프로세스)
     */
    @Bean(name = "diagnosisExecutor")
    public ThreadPoolTaskExecutor diagnosisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(diagnosisCoreSize);
        executor.setMaxPoolSize(diagnosisMaxSize);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("diagnosis-");
        executor.setThreadPriority(Thread.MIN_PRIORITY + 1); // 낮은 우선순위
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        
        log.info("[ThreadPool] 진단 작업용 스레드 풀 생성 - core: {}, max: {}, queue: 50", 
                diagnosisCoreSize, diagnosisMaxSize);
        return executor;
    }
}

