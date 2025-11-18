package com.sys.dbmonitor.domains.dashboard.batch.sql;


import com.sys.dbmonitor.domains.dashboard.service.CollectorService;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRegistry;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.notification.service.command.AlertCheckService;
import com.sys.dbmonitor.domains.sql.repository.SqlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqlCollectionTasklet implements Tasklet {

    private final InstanceRepository instanceRepository;
    private final CollectorService collectorService;

    /**
     * sql_data 저장 여부 제어 플래그.
     * 기본값 true(운영 환경) → DB 저장, dev 프로파일에서는 application-dev.yml에서 false로 설정하여 저장을 막는다.
     */
    @Value("${app.sql.persist:true}")
    private boolean sqlPersistEnabled;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        List<Instance> instances = instanceRepository.findByIsDeletedFalse();

        if (instances.isEmpty()) {
            log.info("[sqlBatch] 수집 대상 인스턴스가 없습니다. 작업을 종료합니다.");
            return RepeatStatus.FINISHED;
        }

        for (Instance instance : instances) {
            Long instanceId = instance.getId();

            if (instanceId == null) {
                continue;
            }

            if (instance.getDbInfo() != null && Boolean.TRUE.equals(instance.getDbInfo().getIsDeleted())) {
                log.debug("[sqlBatch] 삭제된 DBInfo에 연결된 인스턴스 건너뜀: instanceId={}", instanceId);
                continue;
            }

            try {
                collectorService.sqlRunOnce(instanceId);


            } catch (Exception ex) {
                log.error("[sqlBatch] 메트릭 수집 중 오류 발생: instanceId={}", instanceId, ex);
            }
        }

        return RepeatStatus.FINISHED;
    }
}

