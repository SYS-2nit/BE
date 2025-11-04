package com.sys.dbmonitor;

import com.sys.dbmonitor.domains.dashboard.domain.MetricData;
import com.sys.dbmonitor.domains.dashboard.dto.CollectorRawDTO;
import com.sys.dbmonitor.domains.dashboard.service.CollectorService;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRegistry;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRule;
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

/**
 * 개발(dev) 프로파일에서만 실행되는 수집 테스트 앱.
 * - runOnce()로 계산된 finals(Map<String,Object>)를 출력
 * - GraphRegistry의 모든 그래프를 MetricData 한 행으로 매핑하고
 *   "그래프 필요 컬럼 기준 충족도(present/required)"를 요약 출력
 */
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
        final int runs = 5;        // 총 실행 횟수
        final int intervalSec = 5; // 수집 간격(초)
        final long testDbId = 1001L; // 콘솔 테스트용 DB ID

        return args -> {
            for (int i = 1; i <= runs; i++) {
                Instant start = Instant.now();
                System.out.println("COLLECT START [" + i + "/" + runs + "] " + OffsetDateTime.now());

                // 1회차: 원시 키 5개 샘플 출력(디버그용)
                if (i == 1) {
                    CollectorRawDTO raw = svc.collectRaw();
                    raw.getBundle().forEach((inst, m) ->
                        m.entrySet().stream().limit(5).forEach(e ->
                            System.out.println("RAW@" + inst + " " + e.getKey() + "=" + e.getValue())
                        )
                    );
                    System.out.println("--- RAW sample above (first run only) ---");
                }

                // 최종 계산 실행(Δ/Σ/window_sec 포함) — Map<String,Object>
                Map<String, Object> finals = svc.runOnce();

                // finals 내용 일부 확인
                System.out.println("FINAL metrics size=" + finals.size());
                finals.entrySet().stream().limit(200).forEach(e ->
                    System.out.println(e.getKey() + "=" + e.getValue())
                );

                // ===== 그래프별 "필요 컬럼 충족도" 요약 출력 (INSERT 전 검증용) =====
                for (GraphRule rule : GraphRegistry.all()) {
                    MetricData row = GraphRegistry.mapRow(rule.graphId(), testDbId, finals);

                    int required = rule.columns().size();
                    int present  = countFilledRequiredColumns(rule, finals);

                    System.out.println("[G" + rule.graphId() + " / C" + rule.categoryId() + "] "
                        + rule.name() + " -> columns " + present + "/" + required
                        + " filled, at=" + row.getCollectedAt());
                }
                // =================================================================

                Instant end = Instant.now();
                System.out.println("COLLECT END   [" + i + "/" + runs + "] " + OffsetDateTime.now());
                System.out.println("COLLECT TIME  [" + i + "/" + runs + "] " + Duration.between(start, end).toMillis() + " ms");
                System.out.println();

                // 고정 간격 유지
                if (i < runs) {
                    Instant nextStart = start.plusSeconds(intervalSec);
                    long sleepMs = Duration.between(Instant.now(), nextStart).toMillis();
                    if (sleepMs > 0) {
                        Thread.sleep(sleepMs);
                    }
                }
            }
        };
    }

    /**
     * 그래프가 요구하는 컬럼들 중 finals에 실제 값이 존재하는 컬럼 개수 계산
     * - Storage/IO처럼 아직 수집되지 않은 그래프는 columns 0/N 형태로 표시됨
     */
    private static int countFilledRequiredColumns(GraphRule rule, Map<String, Object> finals) {
        int ok = 0;
        for (String col : rule.columns()) {
            Object v = getFromFinals(finals, col);
            if (v != null) ok++;
        }
        return ok;
    }

    /** finals에서 대소문자 섞임을 허용하여 안전하게 값 조회 */
    private static Object getFromFinals(Map<String, Object> finals, String key) {
        if (finals.containsKey(key)) return finals.get(key);
        String u = key.toUpperCase();
        if (finals.containsKey(u)) return finals.get(u);
        String l = key.toLowerCase();
        if (finals.containsKey(l)) return finals.get(l);
        return null;
    }
}
