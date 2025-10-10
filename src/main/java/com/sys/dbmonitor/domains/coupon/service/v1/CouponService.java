package com.sys.dbmonitor.domains.coupon.service.v1;

import com.sys.dbmonitor.domains.coupon.dao.CouponRepository;
import com.sys.dbmonitor.domains.coupon.domain.Coupon;
import com.sys.dbmonitor.global.config.UserIdInterceptor;
import com.sys.dbmonitor.global.exception.BadRequestException;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * V1: DB PESSIMISTIC LOCK을 사용한 동시성 제어 (프로시저 방식)
 * - COUPON_CONFIG 테이블로 전역 쿠폰 수량 관리
 */
@Slf4j
@Service("couponServiceV1")
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public Coupon issueCoupon() {
        log.info("DB Lock 쿠폰 발급 요청 - userId: {}", UserIdInterceptor.getCurrentUserId());

        // 프로시저 호출을 위한 파라미터 맵 생성
        Map<String, Object> params = new HashMap<>();
        params.put("updatedRows", 0); // OUT 파라미터 초기값

        // 프로시저 호출 (쿠폰 발급 수량 체크 및 증가)
        couponRepository.incrementIssuedCountWithLock(params);

        // OUT 파라미터에서 업데이트된 행 수 확인 (BigDecimal 처리)
        Object updatedRowsObj = params.get("updatedRows");
        int updatedRows = 0;

        if (updatedRowsObj instanceof BigDecimal) {
            updatedRows = ((BigDecimal) updatedRowsObj).intValue();
        } else if (updatedRowsObj instanceof Integer) {
            updatedRows = (Integer) updatedRowsObj;
        } else if (updatedRowsObj instanceof Long) {
            updatedRows = ((Long) updatedRowsObj).intValue();
        }

        log.debug("[V1] 프로시저 실행 결과 - updatedRows: {}", updatedRows);

        if (updatedRows == 0) {
            throw new BadRequestException(ExceptionMessage.COUPON_SOLD_OUT);
        }

        // 쿠폰 생성
        Coupon coupon = Coupon.builder()
                .userId(UserIdInterceptor.getCurrentUserId())
                .couponCode(generateCouponCode())
                .status(Coupon.Status.AVAILABLE)
                .build();

        couponRepository.insertCoupon(coupon);

        log.info("[V1] 쿠폰 발급 완료 - couponId: {}, code: {}",
                coupon.getCouponId(), coupon.getCouponCode());

        return coupon;
    }


    private String generateCouponCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}