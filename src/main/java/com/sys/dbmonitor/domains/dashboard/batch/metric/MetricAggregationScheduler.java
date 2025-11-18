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
public class MetricAggregationScheduler {

    private final JobLauncher jobLauncher;
    private final Job metricAggregation10MinutesJob;
    private final Job metricAggregation1HourJob;
    private final Job metricAggregation1DayJob;

    /**
     * 10분 데이터 집계 스케줄러
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void launch10MinutesAggregationJob() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("requestedAt", System.currentTimeMillis())
                .addString("aggregationType", "10m")
                .toJobParameters();
        try {
            jobLauncher.run(metricAggregation10MinutesJob, jobParameters);
            log.debug("[Aggregation] 10분 집계 배치 실행 완료");
        } catch (JobExecutionAlreadyRunningException |
                 JobRestartException |
                 JobInstanceAlreadyCompleteException |
                 JobParametersInvalidException ex) {
            log.warn("[Aggregation] 10분 집계 배치 실행이 진행 중이거나 잘못된 파라미터: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("[Aggregation] 10분 집계 배치 실행 중 알 수 없는 오류가 발생.", ex);
        }
    }

    /**
     * 1시간 데이터 집계 스케줄러
     */
    @Scheduled(cron = "0 0 * * * *")
    public void launch1HourAggregationJob() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("requestedAt", System.currentTimeMillis())
                .addString("aggregationType", "1h")
                .toJobParameters();
        try {
            jobLauncher.run(metricAggregation1HourJob, jobParameters);
            log.debug("[Aggregation] 1시간 집계 배치 실행 요청 완료.");
        } catch (JobExecutionAlreadyRunningException |
                 JobRestartException |
                 JobInstanceAlreadyCompleteException |
                 JobParametersInvalidException ex) {
            log.warn("[Aggregation] 1시간 집계 배치 실행이 진행 중이거나 잘못된 파라미터: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("[Aggregation] 1시간 집계 배치 실행 중 알 수 없는 오류가 발생.", ex);
        }
    }

    /**
     * 1일 데이터 집계 스케줄러
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void launch1DayAggregationJob() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("requestedAt", System.currentTimeMillis())
                .addString("aggregationType", "1d")
                .toJobParameters();
        try {
            jobLauncher.run(metricAggregation1DayJob, jobParameters);
            log.debug("[Aggregation] 1일 집계 배치 실행 요청 완료.");
        } catch (JobExecutionAlreadyRunningException |
                 JobRestartException |
                 JobInstanceAlreadyCompleteException |
                 JobParametersInvalidException ex) {
            log.warn("[Aggregation] 1일 집계 배치 실행이 진행 중이거나 잘못된 파라미터: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("[Aggregation] 1일 집계 배치 실행 중 알 수 없는 오류가 발생.", ex);
        }
    }
}

