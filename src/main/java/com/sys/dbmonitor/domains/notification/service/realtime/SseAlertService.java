package com.sys.dbmonitor.domains.notification.service.realtime;

import com.sys.dbmonitor.domains.notification.domain.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE(Server-Sent Events) 알림 서비스
 * 
 * 실시간 알림 전송을 위한 SSE 연결을 관리하고,
 * 알림 발생 시 클라이언트에게 실시간으로 전송합니다.
 */
@Slf4j
@Service
public class SseAlertService {

    /**
     * 사용자별 SSE 연결 관리
     * Key: userId (Long)
     * Value: SseEmitter
     */
    private final Map<Long, SseEmitter> connections = new ConcurrentHashMap<>();

    /**
     * SSE 연결 생성
     * 
     * @param userId 사용자 ID
     * @return SseEmitter
     */
    public SseEmitter createConnection(Long userId) {
        // 기존 연결이 있으면 제거
        SseEmitter existing = connections.remove(userId);
        if (existing != null) {
            try {
                existing.complete();
            } catch (Exception e) {
                log.warn("[SseAlert] 기존 연결 종료 중 오류: userId={}, error={}", userId, e.getMessage());
            }
        }

        // 새 연결 생성 (타임아웃: 30분)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        // 연결 완료 시 정리
        emitter.onCompletion(() -> {
            connections.remove(userId);
            log.debug("[SseAlert] SSE 연결 종료: userId={}", userId);
        });

        // 타임아웃 시 정리
        emitter.onTimeout(() -> {
            connections.remove(userId);
            log.debug("[SseAlert] SSE 연결 타임아웃: userId={}", userId);
        });

        // 에러 발생 시 정리
        emitter.onError((ex) -> {
            connections.remove(userId);
            log.error("[SseAlert] SSE 연결 에러: userId={}, error={}", userId, ex.getMessage(), ex);
        });

        connections.put(userId, emitter);
        log.info("[SseAlert] SSE 연결 생성: userId={}", userId);

        // 초기 연결 확인 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("SSE 연결이 성공적으로 설정되었습니다."));
        } catch (IOException e) {
            log.error("[SseAlert] 초기 메시지 전송 실패: userId={}, error={}", userId, e.getMessage());
            connections.remove(userId);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 알림 전송
     * 
     * @param userId 사용자 ID
     * @param event 알림 이벤트
     */
    public void sendAlert(Long userId, Event event) {
        SseEmitter emitter = connections.get(userId);
        
        if (emitter == null) {
            log.debug("[SseAlert] SSE 연결이 없어 알림 전송 불가: userId={}, eventId={}", userId, event.getId());
            return;
        }

        try {
            // 알림 데이터 전송
            emitter.send(SseEmitter.event()
                .name("alert")
                .data(Map.of(
                    "eventId", event.getId(),
                    "alertEventId", event.getAlertEvent().getId(),
                    "instanceId", event.getInstance().getId(),
                    "severity", event.getSeverity(),
                    "message", event.getMessage(),
                    "currentValue", event.getCurrentValue(),
                    "thresholdValue", event.getThresholdValue(),
                    "createdAt", event.getCreatedAt().toString()
                )));
            
            log.debug("[SseAlert] 알림 전송 완료: userId={}, eventId={}", userId, event.getId());
        } catch (IOException e) {
            log.error("[SseAlert] 알림 전송 실패: userId={}, eventId={}, error={}", userId, event.getId(), e.getMessage());
            // 연결이 끊어진 것으로 간주하고 제거
            connections.remove(userId);
            emitter.completeWithError(e);
        }
    }

    /**
     * 연결 종료
     * 
     * @param userId 사용자 ID
     */
    public void closeConnection(Long userId) {
        SseEmitter emitter = connections.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
                log.info("[SseAlert] SSE 연결 종료: userId={}", userId);
            } catch (Exception e) {
                log.warn("[SseAlert] 연결 종료 중 오류: userId={}, error={}", userId, e.getMessage());
            }
        }
    }

    /**
     * 하트비트 전송 (30초마다)
     * 연결이 살아있는지 확인하기 위한 주기적 메시지 전송
     */
    @Scheduled(fixedRate = 30000) // 30초마다
    public void sendHeartbeat() {
        if (connections.isEmpty()) {
            return;
        }

        connections.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data(":heartbeat"));
            } catch (IOException e) {
                log.debug("[SseAlert] 하트비트 전송 실패 (연결 종료로 간주): userId={}, error={}", userId, e.getMessage());
                // 연결이 끊어진 것으로 간주하고 제거
                connections.remove(userId);
                emitter.completeWithError(e);
            }
        });
    }

    /**
     * 현재 연결된 사용자 수 조회
     */
    public int getConnectionCount() {
        return connections.size();
    }
}

