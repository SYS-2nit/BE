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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricCollectionScheduler {

    private final JobLauncher jobLauncher;
    private final Job metricCollectionJob;

    @Scheduled(cron = "0 * * * * *")
    public void launchMetricCollectionJob() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("requestedAt", System.currentTimeMillis())
                .toJobParameters();
        try {
            jobLauncher.run(metricCollectionJob, jobParameters);
            log.debug("[MetricBatch] 배치 실행 요청 완료.");
        } catch (JobExecutionAlreadyRunningException |
                 JobRestartException |
                 JobInstanceAlreadyCompleteException |
                 JobParametersInvalidException ex) {
            log.warn("[MetricBatch] 배치 실행이 진행 중이거나 잘못된 파라미터입니다: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("[MetricBatch] 배치 실행 중 알 수 없는 오류가 발생했습니다.", ex);
        }
    }
}

