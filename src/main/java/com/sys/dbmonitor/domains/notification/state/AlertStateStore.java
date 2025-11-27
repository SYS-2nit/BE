/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.state;

import java.time.Instant;
import java.util.Optional;

/**
 * 알림 상태 저장소 인터페이스
 * 
 * 각 알림 규칙(AlertEvent)별로 연속 초과 횟수와 마지막 체크 시간을 저장하여
 * 누적 시간 조건(DELAY_TIME)을 확인합니다.
 * 
 * Key 형식: "instanceId:alertEventId"
 * 예: "1:5" (instanceId=1, alertEventId=5)
 */
public interface AlertStateStore {

    /**
     * 알림 상태 정보
     * 
     * @param consecutiveCount 연속 초과 횟수 (임계값을 연속으로 초과한 횟수)
     * @param lastCheckedAt 마지막 체크 시간
     * @param lastSeverity 마지막 심각도 (WARNING, DANGER, CRITICAL)
     */
    record AlertState(
        int consecutiveCount,
        Instant lastCheckedAt,
        com.sys.dbmonitor.domains.notification.domain.AlertLevel lastSeverity
    ) {
        /**
         * 연속 초과 횟수를 1 증가시킨 새 상태 생성
         */
        public AlertState increment(com.sys.dbmonitor.domains.notification.domain.AlertLevel severity) {
            return new AlertState(
                this.consecutiveCount + 1,
                Instant.now(),
                severity
            );
        }

        /**
         * 연속 초과 횟수를 리셋한 새 상태 생성 (임계값 미달 시)
         */
        public AlertState reset() {
            return new AlertState(
                0,
                Instant.now(),
                null
            );
        }
    }

    /**
     * 알림 상태 조회
     * 
     * @param instanceId 인스턴스 ID
     * @param alertEventId 알림 규칙 ID
     * @return 알림 상태 (없으면 Optional.empty())
     */
    Optional<AlertState> get(Long instanceId, Long alertEventId);

    /**
     * 알림 상태 저장 또는 업데이트
     * 
     * @param instanceId 인스턴스 ID
     * @param alertEventId 알림 규칙 ID
     * @param state 알림 상태
     */
    void put(Long instanceId, Long alertEventId, AlertState state);

    /**
     * 연속 초과 횟수 증가 (임계값 초과 시)
     * 
     * @param instanceId 인스턴스 ID
     * @param alertEventId 알림 규칙 ID
     * @param severity 현재 심각도
     * @return 업데이트된 알림 상태
     */
    default AlertState incrementCount(Long instanceId, Long alertEventId, 
                                      com.sys.dbmonitor.domains.notification.domain.AlertLevel severity) {
        AlertState current = get(instanceId, alertEventId)
            .orElse(new AlertState(0, Instant.now(), null));
        AlertState updated = current.increment(severity);
        put(instanceId, alertEventId, updated);
        return updated;
    }

    /**
     * 연속 초과 횟수 리셋 (임계값 미달 시)
     * 
     * @param instanceId 인스턴스 ID
     * @param alertEventId 알림 규칙 ID
     */
    default void resetCount(Long instanceId, Long alertEventId) {
        AlertState current = get(instanceId, alertEventId)
            .orElse(new AlertState(0, Instant.now(), null));
        AlertState reset = current.reset();
        put(instanceId, alertEventId, reset);
    }

    /**
     * 인스턴스별 모든 알림 상태 초기화
     * 
     * @param instanceId 인스턴스 ID
     */
    void clear(Long instanceId);

    /**
     * 모든 알림 상태 초기화
     */
    void clearAll();
}

