package com.sys.dbmonitor.domains.dashboard.batch;

import com.sys.dbmonitor.domains.dashboard.domain.MetricData;
import com.sys.dbmonitor.domains.dashboard.repository.MetricDataRepository;
import com.sys.dbmonitor.domains.dashboard.service.CollectorService;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRegistry;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricCollectionTasklet implements Tasklet {

    private static final String DEFAULT_INTERVAL_TYPE = "1m";

    private final InstanceRepository instanceRepository;
    private final CollectorService collectorService;
    private final MetricDataRepository metricDataRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        List<Instance> instances = instanceRepository.findByIsDeletedFalse();

        if (instances.isEmpty()) {
            log.info("[MetricBatch] 수집 대상 인스턴스가 없습니다. 작업을 종료합니다.");
            return RepeatStatus.FINISHED;
        }

        for (Instance instance : instances) {
            Long instanceId = instance.getId();

            if (instanceId == null) {
                continue;
            }

            if (instance.getDbInfo() != null && Boolean.TRUE.equals(instance.getDbInfo().getIsDeleted())) {
                log.debug("[MetricBatch] 삭제된 DBInfo에 연결된 인스턴스 건너뜀: instanceId={}", instanceId);
                continue;
            }

            try {
                Map<String, Object> finals = collectorService.runOnce(instanceId);

                if (finals == null || finals.isEmpty()) {
                    log.warn("[MetricBatch] 수집 결과가 비어있어 저장을 건너뜀: instanceId={}", instanceId);
                    continue;
                }

                List<MetricData> rows = new ArrayList<>();
                GraphRegistry.all().forEach(rule -> rows.add(
                        GraphRegistry.mapRow(rule.graphId(), instanceId, DEFAULT_INTERVAL_TYPE, finals)
                ));

                if (!rows.isEmpty()) {
                    metricDataRepository.saveAll(rows);
                    log.info("[MetricBatch] Metric 데이터 저장 완료: instanceId={}, rowCount={}", instanceId, rows.size());
                }
            } catch (Exception ex) {
                log.error("[MetricBatch] 메트릭 수집 중 오류 발생: instanceId={}", instanceId, ex);
            }
        }

        return RepeatStatus.FINISHED;
    }
}

