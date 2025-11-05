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

                // ===== 그래프별 "필요 컬럼 충족도 + 컬럼 목록" 요약 출력 =====
                for (GraphRule rule : GraphRegistry.all()) {
                    // 매핑 자체는 동일 (행을 만들어 collectedAt만 활용)
                    MetricData row = GraphRegistry.mapRow(rule.graphId(), testDbId, finals);

                    ColSummary sum = summarizeColumns(rule, finals);
                    int required = sum.present.size() + sum.missing.size();

                    System.out.println("[G" + rule.graphId() + " / C" + rule.categoryId() + "] "
                        + rule.name()
                        + " -> columns " + sum.present.size() + "/" + required + " filled"
                        + ", present=" + sum.present
                        + ", missing=" + sum.missing
                        + ", at=" + row.getCollectedAt()
                        + " test:"+row.getAasTotal()
                    );
                }
                // ==========================================================

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

    /** 그래프가 요구하는 컬럼들에 대해 finals에 값이 있는지/없는지 분류 */
    private static ColSummary summarizeColumns(GraphRule rule, Map<String, Object> finals) {
        List<String> present = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String col : rule.columns()) {
            Object v = getFromFinals(finals, col);
            if (v != null) present.add(col);
            else missing.add(col);
        }
        return new ColSummary(present, missing);
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

    /** present/missing 리스트 보관용 단순 DTO */
    private static class ColSummary {
        private final List<String> present;
        private final List<String> missing;
        private ColSummary(List<String> present, List<String> missing) {
            this.present = present;
            this.missing = missing;
        }
        public List<String> present() { return present; }
        public List<String> missing() { return missing; }
        @Override public String toString() { return "present=" + present + ", missing=" + missing; }
    }
}
