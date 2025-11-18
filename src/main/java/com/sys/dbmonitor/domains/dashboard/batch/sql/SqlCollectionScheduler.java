package com.sys.dbmonitor.domains.dashboard.batch.sql;

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
@ConditionalOnProperty(prefix = "app.batch", name = "enabled", havingValue = "true")
public class SqlCollectionScheduler {

    private final JobLauncher jobLauncher;
    private final Job sqlCollectionJob;

    @Scheduled(cron = "0 */30 * * * *")
    public void launchSqlCollectionJob() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("requestedAt", System.currentTimeMillis())
                .toJobParameters();
        try {
            jobLauncher.run(sqlCollectionJob, jobParameters);
            log.debug("[sqlBatch] 배치 실행 요청 완료.");
        } catch (JobExecutionAlreadyRunningException |
                 JobRestartException |
                 JobInstanceAlreadyCompleteException |
                 JobParametersInvalidException ex) {
            log.warn("[sqlBatch] 배치 실행이 진행 중이거나 잘못된 파라미터입니다: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("[sqlBatch] 배치 실행 중 알 수 없는 오류가 발생했습니다.", ex);
        }
    }
}

