package com.sys.dbmonitor.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 테스트 환경용 데이터소스 및 JPA 설정
 * - H2 인메모리 데이터베이스 사용
 * - Spring Boot 자동 구성을 활용하여 데이터소스 설정
 */
@TestConfiguration
@Profile("test")
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "com.sys.dbmonitor.domains.instance.repository",
                "com.sys.dbmonitor.domains.member.repository"
        }
)
public class TestDataSourceConfig {
    // Spring Boot 자동 구성을 사용하므로 별도 설정 불필요
    // application-test.yml의 datasource 설정을 사용
}

