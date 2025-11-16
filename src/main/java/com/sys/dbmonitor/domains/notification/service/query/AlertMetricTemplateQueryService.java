package com.sys.dbmonitor.domains.notification.service.query;

import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.AlertMetricTemplate;
import com.sys.dbmonitor.domains.notification.dto.response.AlertMetricTemplateResponse;
import com.sys.dbmonitor.domains.notification.repository.AlertMetricTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 메트릭 템플릿 조회 서비스
 * - 활성화된 템플릿 목록 조회
 * - 카테고리별 템플릿 조회
 * - 단일 템플릿 상세 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertMetricTemplateQueryService {

    private final AlertMetricTemplateRepository templateRepository;

    /**
     * 모든 활성화된 메트릭 템플릿 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AlertMetricTemplateResponse> getActiveTemplates() {
        List<AlertMetricTemplate> templates = templateRepository.findAllActive();
        return templates.stream()
                .map(AlertMetricTemplateResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 카테고리의 활성화된 메트릭 템플릿 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AlertMetricTemplateResponse> getActiveTemplatesByCategory(AlertCategory category) {
        List<AlertMetricTemplate> templates = templateRepository.findByCategoryAndActive(category);
        return templates.stream()
                .map(AlertMetricTemplateResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 단일 메트릭 템플릿 상세 조회
     */
    @Transactional(readOnly = true)
    public AlertMetricTemplateResponse getTemplate(Long id) {
        AlertMetricTemplate template = templateRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("AlertMetricTemplate not found: " + id));
        return AlertMetricTemplateResponse.from(template);
    }
}

