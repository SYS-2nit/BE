package com.sys.dbmonitor.domains.topSql.controller;

import com.sys.dbmonitor.domains.topSql.service.SqlSnapshotService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Top SQL 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/top-sql")
@RequiredArgsConstructor
public class TopSqlController {

    private final SqlSnapshotService sqlSnapshotService;

    /**
     * 테스트용 수집 엔드포인트
     * 1분 간격으로 5회 수집 실행
     */
    @GetMapping("/test-collect")
    public ApiResponse<String> testCollect(@RequestParam Long instanceId) {
        log.info("[TopSQL] 테스트 수집 시작: instanceId={}", instanceId);

        for (int i = 1; i <= 30; i++) {
            try {
                log.info("[TopSQL] 수집 실행 중... ({}/5)", i);
                System.out.println("[TopSQL] 수집 실행 중... (" + i + "/5)");
                
                sqlSnapshotService.runOnce(instanceId);
                
                log.info("[TopSQL] 수집 완료: ({}/5)", i);
                System.out.println("[TopSQL] 수집 완료: (" + i + "/5)");

                // 마지막 수집이 아니면 1분 대기
                if (i < 30) {
                    Thread.sleep(60_000); // 1분 = 60초 = 60000ms
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[TopSQL] 수집 중단됨", e);
                return ApiResponse.ok("수집이 중단되었습니다.");
            } catch (Exception e) {
                log.error("[TopSQL] 수집 실패: ({}/5)", i, e);
                System.out.println("[TopSQL] 수집 실패: (" + i + "/5) - " + e.getMessage());
            }
        }

        log.info("[TopSQL] 테스트 수집 완료: instanceId={}", instanceId);
        System.out.println("[TopSQL] 테스트 수집 완료: instanceId=" + instanceId);
        
        return ApiResponse.ok("테스트 수집이 완료되었습니다. (5회 실행)");
    }
}

