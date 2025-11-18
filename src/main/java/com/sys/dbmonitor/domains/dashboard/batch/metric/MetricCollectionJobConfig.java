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
public class MetricCollectionJobConfig {

    private final JobRepository jobRepository;
    private final MetricCollectionTasklet metricCollectionTasklet;
    @Qualifier("oracleTransactionManager")
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job metricCollectionJob() {
        return new JobBuilder("metricCollectionJob", jobRepository)
                .start(metricCollectionStep())
                .build();
    }

    @Bean
    public Step metricCollectionStep() {
        return new StepBuilder("metricCollectionStep", jobRepository)
                .tasklet(metricCollectionTasklet, transactionManager)
                .build();
    }
}

