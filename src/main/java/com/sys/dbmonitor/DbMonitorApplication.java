package com.sys.dbmonitor;

import com.sys.dbmonitor.domains.dashboard.domain.MetricData;
import com.sys.dbmonitor.domains.dashboard.dto.CollectorRawDTO;
import com.sys.dbmonitor.domains.dashboard.service.CollectorService;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRegistry;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 개발(dev) 프로파일에서만 실행되는 수집 테스트 앱.
 * - runOnce()로 계산된 finals(Map<String,Object>)를 출력
 * - 그래프별 "필요 컬럼 충족도" + 어떤 컬럼이 채워졌는지/비었는지까지 함께 출력
 */
@SpringBootApplication
@EntityScan(basePackages = "com.sys.dbmonitor")
@EnableJpaAuditing  // JPA Auditing 기능 활성화 (@CreatedDate, @LastModifiedDate 등 사용)
public class DbMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbMonitorApplication.class, args);
    }

}
