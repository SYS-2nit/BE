package com.sys.dbmonitor.domains.topSql.controller;

import com.sys.dbmonitor.domains.topSql.dto.TopSqlRowDTO;
import com.sys.dbmonitor.domains.topSql.dto.TopSqlTrendRowDTO;
import com.sys.dbmonitor.domains.topSql.service.SqlSnapshotService;
import com.sys.dbmonitor.domains.topSql.service.TopSqlQueryService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Top SQL 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/top-sql")
@RequiredArgsConstructor
public class TopSqlController {

    private final SqlSnapshotService sqlSnapshotService;
    private final TopSqlQueryService topSqlQueryService;

    /**
     * 테스트용 수집 엔드포인트
     * 1분 간격으로 5회 수집 실행
     */
    @GetMapping("/test-collect")
    public ApiResponse<String> testCollect(@RequestParam Long instanceId) {
        log.info("[TopSQL] 테스트 수집 시작: instanceId={}", instanceId);

        for (int i = 1; i <= 48; i++) {
            try {
                log.info("[TopSQL] 수집 실행 중... ({}/{})", i, 48);
                System.out.println("[TopSQL] 수집 실행 중... (" + i + "/48)");

                sqlSnapshotService.runOnce(instanceId);

                log.info("[TopSQL] 수집 완료: ({}/{})", i, 48);
                System.out.println("[TopSQL] 수집 완료: (" + i + "/48)");

                if (i < 48) {
                    Thread.sleep(30 * 60_000);   // 30분
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[TopSQL] 수집 중단됨", e);
                return ApiResponse.ok("수집이 중단되었습니다.");
            } catch (Exception e) {
                log.error("[TopSQL] 수집 실패: ({}/{})", i, 48, e);
                System.out.println("[TopSQL] 수집 실패: (" + i + "/48) - " + e.getMessage());
            }
        }

        log.info("[TopSQL] 테스트 수집 완료: instanceId={}", instanceId);
        System.out.println("[TopSQL] 테스트 수집 완료: instanceId=" + instanceId);

        return ApiResponse.ok("테스트 수집이 완료되었습니다. (48회 실행)");
    }

    /**
     * 테스트용 Top SQL 표 조회
     */
    @GetMapping("/top-list")
    public ApiResponse<List<TopSqlRowDTO>> getTopSqlList(@RequestParam Long instanceId,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTs,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTs,
                                                         @RequestParam(defaultValue = "ELAPSED") String metric,
                                                         @RequestParam(defaultValue = "10") int topN) {
        List<TopSqlRowDTO> rows = topSqlQueryService.getTopSql(instanceId, startTs, endTs, metric, topN);
        return ApiResponse.ok(rows);
    }

    /**
     * 테스트용 Top SQL 추이 데이터 조회
     */
    @GetMapping("/top-trend")
    public ApiResponse<List<TopSqlTrendRowDTO>> getTopSqlTrend(@RequestParam Long instanceId,
                                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTs,
                                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTs,
                                                               @RequestParam(defaultValue = "ELAPSED") String metric,
                                                               @RequestParam(defaultValue = "10") int topN) {
        List<TopSqlTrendRowDTO> trend = topSqlQueryService.getTopSqlTrend(instanceId, startTs, endTs, metric, topN);
        return ApiResponse.ok(trend);
    }
}
