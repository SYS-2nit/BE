package com.sys.dbmonitor.domains.dashboard.batch;

import com.sys.dbmonitor.domains.dashboard.service.aggregation.MetricAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 메트릭 데이터 집계 Tasklet
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricAggregationTasklet implements Tasklet {

    private final MetricAggregationService aggregationService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        // 현재 시간 (Asia/Seoul 기준)
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        
        // Job 파라미터에서 집계 타입 가져오기
        String aggregationType = chunkContext.getStepContext()
                .getJobParameters()
                .get("aggregationType")
                .toString();
        
        try {
            switch (aggregationType) {
                case "10m":
                    aggregationService.aggregateTo10Minutes(now);
                    break;
                case "1h":
                    aggregationService.aggregateTo1Hour(now);
                    break;
                case "1d":
                    aggregationService.aggregateTo1Day(now);
                    break;
                default:
                    log.warn("[Aggregation] 알 수 없는 집계 타입: {}", aggregationType);
            }
        } catch (Exception e) {
            log.error("[Aggregation] 집계 작업 실패: aggregationType={}", aggregationType, e);
            throw new RuntimeException("집계 작업 실패", e);
        }
        
        return RepeatStatus.FINISHED;
    }
}

