package com.sys.dbmonitor.domains.dashboard.service.command;

import com.sys.dbmonitor.domains.dashboard.domain.MemberWidget;
import com.sys.dbmonitor.domains.dashboard.dto.request.MemberWidgetSaveRequest;
import com.sys.dbmonitor.domains.dashboard.repository.MemberWidgetRepository;
import com.sys.dbmonitor.global.config.UserIdInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 멤버 위젯 설정 저장 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberWidgetCommandService {

    private final MemberWidgetRepository memberWidgetRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "member:widget:";


    public void saveWidgets(MemberWidgetSaveRequest request) {
        Long memberId = UserIdInterceptor.getCurrentUserId();

        // 기존 위젯 소프트 삭제
        memberWidgetRepository.deleteAllByMemberId(memberId);

        // 2. 새로운 위젯 저장
        List<MemberWidget> widgets = new ArrayList<>();
        for (MemberWidgetSaveRequest.WidgetConfig config : request.widgets()) {
            MemberWidget widget = MemberWidget.builder()
                    .memberId(memberId)
                    .graphId(config.graphId())
                    .position(config.position())
                    .build();
            widgets.add(widget);
        }
        memberWidgetRepository.saveAll(widgets);

        // 3. Redis 캐시 무효화
        invalidateCache(memberId);

        log.info("멤버 {}의 위젯 설정 저장 완료: {}개", memberId, widgets.size());
    }

    /**
     * Redis 캐시 무효화
     */
    private void invalidateCache(Long memberId) {
        String cacheKey = REDIS_KEY_PREFIX + memberId;
        redisTemplate.delete(cacheKey);
        log.debug("Redis 캐시 무효화: {}", cacheKey);
    }
}

