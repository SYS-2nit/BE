package com.sys.dbmonitor.domains.coupon.controller;

import com.sys.dbmonitor.domains.coupon.service.CouponLoadTestService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 쿠폰 부하 테스트 컨트롤러
 * JMeter 없이 간편하게 동시성 테스트
 */
@Slf4j
@RestController
@RequestMapping("/api/test/coupon")
@RequiredArgsConstructor
public class CouponTestController {
    
    private final CouponLoadTestService couponLoadTestService;
    
    /**
     * 동시성 부하 테스트 실행
     * 
     * @param apiVersion API 버전 (v1, v2, v3)
     * @param threads 동시 스레드 수 (예: 100)
     * @param requests 총 요청 수 (예: 1000)
     */
    @PostMapping("/load-test")
    public ApiResponse<CouponLoadTestService.LoadTestResult> runLoadTest(
            @RequestParam(defaultValue = "v1") String apiVersion,
            @RequestParam(defaultValue = "100") int threads,
            @RequestParam(defaultValue = "1000") int requests
    ) {
        log.info("═══ 부하 테스트 API 호출 - version: {}, threads: {}, requests: {}", 
                apiVersion, threads, requests);
        
        CouponLoadTestService.LoadTestResult result = 
                couponLoadTestService.runLoadTest(apiVersion, threads, requests);
        
        return ApiResponse.ok(200, result, "부하 테스트 완료");
    }

}

