package com.sys.dbmonitor;

import com.sys.dbmonitor.domains.dashboard.dto.CollectorRawDTO;
import com.sys.dbmonitor.domains.dashboard.service.CollectorService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.sys.dbmonitor")
@MapperScan(basePackages = "com.sys.dbmonitor", annotationClass = org.apache.ibatis.annotations.Mapper.class)
@EntityScan(basePackages = "com.sys.dbmonitor")
@EnableJpaAuditing
public class DbMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbMonitorApplication.class, args);
    }

    @Bean
    @Profile("dev")
    CommandLineRunner collectOnce(@Qualifier("collectorServiceImpl") CollectorService svc) {
        final int runs = 5;     // 총 실행 횟수
        final int intervalSec = 10; // 수집 간격(초)

        return args -> {
            for (int i = 1; i <= runs; i++) {
                Instant start = Instant.now();
                System.out.println("COLLECT START [" + i + "/" + runs + "] " + OffsetDateTime.now());

                // 1회차에만 원시 키 5개 샘플 확인(디버그용)
                if (i == 1) {
                    CollectorRawDTO raw = svc.collectRaw();
                    raw.getBundle().forEach((inst, m) ->
                            m.entrySet().stream().limit(5).forEach(e ->
                                    System.out.println("RAW@" + inst + " " + e.getKey() + "=" + e.getValue())
                            )
                    );
                    System.out.println("--- RAW sample above (first run only) ---");
                }

                // 최종 계산 실행(Δ/Σ/window_sec 포함) — 이제 클러스터 합계만 반환(접미사 없음)
                Map<String, Object> finals = svc.runOnce();

                System.out.println("FINAL metrics size=" + finals.size());
                finals.entrySet().stream().limit(210).forEach(e ->
                        System.out.println(e.getKey() + "=" + e.getValue())
                );

                Instant end = Instant.now();
                System.out.println("COLLECT END   [" + i + "/" + runs + "] " + OffsetDateTime.now());
                System.out.println("COLLECT TIME  [" + i + "/" + runs + "] " + Duration.between(start, end).toMillis() + " ms");
                System.out.println();

                // 고정 간격 유지
                if (i < runs) {
                    Instant nextStart = start.plusSeconds(intervalSec);
                    long sleepMs = Duration.between(Instant.now(), nextStart).toMillis();
                    if (sleepMs > 0) Thread.sleep(sleepMs);
                }
            }
        };
    }
}
