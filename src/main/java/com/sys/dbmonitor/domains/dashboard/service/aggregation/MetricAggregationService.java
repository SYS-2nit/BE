/*
 ******************************************************************
 작성자: 최영준, 배지원
 ******************************************************************
 */
package com.sys.dbmonitor.domains.dashboard.service.aggregation;

import com.sys.dbmonitor.domains.dashboard.domain.MetricData;
import com.sys.dbmonitor.domains.dashboard.repository.MetricDataRepository;
import com.sys.dbmonitor.domains.dashboard.service.mapping.GraphRegistry;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 메트릭 데이터 집계 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricAggregationService {

    private final EntityManager entityManager;
    private final MetricDataRepository metricDataRepository;
    private final InstanceRepository instanceRepository;

    /**
     * 10분 데이터 집계 (1분 데이터 10개 평균)
     */
    @Transactional
    public void aggregateTo10Minutes(LocalDateTime targetTime) {
        log.info("[Aggregation] 10분 데이터 집계 시작: targetTime={}", targetTime);
        
        // 10분 단위로 내림한 시간 (예: 10:10 -> 10:10, 10:15 -> 10:10, 10:20 -> 10:20)
        // 스케줄러가 10:10에 실행되면 10:00~10:09 데이터를 집계해야 하므로, targetTime에서 10분을 빼서 반올림
        LocalDateTime roundedTime = roundTo10Minutes(targetTime.minusMinutes(1));
        
        // 집계할 시간 범위: roundedTime부터 10분 후까지 (예: 10:00 집계 시 10:00~10:09)
        LocalDateTime startTime = roundedTime;
        LocalDateTime endTime = roundedTime.plusMinutes(10);
        
        List<Instance> instances = instanceRepository.findByIsDeletedFalse();
        int totalAggregated = 0;
        
        for (Instance instance : instances) {
            Long instanceId = instance.getId();
            if (instanceId == null) continue;
            
            try {
                int count = aggregateForInstance(instanceId, "1m", "10m", startTime, endTime, roundedTime);
                totalAggregated += count;
            } catch (Exception e) {
                log.error("[Aggregation] 10분 집계 실패: instanceId={}, targetTime={}", instanceId, targetTime, e);
            }
        }
        
        log.info("[Aggregation] 10분 데이터 집계 완료: totalAggregated={}", totalAggregated);
    }

    /**
     * 1시간 데이터 집계 (10분 데이터 6개 평균)
     * 예: 11:00에 실행되면 10:00~10:50 사이의 10분 데이터 6개를 집계하여 10:00 시간으로 저장
     */
    @Transactional
    public void aggregateTo1Hour(LocalDateTime targetTime) {
        log.info("[Aggregation] 1시간 데이터 집계 시작: targetTime={}", targetTime);
        
        // 1시간 단위로 내림한 시간 (예: 11:00 -> 10:00)
        // 스케줄러가 11:00에 실행되면 10:00~10:50 데이터를 집계해야 하므로, targetTime에서 1시간을 빼서 반올림
        LocalDateTime roundedTime = roundTo1Hour(targetTime.minusHours(1));
        
        // 집계할 시간 범위: roundedTime부터 50분 후까지 (예: 10:00 집계 시 10:00~10:50)
        // 10분 데이터는 10:00, 10:10, 10:20, 10:30, 10:40, 10:50에 존재하므로 endTime은 10:51로 설정
        LocalDateTime startTime = roundedTime;
        LocalDateTime endTime = roundedTime.plusMinutes(50).plusSeconds(1); // 10:50:01 (10:50 데이터 포함)
        
        List<Instance> instances = instanceRepository.findByIsDeletedFalse();
        int totalAggregated = 0;
        
        for (Instance instance : instances) {
            Long instanceId = instance.getId();
            if (instanceId == null) continue;
            
            try {
                int count = aggregateForInstance(instanceId, "10m", "1h", startTime, endTime, roundedTime);
                totalAggregated += count;
            } catch (Exception e) {
                log.error("[Aggregation] 1시간 집계 실패: instanceId={}, targetTime={}", instanceId, targetTime, e);
            }
        }
        
        log.info("[Aggregation] 1시간 데이터 집계 완료: totalAggregated={}", totalAggregated);
    }

    /**
     * 1일 데이터 집계 (1시간 데이터 24개 평균)
     * 예: 다음날 00:00에 실행되면 전날 00:00~23:00 사이의 1시간 데이터 24개를 집계하여 전날 00:00 시간으로 저장
     */
    @Transactional
    public void aggregateTo1Day(LocalDateTime targetTime) {
        log.info("[Aggregation] 1일 데이터 집계 시작: targetTime={}", targetTime);
        
        // 1일 단위로 내림한 시간 (자정)
        // 스케줄러가 다음날 00:00에 실행되면 전날 00:00~23:00 데이터를 집계해야 하므로, targetTime에서 1일을 빼서 반올림
        LocalDateTime roundedTime = roundTo1Day(targetTime.minusDays(1));
        
        // 집계할 시간 범위: roundedTime부터 23시간 후까지 (예: 전날 00:00 집계 시 전날 00:00~23:00)
        // 1시간 데이터는 00:00, 01:00, ..., 23:00에 존재하므로 endTime은 다음날 00:00:01로 설정
        LocalDateTime startTime = roundedTime;
        LocalDateTime endTime = roundedTime.plusDays(1).plusSeconds(1); // 다음날 00:00:01 (23:00 데이터 포함)
        
        List<Instance> instances = instanceRepository.findByIsDeletedFalse();
        int totalAggregated = 0;
        
        for (Instance instance : instances) {
            Long instanceId = instance.getId();
            if (instanceId == null) continue;
            
            try {
                int count = aggregateForInstance(instanceId, "1h", "1d", startTime, endTime, roundedTime);
                totalAggregated += count;
            } catch (Exception e) {
                log.error("[Aggregation] 1일 집계 실패: instanceId={}, targetTime={}", instanceId, targetTime, e);
            }
        }
        
        log.info("[Aggregation] 1일 데이터 집계 완료: totalAggregated={}", totalAggregated);
    }

    /**
     * 인스턴스별 집계 수행
     */
    private int aggregateForInstance(
            Long instanceId,
            String sourceIntervalType,
            String targetIntervalType,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime roundedTime
    ) {
        // 모든 그래프에 대해 집계 수행
        List<Integer> graphIds = GraphRegistry.all().stream()
                .map(rule -> rule.graphId())
                .collect(Collectors.toList());
        
        int aggregatedCount = 0;
        
        for (Integer graphId : graphIds) {
            try {
                // 소스 데이터 조회 (집계할 데이터) - JPQL 사용
                // startTime <= collectedAt < endTime 조건으로 조회
                String jpql = "SELECT m FROM MetricData m " +
                        "WHERE m.instanceId = :instanceId " +
                        "AND m.graphId = :graphId " +
                        "AND m.intervalType = :intervalType " +
                        "AND m.collectedAt >= :startTime " +
                        "AND m.collectedAt < :endTime " +
                        "ORDER BY m.collectedAt ASC";
                
                Query query = entityManager.createQuery(jpql, MetricData.class);
                query.setParameter("instanceId", instanceId);
                query.setParameter("graphId", (long) graphId);
                query.setParameter("intervalType", sourceIntervalType);
                query.setParameter("startTime", startTime);
                query.setParameter("endTime", endTime);
                
                @SuppressWarnings("unchecked")
                List<MetricData> sourceData = query.getResultList();
                
                log.debug("[Aggregation] 소스 데이터 조회: instanceId={}, graphId={}, sourceIntervalType={}, startTime={}, endTime={}, count={}",
                        instanceId, graphId, sourceIntervalType, startTime, endTime, sourceData.size());
                
                if (sourceData.isEmpty()) {
                    continue;
                }
                
                // 이미 집계된 데이터가 있는지 확인
                String checkJpql = "SELECT COUNT(m) FROM MetricData m " +
                        "WHERE m.instanceId = :instanceId " +
                        "AND m.graphId = :graphId " +
                        "AND m.intervalType = :intervalType " +
                        "AND m.collectedAt = :collectedAt";
                
                Query checkQuery = entityManager.createQuery(checkJpql);
                checkQuery.setParameter("instanceId", instanceId);
                checkQuery.setParameter("graphId", (long) graphId);
                checkQuery.setParameter("intervalType", targetIntervalType);
                checkQuery.setParameter("collectedAt", roundedTime);
                
                Long count = (Long) checkQuery.getSingleResult();
                
                if (count > 0) {
                    log.debug("[Aggregation] 이미 집계된 데이터가 있습니다. instanceId={}, graphId={}, intervalType={}, collectedAt={}",
                            instanceId, graphId, targetIntervalType, roundedTime);
                    continue;
                }
                
                // 평균값 계산하여 새로운 MetricData 생성
                MetricData aggregated = calculateAverage(sourceData, instanceId, graphId, targetIntervalType, roundedTime);
                
                if (aggregated != null) {
                    metricDataRepository.save(aggregated);
                    aggregatedCount++;
                    log.debug("[Aggregation] 집계 완료: instanceId={}, graphId={}, intervalType={}, collectedAt={}, sourceCount={}",
                            instanceId, graphId, targetIntervalType, roundedTime, sourceData.size());
                }
                
            } catch (Exception e) {
                log.error("[Aggregation] 그래프별 집계 실패: instanceId={}, graphId={}, intervalType={}",
                        instanceId, graphId, targetIntervalType, e);
            }
        }
        
        return aggregatedCount;
    }

    /**
     * 평균값 계산
     */
    private MetricData calculateAverage(
            List<MetricData> sourceData,
            Long instanceId,
            Integer graphId,
            String targetIntervalType,
            LocalDateTime collectedAt
    ) {
        if (sourceData.isEmpty()) {
            return null;
        }
        
        // 첫 번째 데이터를 기준으로 생성
        MetricData first = sourceData.get(0);
        MetricData aggregated = new MetricData();
        aggregated.setInstanceId(instanceId);
        aggregated.setGraphId((long) graphId);
        aggregated.setCategoryId(first.getCategoryId());
        aggregated.setCollectedAt(collectedAt);
        aggregated.setIntervalType(targetIntervalType);
        
        // MetricData의 모든 필드에 대해 평균 계산
        Field[] fields = MetricData.class.getDeclaredFields();
        
        for (Field field : fields) {
            // 메타데이터 필드는 제외
            if (isMetadataField(field.getName())) {
                continue;
            }
            
            try {
                Method getter = getGetterMethod(MetricData.class, field.getName());
                if (getter == null) continue;
                
                Class<?> fieldType = field.getType();
                
                // Double 필드만 평균 계산
                if (fieldType == Double.class) {
                    List<Double> values = new ArrayList<>();
                    for (MetricData data : sourceData) {
                        Double value = (Double) getter.invoke(data);
                        if (value != null) {
                            values.add(value);
                        }
                    }
                    
                    if (!values.isEmpty()) {
                        double average = values.stream()
                                .mapToDouble(Double::doubleValue)
                                .average()
                                .orElse(0.0);
                        
                        Method setter = getSetterMethod(MetricData.class, field.getName(), Double.class);
                        if (setter != null) {
                            setter.invoke(aggregated, average);
                        }
                    }
                }
                // String 필드는 첫 번째 값 사용
                else if (fieldType == String.class) {
                    String value = (String) getter.invoke(first);
                    if (value != null) {
                        Method setter = getSetterMethod(MetricData.class, field.getName(), String.class);
                        if (setter != null) {
                            setter.invoke(aggregated, value);
                        }
                    }
                }
                
            } catch (Exception e) {
                log.debug("[Aggregation] 필드 처리 실패: field={}, error={}", field.getName(), e.getMessage());
            }
        }
        
        return aggregated;
    }

    /**
     * 메타데이터 필드 여부 확인
     */
    private boolean isMetadataField(String fieldName) {
        return fieldName.equals("id") ||
               fieldName.equals("instanceId") ||
               fieldName.equals("graphId") ||
               fieldName.equals("categoryId") ||
               fieldName.equals("collectedAt") ||
               fieldName.equals("intervalType");
    }

    /**
     * Getter 메서드 찾기
     */
    private Method getGetterMethod(Class<?> clazz, String fieldName) {
        String getterName = "get" + capitalize(fieldName);
        try {
            return clazz.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            // boolean 필드의 경우 "is" 접두사 사용
            if (fieldName.startsWith("is")) {
                try {
                    return clazz.getMethod(fieldName);
                } catch (NoSuchMethodException ex) {
                    return null;
                }
            }
            return null;
        }
    }

    /**
     * Setter 메서드 찾기
     */
    private Method getSetterMethod(Class<?> clazz, String fieldName, Class<?> paramType) {
        String setterName = "set" + capitalize(fieldName);
        try {
            return clazz.getMethod(setterName, paramType);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * 필드명 첫 글자 대문자화
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 10분 단위로 시간 반올림 (내림)
     * 예: 10:15 -> 10:10, 10:09 -> 10:00
     */
    private LocalDateTime roundTo10Minutes(LocalDateTime time) {
        int minute = time.getMinute();
        int roundedMinute = (minute / 10) * 10;
        return time.withMinute(roundedMinute).withSecond(0).withNano(0);
    }

    /**
     * 1시간 단위로 시간 반올림
     */
    private LocalDateTime roundTo1Hour(LocalDateTime time) {
        return time.withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * 1일 단위로 시간 반올림 (자정)
     */
    private LocalDateTime roundTo1Day(LocalDateTime time) {
        return time.withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
}

