//package com.sys.dbmonitor.domains.coupon.service.v3;
//
//import com.sys.dbmonitor.domains.coupon.dto.request.CouponIssueRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//
//@Slf4j
//// @Component  // Kafka 미사용 시 주석 처리
//@RequiredArgsConstructor
//public class CouponConsumer {
//
//    private final CouponService couponService;
//
//    @KafkaListener(
//            topics = "coupon-issue-requests",
//            groupId = "coupon-service",
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void consumeCouponIssueRequest(CouponIssueRequest request) {
//        try {
//            log.info("쿠폰 발급 메시지 수신 - userId: {}", request.getUserId());
//            couponService.issueCoupon(request);
//        } catch (Exception e) {
//            log.error("쿠폰 발급 처리 실패 - userId: {}, error: {}",
//                    request.getUserId(),
//                    e.getMessage());
//        }
//    }
//}
//
