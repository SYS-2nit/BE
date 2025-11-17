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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

        // 위치 중복 방지
        validateDuplicatePositions(request);

        // 기존 위젯 조회 (소프트 삭제되지 않은 항목만)
        List<MemberWidget> existingWidgets = memberWidgetRepository.findByMemberIdOrderByPosition(memberId);
        Map<Integer, MemberWidget> widgetByPosition = existingWidgets.stream()
                .collect(Collectors.toMap(MemberWidget::getPosition, widget -> widget));

        List<MemberWidget> newWidgets = new ArrayList<>();

        for (MemberWidgetSaveRequest.WidgetConfig config : request.widgets()) {
            Integer position = config.position();
            MemberWidget widget = widgetByPosition.get(position);

            if (widget != null) {
                widget.updateGraph(config.graphId());
            } else {
                MemberWidget newWidget = MemberWidget.builder()
                        .memberId(memberId)
                        .graphId(config.graphId())
                        .position(position)
                        .build();
                newWidgets.add(newWidget);
                widgetByPosition.put(position, newWidget);
            }
        }

        if (!newWidgets.isEmpty()) {
            memberWidgetRepository.saveAll(newWidgets);
        }

        // 3. Redis 캐시 무효화
        invalidateCache(memberId);

        log.info("멤버 {}의 위젯 설정 저장 완료: 업데이트={}, 신규={}", memberId,
                request.widgets().size() - newWidgets.size(), newWidgets.size());
    }

    /**
     * Redis 캐시 무효화
     */
    private void invalidateCache(Long memberId) {
        try {
            String cacheKey = REDIS_KEY_PREFIX + memberId;
            redisTemplate.delete(cacheKey);
            log.debug("Redis 캐시 무효화: {}", cacheKey);
        } catch (Exception e) {
            // Redis 연결 실패 등 예외 발생 시 로그만 남기고 계속 진행
            // 캐시 무효화 실패는 DB 저장에 영향을 주지 않아야 함
            log.warn("Redis 캐시 무효화 실패 (무시): {}", e.getMessage());
        }
    }

    private void validateDuplicatePositions(MemberWidgetSaveRequest request) {
        Set<Integer> positions = new HashSet<>();
        for (MemberWidgetSaveRequest.WidgetConfig widget : request.widgets()) {
            if (!positions.add(widget.position())) {
                throw new IllegalArgumentException("위젯 위치가 중복되었습니다: " + widget.position());
            }
        }

        if (positions.size() > 9) {
            throw new IllegalArgumentException("위젯 위치는 최대 9개까지 허용됩니다.");
        }
    }
}

