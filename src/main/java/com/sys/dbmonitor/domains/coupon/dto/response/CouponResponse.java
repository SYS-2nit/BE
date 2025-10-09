package com.sys.dbmonitor.domains.coupon.dto.response;

import com.sys.dbmonitor.domains.coupon.domain.Coupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {
    private Long couponId;
    private Long userId;
    private String couponCode;
    private String status;
    private Long orderId;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    
    public static CouponResponse from(Coupon coupon) {
        return CouponResponse.builder()
                .couponId(coupon.getCouponId())
                .userId(coupon.getUserId())
                .couponCode(coupon.getCouponCode())
                .status(coupon.getStatus().name())
                .orderId(coupon.getOrderId())
                .usedAt(coupon.getUsedAt())
                .createdAt(coupon.getCreatedAt())
                .build();
    }
}

