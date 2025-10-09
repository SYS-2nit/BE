package com.sys.dbmonitor.domains.coupon.controller.v2;

import com.sys.dbmonitor.domains.coupon.domain.Coupon;
import com.sys.dbmonitor.domains.coupon.dto.response.CouponResponse;
import com.sys.dbmonitor.domains.coupon.service.v2.CouponRedisService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController("couponControllerV2")
@RequestMapping("/api/v2/coupons")
@RequiredArgsConstructor
public class CouponController {
    
    private final CouponRedisService couponRedisService;
    
    @PostMapping("/issue")
    public ApiResponse<CouponResponse> issueCoupon() {
        Coupon coupon = couponRedisService.issueCoupon();
        return ApiResponse.ok(201, CouponResponse.from(coupon), "쿠폰이 발급되었습니다.");
    }
}

