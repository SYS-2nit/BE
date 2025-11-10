package com.sys.dbmonitor.domains.dashboard.service.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sys.dbmonitor.domains.dashboard.domain.MemberWidget;
import com.sys.dbmonitor.domains.dashboard.dto.response.MemberWidgetResponse;
import com.sys.dbmonitor.domains.dashboard.repository.MemberWidgetRepository;
import com.sys.dbmonitor.global.config.UserIdInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 멤버 위젯 설정 조회 서비스 (Redis 캐싱 포함)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MemberWidgetQueryService {

    private final MemberWidgetRepository memberWidgetRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY_PREFIX = "member:widget:";
    private static final long CACHE_TTL_MINUTES = 30; // 30분 캐시 유지

    /**
     * 멤버 위젯 설정 조회
     * 1. Redis 캐시에서 조회 시도
     * 2. 캐시 미스 시 DB에서 조회 후 Redis에 캐싱
     */
    public MemberWidgetResponse getWidgets() {
        try {
            Long memberId = UserIdInterceptor.getCurrentUserId();
            String cacheKey = REDIS_KEY_PREFIX + memberId;

            // 1. Redis 캐시에서 조회 시도
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("Redis 캐시에서 위젯 설정 조회: {}", cacheKey);
                    // StringRedisSerializer 사용 시 JSON 문자열로 저장되므로 파싱 필요
                    if (cached instanceof String) {
                        return objectMapper.readValue((String) cached, MemberWidgetResponse.class);
                    }
                    // GenericJackson2JsonRedisSerializer 사용 시
                    return (MemberWidgetResponse) cached;
                }
            } catch (Exception e) {
                log.warn("Redis 캐시 조회 실패, DB에서 조회: {}", e.getMessage());
            }

            // 2. DB에서 조회
            List<MemberWidget> widgets = memberWidgetRepository.findByMemberIdOrderByPosition(memberId);

            // 3. DTO 변환
            MemberWidgetResponse response = new MemberWidgetResponse(
                    widgets.stream()
                            .map(w -> new MemberWidgetResponse.WidgetInfo(
                                    w.getId(),
                                    w.getGraphId(),
                                    w.getPosition()
                            ))
                            .collect(Collectors.toList())
            );

            // 4. Redis에 캐싱 (JSON 문자열로 변환하여 저장)
            try {
                String jsonValue = objectMapper.writeValueAsString(response);
                redisTemplate.opsForValue().set(
                        cacheKey,
                        jsonValue,
                        CACHE_TTL_MINUTES,
                        TimeUnit.MINUTES
                );
                log.debug("Redis 캐시 저장: {}", cacheKey);
            } catch (Exception e) {
                log.warn("Redis 캐시 저장 실패: {}", e.getMessage());
                // 캐시 저장 실패해도 DB 결과는 반환
            }

            return response;
        } catch (IllegalStateException e) {
            // UserIdInterceptor에서 발생한 예외 (X-User-ID 헤더 없음)
            log.warn("사용자 ID를 가져올 수 없어 빈 위젯 설정을 반환합니다: {}", e.getMessage());
            return new MemberWidgetResponse(List.of());
        }
    }

    /**
     * 그래프 ID 목록 조회 (위젯 설정에 저장된 그래프 순서대로)
     */
    public List<Long> getGraphIds() {
        MemberWidgetResponse response = getWidgets();
        return response.widgets().stream()
                .map(MemberWidgetResponse.WidgetInfo::graphId)
                .collect(Collectors.toList());
    }
}

