package com.sys.dbmonitor.domains.dashboard.batch.sql;

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
public class SqlCollectionJobConfig {

    private final JobRepository jobRepository;
    private final SqlCollectionTasklet sqlCollectionTasklet;
    @Qualifier("oracleTransactionManager")
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job sqlCollectionJob() {
        return new JobBuilder("sqlCollectionJob", jobRepository)
                .start(sqlCollectionStep())
                .build();
    }

    @Bean
    public Step sqlCollectionStep() {
        return new StepBuilder("sqlCollectionStep", jobRepository)
                .tasklet(sqlCollectionTasklet, transactionManager)
                .build();
    }
}

