package com.sys.dbmonitor.domains.dashboard.batch.metric;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricCollectionScheduler {

    private final JobLauncher jobLauncher;
    private final Job metricCollectionJob;
    
    @Qualifier("batchExecutor")
    private final ThreadPoolTaskExecutor batchExecutor;

    @Scheduled(cron = "0 * * * * *")
    public void launchMetricCollectionJob() {
        // 배치 작업을 전용 스레드 풀에서 실행 (최고 우선순위)
        batchExecutor.execute(() -> {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("requestedAt", System.currentTimeMillis())
                    .toJobParameters();
            
            // Spring Batch 우선 실행을 위한 재시도 로직
            int maxRetries = 3;
            int retryDelayMs = 2000; // 2초 대기
            boolean success = false;
            
            for (int attempt = 1; attempt <= maxRetries && !success; attempt++) {
                try {
                    jobLauncher.run(metricCollectionJob, jobParameters);
                    log.info("[MetricBatch] 배치 실행 완료 (시도: {}/{})", attempt, maxRetries);
                    success = true;
                } catch (JobExecutionAlreadyRunningException |
                         JobRestartException |
                         JobInstanceAlreadyCompleteException |
                         JobParametersInvalidException ex) {
                    log.warn("[MetricBatch] 배치 실행이 진행 중이거나 잘못된 파라미터입니다: {}", ex.getMessage());
                    success = true; // 재시도 불필요
                } catch (Exception ex) {
                    if (attempt < maxRetries) {
                        log.warn("[MetricBatch] 배치 실행 실패 (시도: {}/{}), {}ms 후 재시도: {}", 
                                attempt, maxRetries, retryDelayMs, ex.getMessage());
                        try {
                            Thread.sleep(retryDelayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.error("[MetricBatch] 재시도 대기 중 인터럽트 발생");
                            break;
                        }
                    } else {
                        log.error("[MetricBatch] 배치 실행 최종 실패 (시도: {}/{}): {}", 
                                attempt, maxRetries, ex.getMessage(), ex);
                    }
                }
            }
        });
    }
}

