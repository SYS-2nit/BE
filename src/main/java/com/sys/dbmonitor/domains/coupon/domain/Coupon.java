package com.sys.dbmonitor.domains.coupon.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    public enum Status {
        AVAILABLE,   // 사용 가능
        USED,        // 사용됨
        EXPIRED,     // 만료됨
        CANCELLED    // 취소됨
    }

    private Long couponId;
    private Long userId;
    private String couponCode;
    private Status status;
    private Long orderId;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 쿠폰 사용
    public void use(Long orderId) {
        this.status = Status.USED;
        this.orderId = orderId;
        this.usedAt = LocalDateTime.now();
    }

    // 쿠폰 취소
    public void cancel() {
        this.status = Status.CANCELLED;
        this.orderId = null;
        this.usedAt = null;
    }
}