package com.sys.dbmonitor.domains.coupon.dao;

import com.sys.dbmonitor.domains.coupon.domain.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;
import java.util.Optional;

@Mapper
public interface CouponRepository {

    // 쿠폰 발급 (INSERT)
    int insertCoupon(Coupon coupon);

    // 쿠폰 ID로 조회
    Optional<Coupon> findById(@Param("couponId") Long couponId);

    // 쿠폰 ID와 사용자 ID로 조회
    Optional<Coupon> findByIdAndUserId(@Param("couponId") Long couponId, @Param("userId") Long userId);

    // 발급된 쿠폰 수 조회
    int countIssuedCoupons();

    // 쿠폰 상태 업데이트
    int updateCouponStatus(Coupon coupon);

    // 쿠폰 발급 카운트 증가 (V1용 - 프로시저 호출)
    void incrementIssuedCountWithLock(Map<String, Object> params);
}