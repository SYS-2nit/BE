package com.sys.dbmonitor.domains.swingbench.controller.query;

import com.sys.dbmonitor.domains.swingbench.domain.Metrics;
import com.sys.dbmonitor.domains.swingbench.service.MetricStorageService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 메트릭 조회 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/swingbench/metrics")
@RequiredArgsConstructor
public class MetricQueryController {
    
    private final MetricStorageService metricStorageService;
    

    @GetMapping("/{testId}")
    public ApiResponse<List<Metrics>> getAllMetrics(@PathVariable String testId) {
        List<Metrics> metrics = metricStorageService.getAllMetrics(testId);
        return ApiResponse.ok(200,metrics,"전체 메트릭 조회 성공");
    }
    

    @GetMapping("/{testId}/start")
    public ApiResponse<Metrics> getStartMetrics(@PathVariable String testId) {
        Metrics metrics = metricStorageService.getStartMetrics(testId);
        return ApiResponse.ok(200,metrics,"시작 시점 매트릭 조회");
    }
    

    @GetMapping("/{testId}/category/{category}")
    public ApiResponse<List<Map<String, Object>>> getCategoryMetrics(
            @PathVariable String testId,
            @PathVariable String category) {
        List<Map<String, Object>> metrics = metricStorageService.extractCategoryMetrics(testId, category);
        return ApiResponse.ok(200,metrics,"카테고리별 메트릭 조회");
    }
}

