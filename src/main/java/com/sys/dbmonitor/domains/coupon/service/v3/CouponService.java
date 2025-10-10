//package com.sys.dbmonitor.domains.coupon.service.v3;
//
//import com.sys.dbmonitor.domains.coupon.dao.CouponRepository;
//import com.sys.dbmonitor.domains.coupon.domain.Coupon;
//import com.sys.dbmonitor.domains.coupon.dto.request.CouponIssueRequest;
//import com.sys.dbmonitor.global.exception.BadRequestException;
//import com.sys.dbmonitor.global.exception.ExceptionMessage;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.redisson.api.RAtomicLong;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//
///**
// * V3: Redis + Kafka를 사용한 비동기 동시성 제어
// *
// * 장점:
// * - 빠른 응답 시간 (비동기 처리)
// * - 높은 처리량
// * - 안정적인 동시성 제어
// * - 이벤트 기반 확장 가능
// *
// * 단점:
// * - 복잡한 아키텍처
// * - Kafka 인프라 필요
// * - 즉시 결과 확인 불가
// */
//@Slf4j
//@Service("couponServiceV3")
//@RequiredArgsConstructor
//public class CouponService {
//
//    private static final String COUPON_QUANTITY_KEY = "coupon:quantity";
//    private static final String COUPON_LOCK_KEY = "coupon:lock";
//    private static final long LOCK_WAIT_TIME = 3L;
//    private static final long LOCK_LEASE_TIME = 5L;
//
//    private final RedissonClient redissonClient;
//    private final CouponRepository couponRepository;
//    private final CouponProducer couponProducer;
//
//    /**
//     * 쿠폰 발급 요청 (비동기)
//     */
//    public void requestCouponIssue(CouponIssueRequest request) {
//        log.info("[V3] 쿠폰 발급 요청 접수 - userId: {}", request.getUserId());
//
//        RLock lock = redissonClient.getLock(COUPON_LOCK_KEY);
//
//        try {
//            boolean isLocked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
//
//            if (!isLocked) {
//                throw new BadRequestException(ExceptionMessage.COUPON_ISSUE_TIMEOUT);
//            }
//
//            // Redis에서 수량 체크 및 감소
//            RAtomicLong atomicQuantity = redissonClient.getAtomicLong(COUPON_QUANTITY_KEY);
//            long remainingQuantity = atomicQuantity.decrementAndGet();
//
//            if (remainingQuantity < 0) {
//                atomicQuantity.incrementAndGet(); // 롤백
//                throw new BadRequestException(ExceptionMessage.COUPON_SOLD_OUT);
//            }
//
//            // Kafka로 쿠폰 발급 메시지 전송
//            couponProducer.sendCouponIssueRequest(request);
//
//            log.info("[V3] 쿠폰 발급 요청 완료 - userId: {}, 남은수량: {}",
//                    request.getUserId(), remainingQuantity);
//
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new BadRequestException(ExceptionMessage.COUPON_ISSUE_FAILED);
//        } finally {
//            if (lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
//        }
//    }
//
//    /**
//     * 실제 쿠폰 발급 (Consumer에서 호출)
//     */
//    @Transactional
//    public void issueCoupon(CouponIssueRequest request) {
//        log.info("[V3] 실제 쿠폰 발급 처리 - userId: {}", request.getUserId());
//
//        Coupon coupon = Coupon.builder()
//                .userId(request.getUserId())
//                .couponCode(generateCouponCode())
//                .status(Coupon.Status.AVAILABLE)
//                .build();
//
//        couponRepository.insertCoupon(coupon);
//
//        log.info("[V3] 쿠폰 발급 완료 - couponId: {}, code: {}",
//                coupon.getCouponId(), coupon.getCouponCode());
//    }
//
//    private String generateCouponCode() {
//        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
//    }
//}
//
