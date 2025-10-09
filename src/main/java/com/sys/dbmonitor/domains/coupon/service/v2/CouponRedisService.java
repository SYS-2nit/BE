package com.sys.dbmonitor.domains.coupon.service.v2;

import com.sys.dbmonitor.domains.coupon.dao.CouponRepository;
import com.sys.dbmonitor.domains.coupon.domain.Coupon;
import com.sys.dbmonitor.global.config.UserIdInterceptor;
import com.sys.dbmonitor.global.exception.BadRequestException;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * V2: Redis Distributed Lock을 사용한 동시성 제어
 * 
 * 장점:
 * - DB 부하 감소
 * - 빠른 락 획득/해제
 * - 분산 환경에서 안전한 동시성 제어
 * 
 * 단점:
 * - Redis 인프라 필요
 * - Redis 장애 시 서비스 영향
 * - 여전히 동기 처리로 응답 시간 증가
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedisService {
    
    private static final String COUPON_QUANTITY_KEY = "coupon:quantity";
    private static final String COUPON_LOCK_KEY = "coupon:lock";
    private static final long LOCK_WAIT_TIME = 3L;
    private static final long LOCK_LEASE_TIME = 5L;
    
    private final RedissonClient redissonClient;
    private final CouponRepository couponRepository;
    
    @Transactional
    public Coupon issueCoupon() {
        log.info("[V2] 쿠폰 발급 요청 - userId: {}", UserIdInterceptor.getCurrentUserId());
        
        RLock lock = redissonClient.getLock(COUPON_LOCK_KEY);
        
        try {
            boolean isLocked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            
            if (!isLocked) {
                throw new BadRequestException(ExceptionMessage.COUPON_ISSUE_TIMEOUT);
            }
            
            // Redis에서 수량 체크 및 감소
            RAtomicLong atomicQuantity = redissonClient.getAtomicLong(COUPON_QUANTITY_KEY);
            long remainingQuantity = atomicQuantity.decrementAndGet();
            
            if (remainingQuantity < 0) {
                atomicQuantity.incrementAndGet(); // 롤백
                throw new BadRequestException(ExceptionMessage.COUPON_SOLD_OUT);
            }
            
            // 쿠폰 생성
            Coupon coupon = Coupon.builder()
                    .userId(UserIdInterceptor.getCurrentUserId())
                    .couponCode(generateCouponCode())
                    .status(Coupon.Status.AVAILABLE)
                    .build();
            
            couponRepository.insertCoupon(coupon);
            
            log.info("[V2] 쿠폰 발급 완료 - couponId: {}, 남은수량: {}", 
                    coupon.getCouponId(), remainingQuantity);
            
            return coupon;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadRequestException(ExceptionMessage.COUPON_ISSUE_FAILED);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    private String generateCouponCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}

