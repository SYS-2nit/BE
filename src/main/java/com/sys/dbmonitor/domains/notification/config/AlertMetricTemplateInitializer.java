package com.sys.dbmonitor.domains.notification.config;

import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.domains.graph.repository.GraphRepository;
import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.AlertMetricTemplate;
import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import com.sys.dbmonitor.domains.notification.repository.AlertMetricTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 알림 메트릭 템플릿 초기 데이터 삽입
 * plan.md의 "6. 카테고리별 메트릭 선정" 섹션에 정의된 모든 메트릭을 ALERT_METRIC_TEMPLATE 테이블에 삽입합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertMetricTemplateInitializer {

    private final AlertMetricTemplateRepository templateRepository;
    private final GraphRepository graphRepository;

    /**
     * 애플리케이션 시작 완료 후 메트릭 템플릿 초기 데이터 삽입
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeMetricTemplates() {
        log.info("[AlertMetricTemplateInitializer] 메트릭 템플릿 초기 데이터 삽입 시작");

        // plan.md의 "6. 카테고리별 메트릭 선정" 섹션에 정의된 모든 메트릭 데이터
        List<MetricTemplateData> templates = List.of(
            // ===== CPU (카테고리 2) - 5개 메트릭 =====
            new MetricTemplateData(AlertCategory.CPU, 15L, "HOST_CPU_UTIL_PCT",
                "Host CPU 사용률", ThresholdFormat.PERCENT, 70.0, 85.0, 95.0, "호스트 전체 CPU 사용률"),
            new MetricTemplateData(AlertCategory.CPU, 14L, "CPU_SATURATION_PCT",
                "DB CPU 포화도", ThresholdFormat.PERCENT, 80.0, 90.0, 95.0, "DB CPU 포화도"),
            new MetricTemplateData(AlertCategory.CPU, 16L, "DB_OF_HOST_SHARE_PCT",
                "DB CPU 비율", ThresholdFormat.PERCENT, 60.0, 75.0, 90.0, "DB CPU Share of Host (%)"),
            new MetricTemplateData(AlertCategory.CPU, 16L, "OTHER_PROCESSES_PCT",
                "기타 프로세스 CPU 비율", ThresholdFormat.PERCENT, 30.0, 50.0, 70.0, "기타 프로세스 CPU 비율 (Graph 16과 공유)"),
            new MetricTemplateData(AlertCategory.CPU, 14L, "AAS_ONCPU_SESSIONS",
                "On-CPU 세션 수", ThresholdFormat.PERCENT, 70.0, 85.0, 95.0, "AAS_ONCPU_SESSIONS / CORE_BASELINE_SESSIONS * 100 (Graph 14와 공유)"),

            // ===== CPU 추가 (5개) =====
            new MetricTemplateData(AlertCategory.CPU, 17L, "RunQ_per_Core_LOAD_PROXY",
                "Run Queue per Core", ThresholdFormat.PERCENT, 70.0, 85.0, 95.0, "스케줄러 큐 적체 비율 (높을수록 문제)"),
            new MetricTemplateData(AlertCategory.CPU, 18L, "CPU_per_Commit_ms",
                "커밋당 CPU 시간", ThresholdFormat.MS, 5.0, 10.0, 20.0, "Commit 당 CPU 시간 (ms)"),
            new MetricTemplateData(AlertCategory.CPU, 18L, "CPU_per_Exec_ms",
                "실행당 CPU 시간", ThresholdFormat.MS, 10.0, 20.0, 50.0, "SQL 실행당 CPU 시간 (ms)"),
            new MetricTemplateData(AlertCategory.CPU, 32L, "TPS_PER_SEC",
                "초당 트랜잭션 수", ThresholdFormat.COUNT, 1000.0, 2000.0, 3000.0, "TPS — Trend"),
            new MetricTemplateData(AlertCategory.CPU, 33L, "EXECS_PER_SEC",
                "초당 실행 수", ThresholdFormat.COUNT, 5000.0, 10000.0, 15000.0, "Exec/s — Trend"),

            // ===== Memory (카테고리 3) - 5개 메트릭 =====
            new MetricTemplateData(AlertCategory.MEMORY, 23L, "PGA_UTIL_PCT",
                "PGA 사용률", ThresholdFormat.PERCENT, 70.0, 85.0, 95.0, "PGA Utilization (%)"),
            new MetricTemplateData(AlertCategory.MEMORY, 24L, "SGA_UTIL_PCT",
                "SGA 사용률", ThresholdFormat.PERCENT, 70.0, 85.0, 95.0, "SGA Utilization (%)"),
            new MetricTemplateData(AlertCategory.MEMORY, 25L, "WORKAREA_SPILL_RATE_PCT",
                "Workarea Spill 비율", ThresholdFormat.PERCENT, 10.0, 20.0, 30.0, "Workarea Spill Rate (%)"),
            new MetricTemplateData(AlertCategory.MEMORY, 27L, "BUFFER_MISS_PCT",
                "Buffer Cache Miss 비율", ThresholdFormat.PERCENT, 10.0, 20.0, 30.0, "Buffer Cache Miss Rate (%)"),
            new MetricTemplateData(AlertCategory.MEMORY, 26L, "LIBRARY_CACHE_RELOADS_PER_SEC",
                "라이브러리 캐시 재적재율", ThresholdFormat.COUNT, 5.0, 10.0, 20.0, "Library Cache Reloads per Second"),

            // ===== Memory 추가 (5개) =====
            new MetricTemplateData(AlertCategory.MEMORY, 22L, "LIBRARY_CACHE_HIT_PCT",
                "Library Cache Hit 비율 (역방향)", ThresholdFormat.PERCENT, 10.0, 15.0, 20.0, "100 - value로 역방향 처리"),
            new MetricTemplateData(AlertCategory.MEMORY, 22L, "DICTIONARY_CACHE_HIT_PCT",
                "Dictionary Cache Hit 비율 (역방향)", ThresholdFormat.PERCENT, 10.0, 15.0, 20.0, "100 - value로 역방향 처리"),
            new MetricTemplateData(AlertCategory.MEMORY, 22L, "LATCH_HIT_PCT",
                "Latch Hit 비율 (역방향)", ThresholdFormat.PERCENT, 5.0, 10.0, 15.0, "100 - value로 역방향 처리"),
            new MetricTemplateData(AlertCategory.MEMORY, 21L, "MEMORY_SORT_PCT",
                "메모리 정렬 비율", ThresholdFormat.PERCENT, 60.0, 75.0, 90.0, "메모리 정렬 비율"),
            new MetricTemplateData(AlertCategory.MEMORY, 22L, "REDO_BUFFER_WAIT_PCT",
                "Redo Buffer Wait 비율", ThresholdFormat.PERCENT, 1.0, 2.0, 5.0, "Redo Buffer Wait (%)"),

            // ===== Session (카테고리 4) - 5개 메트릭 =====
            new MetricTemplateData(AlertCategory.SESSION, 35L, "ACTIVE_USER_RATIO_PCT",
                "활성 사용자 비율", ThresholdFormat.PERCENT, 80.0, 90.0, 95.0, "Active User Ratio (%)"),
            new MetricTemplateData(AlertCategory.SESSION, 35L, "SESSIONS_LIMIT_UTIL_PCT",
                "세션 한도 사용률", ThresholdFormat.PERCENT, 70.0, 85.0, 95.0, "Sessions Limit Utilization (%) (Graph 35과 공유)"),
            new MetricTemplateData(AlertCategory.SESSION, 35L, "PROCESSES_LIMIT_UTIL_PCT",
                "프로세스 한도 사용률", ThresholdFormat.PERCENT, 70.0, 85.0, 95.0, "Processes Limit Utilization (%) (Graph 35과 공유)"),
            new MetricTemplateData(AlertCategory.SESSION, 34L, "LOGONS_PER_SEC",
                "초당 로그인 수", ThresholdFormat.COUNT, 5.0, 10.0, 15.0, "Logons per Second"),
            new MetricTemplateData(AlertCategory.SESSION, 35L, "BLOCKERS_NOW",
                "현재 블로커 세션 수", ThresholdFormat.COUNT, 1.0, 3.0, 5.0, "현재 블로킹 세션 수"),

            // ===== Session 추가 (4개) =====
            new MetricTemplateData(AlertCategory.SESSION, 35L, "ACTIVE_USER_SESSIONS_NOW",
                "활성 세션 수", ThresholdFormat.COUNT, 20.0, 40.0, 80.0, "활성 사용자 세션 수"),
            new MetricTemplateData(AlertCategory.SESSION, 35L, "TOTAL_USER_SESSIONS_NOW",
                "총 세션 수", ThresholdFormat.COUNT, 50.0, 100.0, 150.0, "총 사용자 세션 수"),
            new MetricTemplateData(AlertCategory.SESSION, 35L, "USER_CALLS_PER_SEC",
                "초당 사용자 호출 수", ThresholdFormat.COUNT, 1000.0, 3000.0, 6000.0, "User Calls per Second"),
            new MetricTemplateData(AlertCategory.SESSION, 31L, "lock_wait_total",
                "총 락 대기 세션 수", ThresholdFormat.COUNT, 1.0, 3.0, 5.0, "락 대기 세션 수 합계"),

            // ===== I/O (카테고리 5) - 5개 메트릭 =====
            new MetricTemplateData(AlertCategory.IO, 38L, "direct_io_ratio_pct",
                "Direct I/O 비율", ThresholdFormat.PERCENT, 20.0, 30.0, 40.0, "Direct Path I/O Ratio (%)"),
            new MetricTemplateData(AlertCategory.IO, 37L, "parse_execute_ratio",
                "Parse/Execute 비율", ThresholdFormat.PERCENT, 50.0, 70.0, 90.0, "Parse/Execute Ratio (0~1 범위이므로 * 100으로 변환하여 0~100%로 처리)"),
            new MetricTemplateData(AlertCategory.IO, 37L, "cache_hit_ratio_pct",
                "캐시 히트 비율 (역방향)", ThresholdFormat.PERCENT, 10.0, 15.0, 20.0, "Cache Hit Ratio (%) - 역방향 (100 - cache_hit_ratio_pct로 변환하여 높을수록 문제로 처리)"),
            new MetricTemplateData(AlertCategory.IO, 37L, "avg_io_wait_time_ms",
                "평균 I/O 대기 시간", ThresholdFormat.MS, 20.0, 35.0, 50.0, "Average I/O Wait Time (ms)"),
            new MetricTemplateData(AlertCategory.IO, 42L, "redo_generation_mbps",
                "Redo 생성량", ThresholdFormat.MBPS, 80.0, 120.0, 160.0, "Redo Generation Rate (MB/s)"),

            // ===== I/O 추가 (6개) =====
            new MetricTemplateData(AlertCategory.IO, 41L, "avg_wait_time_ms",
                "평균 I/O 대기 시간 (상세)", ThresholdFormat.MS, 20.0, 35.0, 50.0, "Average I/O Wait Time (ms) 상세"),
            new MetricTemplateData(AlertCategory.IO, 41L, "p95_wait_time_ms",
                "95퍼센타일 I/O 대기 시간", ThresholdFormat.MS, 40.0, 60.0, 80.0, "95th percentile I/O wait time (ms)"),
            new MetricTemplateData(AlertCategory.IO, 41L, "io_waits_per_sec",
                "초당 I/O 대기 횟수", ThresholdFormat.COUNT, 200.0, 400.0, 800.0, "I/O waits per second"),
            new MetricTemplateData(AlertCategory.IO, 38L, "physical_reads_direct_per_sec",
                "초당 Direct Read 횟수", ThresholdFormat.COUNT, 50.0, 100.0, 200.0, "Direct Reads per second"),
            new MetricTemplateData(AlertCategory.IO, 40L, "total_reads_per_sec",
                "초당 총 읽기 횟수", ThresholdFormat.COUNT, 500.0, 1000.0, 2000.0, "Total reads per second"),
            new MetricTemplateData(AlertCategory.IO, 40L, "cache_hit_ratio_diff_pct",
                "캐시 히트 비율 차이 (역방향)", ThresholdFormat.PERCENT, 10.0, 15.0, 20.0, "100 - value로 역방향 처리"),

            // ===== Storage (카테고리 6) - 5개 메트릭 =====
            new MetricTemplateData(AlertCategory.STORAGE, 51L, "total_db_usage_percent",
                "전체 DB 사용률", ThresholdFormat.PERCENT, 70.0, 85.0, 95.0, "Total Database Usage (%)"),
            new MetricTemplateData(AlertCategory.STORAGE, 49L, "usage_pct",
                "FRA 사용률", ThresholdFormat.PERCENT, 70.0, 85.0, 95.0, "FRA Usage (%) - Graph 49의 usage_pct 필드 사용"),
            new MetricTemplateData(AlertCategory.STORAGE, 50L, "undo_usage_percent",
                "Undo 사용률", ThresholdFormat.PERCENT, 70.0, 85.0, 95.0, "Undo Usage (%)"),
            new MetricTemplateData(AlertCategory.STORAGE, 46L, "temp_usage_percent",
                "Temp 사용률", ThresholdFormat.PERCENT, 70.0, 85.0, 95.0, "Temp Tablespace Usage (%)"),
            new MetricTemplateData(AlertCategory.STORAGE, 45L, "MAX_TS_USAGE_PCT",
                "최대 테이블스페이스 사용률", ThresholdFormat.PERCENT, 80.0, 90.0, 95.0, "Maximum Tablespace Usage (%)"),

            // ===== Storage 추가 (3개) =====
            new MetricTemplateData(AlertCategory.STORAGE, 46L, "temp_usage_pct_of_max",
                "Temp 사용률 (최대 대비)", ThresholdFormat.PERCENT, 70.0, 85.0, 95.0, "Temp 사용률(최대 대비)"),
            new MetricTemplateData(AlertCategory.STORAGE, 49L, "hourly_growth_pct",
                "FRA 시간당 증가율", ThresholdFormat.PERCENT, 2.0, 5.0, 10.0, "FRA Usage 증가율 (시간당)"),
            new MetricTemplateData(AlertCategory.STORAGE, 49L, "fra_free_gb",
                "FRA 여유 용량 (역방향)", ThresholdFormat.COUNT, 50.0, 30.0, 20.0, "여유 용량이 낮을수록 문제 (GB)")
        );

        int insertedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        for (MetricTemplateData data : templates) {
            try {
                // 중복 체크 (Metric Key 기준)
                Optional<AlertMetricTemplate> existing = templateRepository.findByMetricKey(data.metricKey);
                if (existing.isPresent()) {
                    log.debug("[AlertMetricTemplateInitializer] 메트릭 템플릿이 이미 존재합니다. 스킵: metricKey={}", data.metricKey);
                    skippedCount++;
                    continue;
                }

                // Graph 엔티티 조회
                Graph graph = graphRepository.findById(data.graphId)
                    .orElse(null);
                if (graph == null) {
                    log.warn("[AlertMetricTemplateInitializer] Graph를 찾을 수 없습니다. 스킵: graphId={}, metricKey={}", 
                        data.graphId, data.metricKey);
                    errorCount++;
                    continue;
                }

                // AlertMetricTemplate 생성 및 저장
                AlertMetricTemplate template = AlertMetricTemplate.builder()
                    .category(data.category)
                    .graph(graph)
                    .metricKey(data.metricKey)
                    .metricName(data.metricName)
                    .thresholdFormat(data.thresholdFormat)
                    .defaultWarning(data.defaultWarning)
                    .defaultDanger(data.defaultDanger)
                    .defaultCritical(data.defaultCritical)
                    .description(data.description)
                    .isActive(true)
                    .build();

                templateRepository.save(template);
                insertedCount++;
                log.debug("[AlertMetricTemplateInitializer] 메트릭 템플릿 삽입 완료: category={}, metricKey={}, metricName={}", 
                    data.category, data.metricKey, data.metricName);

            } catch (Exception e) {
                log.error("[AlertMetricTemplateInitializer] 메트릭 템플릿 삽입 실패: metricKey={}, error={}", 
                    data.metricKey, e.getMessage(), e);
                errorCount++;
            }
        }

        log.info("[AlertMetricTemplateInitializer] 메트릭 템플릿 초기 데이터 삽입 완료: 삽입={}, 스킵={}, 실패={}", 
            insertedCount, skippedCount, errorCount);
    }

    /**
     * 메트릭 템플릿 데이터를 담는 내부 클래스
     */
    private static class MetricTemplateData {
        final AlertCategory category;
        final Long graphId;
        final ThresholdFormat thresholdFormat;
        final String metricKey;
        final String metricName;
        final Double defaultWarning;
        final Double defaultDanger;
        final Double defaultCritical;
        final String description;

        MetricTemplateData(AlertCategory category, Long graphId, String metricKey, String metricName,
                           ThresholdFormat thresholdFormat,
                           Double defaultWarning, Double defaultDanger, Double defaultCritical, String description) {
            this.category = category;
            this.graphId = graphId;
            this.thresholdFormat = thresholdFormat;
            this.metricKey = metricKey;
            this.metricName = metricName;
            this.defaultWarning = defaultWarning;
            this.defaultDanger = defaultDanger;
            this.defaultCritical = defaultCritical;
            this.description = description;
        }
    }
}

