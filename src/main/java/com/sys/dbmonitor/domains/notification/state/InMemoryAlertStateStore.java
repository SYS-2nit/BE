/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.state;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인메모리 기반 알림 상태 저장소 구현
 * 
 * ConcurrentHashMap을 사용하여 멀티스레드 환경에서 안전하게 상태를 관리.
 * 프로덕션 환경에서는 Redis 기반 구현으로 대체 가능.
 */
@Component("inMemoryAlertStateStore")
@Primary
public class InMemoryAlertStateStore implements AlertStateStore {

    /**
     * Key: "instanceId:alertEventId" → AlertState
     * 예: "1:5" → AlertState(consecutiveCount=3, lastCheckedAt=..., lastSeverity=CRITICAL)
     */
    private final ConcurrentHashMap<String, AlertState> stateMap = new ConcurrentHashMap<>();

    /**
     * Key 생성: "instanceId:alertEventId"
     */
    private static String key(Long instanceId, Long alertEventId) {
        return instanceId + ":" + alertEventId;
    }

    @Override
    public Optional<AlertState> get(Long instanceId, Long alertEventId) {
        if (instanceId == null || alertEventId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(stateMap.get(key(instanceId, alertEventId)));
    }

    @Override
    public void put(Long instanceId, Long alertEventId, AlertState state) {
        if (instanceId == null || alertEventId == null || state == null) {
            return;
        }
        stateMap.put(key(instanceId, alertEventId), state);
    }

    @Override
    public void clear(Long instanceId) {
        if (instanceId == null) {
            return;
        }
        String prefix = instanceId + ":";
        stateMap.keySet().removeIf(key -> key.startsWith(prefix));
    }

    @Override
    public void clearAll() {
        stateMap.clear();
    }
}

