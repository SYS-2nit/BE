package com.sys.dbmonitor.domains.dashboard.batch.metric;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class MetricAggregationJobConfig {

    private final JobRepository jobRepository;
    private final MetricAggregationTasklet metricAggregationTasklet;
    @Qualifier("oracleTransactionManager")
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job metricAggregation10MinutesJob() {
        return new JobBuilder("metricAggregation10MinutesJob", jobRepository)
                .start(metricAggregation10MinutesStep())
                .build();
    }

    @Bean
    public Job metricAggregation1HourJob() {
        return new JobBuilder("metricAggregation1HourJob", jobRepository)
                .start(metricAggregation1HourStep())
                .build();
    }

    @Bean
    public Job metricAggregation1DayJob() {
        return new JobBuilder("metricAggregation1DayJob", jobRepository)
                .start(metricAggregation1DayStep())
                .build();
    }

    @Bean
    public Step metricAggregation10MinutesStep() {
        return new StepBuilder("metricAggregation10MinutesStep", jobRepository)
                .tasklet(metricAggregationTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step metricAggregation1HourStep() {
        return new StepBuilder("metricAggregation1HourStep", jobRepository)
                .tasklet(metricAggregationTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step metricAggregation1DayStep() {
        return new StepBuilder("metricAggregation1DayStep", jobRepository)
                .tasklet(metricAggregationTasklet, transactionManager)
                .build();
    }
}

