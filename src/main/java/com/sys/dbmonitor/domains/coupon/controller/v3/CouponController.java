package com.sys.dbmonitor.domains.coupon.controller.v3;

import com.sys.dbmonitor.domains.coupon.dto.request.CouponIssueRequest;
import com.sys.dbmonitor.domains.coupon.service.v3.CouponService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// @RestController("couponControllerV3")  // Kafka 미사용 시 주석 처리
@RequestMapping("/api/v3/coupons")
@RequiredArgsConstructor
public class CouponController {
    
    private final CouponService couponService;
    
    @PostMapping("/issue")
    public ApiResponse<Void> issueCoupon(@RequestBody CouponIssueRequest request) {
        couponService.requestCouponIssue(request);
        return ApiResponse.ok(202, null, "쿠폰 발급 요청이 접수되었습니다.");
    }
}

