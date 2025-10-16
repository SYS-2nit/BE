package com.sys.dbmonitor.domains.coupon.controller.v1;

import com.sys.dbmonitor.domains.coupon.domain.Coupon;
import com.sys.dbmonitor.domains.coupon.dto.response.CouponResponse;
import com.sys.dbmonitor.domains.coupon.service.v1.CouponService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController("couponControllerV1")
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {
    
    private final CouponService couponService;

    @PostMapping("/issue")
    public ApiResponse<CouponResponse> issueCoupon() {
        Coupon coupon = couponService.issueCoupon();
        return ApiResponse.ok(201, CouponResponse.from(coupon), "쿠폰이 발급되었습니다.");
    }

}

