package com.sys.dbmonitor.domains.instance.service.query;

import com.sys.dbmonitor.domains.dashboard.engine.MetricsEngine;
import com.sys.dbmonitor.domains.dashboard.repository.MetricDataRepository;
import com.sys.dbmonitor.domains.dashboard.service.CollectorService;
import com.sys.dbmonitor.domains.instance.domain.DBInfo;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.dto.InstanceDataDTO;
import com.sys.dbmonitor.domains.instance.repository.DBInfoRepository;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.instance.dto.response.InstanceListResponse;
import com.sys.dbmonitor.domains.instance.dto.response.InstanceResponse;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import com.sys.dbmonitor.domains.notification.domain.AlertLevel;
import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import com.sys.dbmonitor.domains.notification.repository.AlertEventRepository;
import com.sys.dbmonitor.domains.notification.repository.EventRepository;
import com.sys.dbmonitor.global.config.DynamicDataSourceFactory;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.System.out;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstanceQueryService {

    private final InstanceRepository instanceRepository;
    private final DBInfoRepository dbInfoRepository;
    private final DynamicDataSourceFactory dynamicDataSourceFactory;
    private final EventRepository eventRepository;
    private final MetricDataRepository metricDataRepository;
    private final AlertEventRepository alertEventRepository;
    private final CollectorService collectorService;

    /**
     * 타겟 DB 목록 조회 (전체)
     */
    public List<InstanceResponse> getAllTargetDatabases() {
        return dbInfoRepository.findByIsDeletedFalse().stream()
                .map(InstanceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 활성화된 타겟 DB 목록 조회
     */
    public List<InstanceResponse> getActiveTargetDatabases() {
        // 활성화된 DBInfo 목록 조회
        List<DBInfo> activeDbInfos = dbInfoRepository.findByIsActiveTrueAndIsDeletedFalse();
        
        if (activeDbInfos.isEmpty()) {
            throw new NotFoundException(ExceptionMessage.DB_NOT_ACTIVE, "활성화 된 DB가 없습니다.");
        }

        return activeDbInfos.stream()
                .map(InstanceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 타겟 DB 상세 조회
     */
    public InstanceResponse getTargetDatabase(Long id) {
        Instance targetDatabase = instanceRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB를 찾을 수 없습니다."));

        if (targetDatabase.getDbInfo() != null && Boolean.TRUE.equals(targetDatabase.getDbInfo().getIsDeleted())) {
            throw new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB를 찾을 수 없습니다.");
        }
        return InstanceResponse.from(targetDatabase);
    }

    /**
     * 이름으로 타겟 DB 조회 (DBInfo의 name으로 조회)
     */
    public InstanceResponse getTargetDatabaseByName(String name) {
        // DBInfo에서 name으로 조회
        DBInfo dbInfo = dbInfoRepository.findByNameAndIsDeletedFalse(name)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.DB_INFO_NOT_FOUND, 
                        "데이터베이스 정보를 찾을 수 없습니다: " + name));
        
        // 해당 DBInfo에 연결된 첫 번째 Instance 조회
        List<Instance> instances = instanceRepository.findByDbInfoAndIsDeletedFalse(dbInfo);
        if (instances.isEmpty()) {
            throw new NotFoundException(ExceptionMessage.DB_INSTANCE_NOT_FOUND, 
                    "데이터베이스 인스턴스를 찾을 수 없습니다: " + name);
        }
        
        // 첫 번째 Instance 반환 (여러 개인 경우 첫 번째)
        return InstanceResponse.from(instances.get(0));
    }

    /**
     * 타겟 DB에서 데이터 조회
     */
    public List<Map<String, Object>> queryTargetDatabase(Long instanceId) {
        // 타겟 DB 존재 확인
        Instance instance = instanceRepository.findByIdAndIsDeletedFalse(instanceId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB를 찾을 수 없습니다."));

        if (instance.getDbInfo() != null && Boolean.TRUE.equals(instance.getDbInfo().getIsDeleted())) {
            throw new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB를 찾을 수 없습니다.");
        }

        // 타겟 DB 데이터소스 가져오기
        DataSource dataSource = dynamicDataSourceFactory.getDataSource(instanceId);
        if (dataSource == null) {
            throw new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB가 연결되지 않았습니다. 먼저 연결해주세요.");
        }

        // JdbcTemplate을 사용하여 쿼리 실행
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        
        // Oracle 테이블 목록 조회 예시
        String sql = "SELECT TABLE_NAME, TABLESPACE_NAME, NUM_ROWS, LAST_ANALYZED " +
                     "FROM USER_TABLES " +
                     "ORDER BY TABLE_NAME";


        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 특정 DB에 속한 인스턴스 목록 조회
     */
    public List<InstanceListResponse> getInstancesByDatabase(Long dbInfoId) {
        DBInfo dbInfo = dbInfoRepository.findByIdAndIsDeletedFalse(dbInfoId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.DB_INFO_NOT_FOUND,
                        "데이터베이스 정보를 찾을 수 없습니다."));

        List<Instance> instances = instanceRepository.findByDbInfoIdAndIsDeletedFalse(dbInfoId);

        // 각 인스턴스별로 데이터 조회 및 계산
        return instances.stream()
                .filter(instance -> instance.getDbInfo() == null
                        || Boolean.FALSE.equals(instance.getDbInfo().getIsDeleted()))
                .map(instance -> {
                    // 인스턴스별 최신 데이터 조회
                    List<InstanceDataDTO> dataList = metricDataRepository.findInstanceDataByInstance(instance.getId());
                    InstanceDataDTO instanceData = dataList.isEmpty() ? null : dataList.get(0);

                    // 계산이 필요한 값들 처리
                    String cpuUsage = null;
                    String sga = null;

                    if (instanceData != null) {
                        // cpuUsage는 이미 계산된 값이므로 그대로 사용
                        cpuUsage = instanceData.cpuUsage();

                        sga = instanceData.sga();
                    }

                    // 계산된 값으로 새로운 DTO 생성
                    InstanceDataDTO calculatedData = instanceData != null
                            ? new InstanceDataDTO(
                                    cpuUsage,
                                    instanceData.sessionCount(),
                                    instanceData.activeSessionCount(),
                                    instanceData.lockWait(),
                                    instanceData.pga(),
                                    sga
                            )
                            : null;

                    // 인스턴스별 현재 메트릭 값 기준 최고 심각도 계산
                    Integer maxSeverity = calculateCurrentSeverity(instance.getId());

                    return InstanceListResponse.from(instance, calculatedData, maxSeverity);
                })
                .collect(Collectors.toList());
    }

    /**
     * 인스턴스별 현재 메트릭 값 기준 최고 심각도 계산
     * 
     * 현재 메트릭 값을 실시간으로 수집하여 각 알림 이벤트의 임계값과 비교하고,
     * 모든 알림 중 최고 심각도를 반환합니다.
     *
     * @param instanceId 인스턴스 ID
     * @return 최고 심각도 (null=알림 없음 또는 정상, 1=주의, 2=위험, 3=치명)
     */
    private Integer calculateCurrentSeverity(Long instanceId) {
        try {
            // 1. 활성화된 알림 이벤트 목록 조회
            List<AlertEvent> activeEvents = alertEventRepository.findActiveEventsByInstanceId(instanceId);
            
            if (activeEvents.isEmpty()) {
                return null; // 알림이 없으면 정상
            }

            // 2. 현재 메트릭 값 수집 (실시간)
            Map<String, Object> finals = collectorService.runOnce(instanceId);
            
            if (finals == null || finals.isEmpty()) {
                log.warn("[InstanceQueryService] 메트릭 데이터가 비어있습니다: instanceId={}", instanceId);
                return null; // 메트릭 데이터가 없으면 정상으로 처리
            }

            // 3. 각 알림 이벤트의 심각도 계산 및 최고 심각도 추적
            Integer maxSeverity = null;

            for (AlertEvent alertEvent : activeEvents) {
                try {
                    // 메트릭 값 추출
                    String metricKey = alertEvent.getMetricKey();
                    Object metricValueObj = finals.get(metricKey);
                    
                    if (metricValueObj == null) {
                        log.debug("[InstanceQueryService] 메트릭 값이 null입니다: instanceId={}, metricKey={}", 
                                instanceId, metricKey);
                        continue;
                    }

                    Double metricValue = convertToDouble(metricValueObj);
                    if (metricValue == null || Double.isNaN(metricValue) || Double.isInfinite(metricValue)) {
                        log.debug("[InstanceQueryService] 메트릭 값이 유효하지 않습니다: instanceId={}, metricKey={}, value={}", 
                                instanceId, metricKey, metricValueObj);
                        continue;
                    }

                    // 역방향 메트릭 처리
                    if (Boolean.TRUE.equals(alertEvent.getIsReverse())) {
                        if (alertEvent.getThresholdFormat() == ThresholdFormat.PERCENT) {
                            metricValue = 100.0 - metricValue;
                        }
                    }

                    // 심각도 결정
                    AlertLevel severity = determineSeverity(metricValue, alertEvent);
                    
                    if (severity != null) {
                        int severityValue = severity.getValue();
                        if (maxSeverity == null || severityValue > maxSeverity) {
                            maxSeverity = severityValue;
                        }
                    }
                } catch (Exception e) {
                    log.warn("[InstanceQueryService] 알림 이벤트 심각도 계산 중 오류: instanceId={}, alertEventId={}, error={}", 
                            instanceId, alertEvent.getId(), e.getMessage());
                    // 개별 알림 이벤트 오류는 무시하고 계속 진행
                }
            }

            return maxSeverity; // null이면 정상 (모든 알림이 임계값 이하)
        } catch (Exception e) {
            log.error("[InstanceQueryService] 현재 심각도 계산 중 오류 발생: instanceId={}, error={}", 
                    instanceId, e.getMessage(), e);
            return null; // 오류 발생 시 null 반환 (정상으로 표시)
        }
    }

    /**
     * Object를 Double로 변환
     */
    private Double convertToDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            log.warn("[InstanceQueryService] Double 변환 실패: value={}, error={}", value, e.getMessage());
            return null;
        }
    }

    /**
     * 임계값 비교 및 심각도 결정
     * 
     * @param metricValue 메트릭 값
     * @param alertEvent 알림 이벤트
     * @return 심각도 (null이면 임계값 미만)
     */
    private AlertLevel determineSeverity(Double metricValue, AlertEvent alertEvent) {
        boolean isReverse = Boolean.TRUE.equals(alertEvent.getIsReverse());
        ThresholdFormat format = alertEvent.getThresholdFormat();

        if (isReverse && format != ThresholdFormat.PERCENT) {
            // 역방향이면서 퍼센트가 아닌 경우(예: fra_free_gb)는 낮을수록 심각
            if (metricValue <= alertEvent.getCritical()) {
                return AlertLevel.CRITICAL;
            }
            if (metricValue <= alertEvent.getDanger()) {
                return AlertLevel.DANGER;
            }
            if (metricValue <= alertEvent.getWarning()) {
                return AlertLevel.WARNING;
            }
            return null;
        } else {
            // 기본: 높을수록 심각
            if (metricValue >= alertEvent.getCritical()) {
                return AlertLevel.CRITICAL;
            }
            if (metricValue >= alertEvent.getDanger()) {
                return AlertLevel.DANGER;
            }
            if (metricValue >= alertEvent.getWarning()) {
                return AlertLevel.WARNING;
            }
            return null;
        }
    }
}

