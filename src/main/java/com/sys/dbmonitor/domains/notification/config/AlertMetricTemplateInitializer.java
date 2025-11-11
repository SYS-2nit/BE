package com.sys.dbmonitor.domains.notification.config;

import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.domains.graph.repository.GraphRepository;
import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.AlertMetricTemplate;
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
                "Host CPU 사용률", 70.0, 85.0, 95.0, "호스트 전체 CPU 사용률"),
            new MetricTemplateData(AlertCategory.CPU, 14L, "CPU_SATURATION_PCT", 
                "DB CPU 포화도", 80.0, 90.0, 95.0, "DB CPU 포화도"),
            new MetricTemplateData(AlertCategory.CPU, 16L, "DB_OF_HOST_SHARE_PCT", 
                "DB CPU 비율", 60.0, 75.0, 90.0, "DB CPU Share of Host (%)"),
            new MetricTemplateData(AlertCategory.CPU, 16L, "OTHER_PROCESSES_PCT", 
                "기타 프로세스 CPU 비율", 30.0, 50.0, 70.0, "기타 프로세스 CPU 비율 (Graph 16과 공유)"),
            new MetricTemplateData(AlertCategory.CPU, 14L, "AAS_ONCPU_SESSIONS", 
                "On-CPU 세션 수", 70.0, 85.0, 95.0, "AAS_ONCPU_SESSIONS / CORE_BASELINE_SESSIONS * 100 (Graph 14와 공유)"),

            // ===== Memory (카테고리 3) - 5개 메트릭 =====
            new MetricTemplateData(AlertCategory.MEMORY, 23L, "PGA_UTIL_PCT", 
                "PGA 사용률", 70.0, 85.0, 95.0, "PGA Utilization (%)"),
            new MetricTemplateData(AlertCategory.MEMORY, 24L, "SGA_UTIL_PCT", 
                "SGA 사용률", 70.0, 85.0, 95.0, "SGA Utilization (%)"),
            new MetricTemplateData(AlertCategory.MEMORY, 25L, "WORKAREA_SPILL_RATE_PCT", 
                "Workarea Spill 비율", 10.0, 20.0, 30.0, "Workarea Spill Rate (%)"),
            new MetricTemplateData(AlertCategory.MEMORY, 27L, "BUFFER_MISS_PCT", 
                "Buffer Cache Miss 비율", 10.0, 20.0, 30.0, "Buffer Cache Miss Rate (%)"),
            new MetricTemplateData(AlertCategory.MEMORY, 7L, "HARD_PARSE_RATIO_PCT", 
                "Hard Parse 비율", 5.0, 10.0, 20.0, "Hard Parse Ratio (%)"),

            // ===== Session (카테고리 4) - 5개 메트릭 =====
            new MetricTemplateData(AlertCategory.SESSION, 35L, "ACTIVE_USER_RATIO_PCT", 
                "활성 사용자 비율", 80.0, 90.0, 95.0, "Active User Ratio (%)"),
            new MetricTemplateData(AlertCategory.SESSION, 35L, "SESSIONS_LIMIT_UTIL_PCT", 
                "세션 한도 사용률", 70.0, 85.0, 95.0, "Sessions Limit Utilization (%) (Graph 35과 공유)"),
            new MetricTemplateData(AlertCategory.SESSION, 35L, "PROCESSES_LIMIT_UTIL_PCT", 
                "프로세스 한도 사용률", 70.0, 85.0, 95.0, "Processes Limit Utilization (%) (Graph 35과 공유)"),
            new MetricTemplateData(AlertCategory.SESSION, 8L, "session_usage_pct", 
                "세션 사용률", 70.0, 85.0, 95.0, "Session Usage (%)"),
            new MetricTemplateData(AlertCategory.SESSION, 12L, "processes_usage_pct", 
                "프로세스 사용률", 70.0, 85.0, 95.0, "Processes Usage (%)"),

            // ===== I/O (카테고리 5) - 3개 메트릭 =====
            new MetricTemplateData(AlertCategory.IO, 38L, "direct_io_ratio_pct", 
                "Direct I/O 비율", 20.0, 30.0, 40.0, "Direct Path I/O Ratio (%)"),
            new MetricTemplateData(AlertCategory.IO, 37L, "parse_execute_ratio", 
                "Parse/Execute 비율", 50.0, 70.0, 90.0, "Parse/Execute Ratio (0~1 범위이므로 * 100으로 변환하여 0~100%로 처리)"),
            new MetricTemplateData(AlertCategory.IO, 37L, "cache_hit_ratio_pct", 
                "캐시 히트 비율 (역방향)", 10.0, 15.0, 20.0, "Cache Hit Ratio (%) - 역방향 (100 - cache_hit_ratio_pct로 변환하여 높을수록 문제로 처리)"),

            // ===== Storage (카테고리 6) - 5개 메트릭 =====
            new MetricTemplateData(AlertCategory.STORAGE, 51L, "total_db_usage_percent", 
                "전체 DB 사용률", 70.0, 85.0, 95.0, "Total Database Usage (%)"),
            new MetricTemplateData(AlertCategory.STORAGE, 49L, "usage_pct", 
                "FRA 사용률", 70.0, 85.0, 95.0, "FRA Usage (%) - Graph 49의 usage_pct 필드 사용"),
            new MetricTemplateData(AlertCategory.STORAGE, 50L, "undo_usage_percent", 
                "Undo 사용률", 70.0, 85.0, 95.0, "Undo Usage (%)"),
            new MetricTemplateData(AlertCategory.STORAGE, 46L, "temp_usage_percent", 
                "Temp 사용률", 70.0, 85.0, 95.0, "Temp Tablespace Usage (%)"),
            new MetricTemplateData(AlertCategory.STORAGE, 45L, "MAX_TS_USAGE_PCT", 
                "최대 테이블스페이스 사용률", 80.0, 90.0, 95.0, "Maximum Tablespace Usage (%)")
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
        final String metricKey;
        final String metricName;
        final Double defaultWarning;
        final Double defaultDanger;
        final Double defaultCritical;
        final String description;

        MetricTemplateData(AlertCategory category, Long graphId, String metricKey, String metricName,
                          Double defaultWarning, Double defaultDanger, Double defaultCritical, String description) {
            this.category = category;
            this.graphId = graphId;
            this.metricKey = metricKey;
            this.metricName = metricName;
            this.defaultWarning = defaultWarning;
            this.defaultDanger = defaultDanger;
            this.defaultCritical = defaultCritical;
            this.description = description;
        }
    }
}

