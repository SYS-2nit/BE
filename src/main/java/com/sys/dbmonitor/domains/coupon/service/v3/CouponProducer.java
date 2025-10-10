//package com.sys.dbmonitor.domains.coupon.service.v3;
//
//import com.sys.dbmonitor.domains.coupon.dto.request.CouponIssueRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class CouponProducer {
//
//    private static final String TOPIC = "coupon-issue-requests";
//
//    private final KafkaTemplate<String, CouponIssueRequest> kafkaTemplate;
//
//    public void sendCouponIssueRequest(CouponIssueRequest request) {
//        kafkaTemplate.send(TOPIC, String.valueOf(request.getUserId()), request)
//                .whenComplete((result, ex) -> {
//                    if (ex == null) {
//                        log.info("쿠폰 발급 메시지 전송 성공 - userId: {}, offset: {}",
//                                request.getUserId(),
//                                result.getRecordMetadata().offset());
//                    } else {
//                        log.error("쿠폰 발급 메시지 전송 실패 - userId: {}, error: {}",
//                                request.getUserId(),
//                                ex.getMessage());
//                    }
//                });
//    }
//}
//
