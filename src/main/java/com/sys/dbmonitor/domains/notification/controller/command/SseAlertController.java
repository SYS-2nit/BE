package com.sys.dbmonitor.domains.notification.controller.command;

import com.sys.dbmonitor.domains.notification.service.realtime.SseAlertService;
import com.sys.dbmonitor.global.config.UserIdInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/alerts/sse")
@RequiredArgsConstructor
@Tag(name = "SSE Alert API", description = "실시간 알림 SSE 연결 API")
public class SseAlertController {

    private final SseAlertService sseAlertService;

    /**
     * SSE 연결 생성
     * 사용자 ID는 X-User-ID 헤더에서 자동으로 추출됩니다.
     */
    @Operation(summary = "SSE 연결 생성", description = "실시간 알림을 받기 위한 SSE 연결을 생성합니다. 사용자 ID는 X-User-ID 헤더에서 자동으로 추출됩니다.")
    @GetMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter createConnection(HttpServletResponse response) {
        Long userId = UserIdInterceptor.getCurrentUserId();
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/event-stream;charset=UTF-8");
        return sseAlertService.createConnection(userId);
    }

    /**
     * 연결 종료
     */
    @Operation(summary = "SSE 연결 종료", description = "SSE 연결을 종료합니다. 사용자 ID는 X-User-ID 헤더에서 자동으로 추출됩니다.")
    @DeleteMapping("/stream")
    public void closeConnection() {
        Long userId = UserIdInterceptor.getCurrentUserId();
        sseAlertService.closeConnection(userId);
    }

    /**
     * 현재 연결 수 조회 (테스트용)
     */
    @Operation(summary = "연결 수 조회", description = "현재 연결된 SSE 연결 수를 조회합니다.")
    @GetMapping("/connections/count")
    public int getConnectionCount() {
        return sseAlertService.getConnectionCount();
    }
}


