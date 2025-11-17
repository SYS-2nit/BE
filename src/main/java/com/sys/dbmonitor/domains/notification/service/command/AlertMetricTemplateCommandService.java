package com.sys.dbmonitor.domains.notification.service.command;

import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.domains.graph.repository.GraphRepository;
import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.AlertMetricTemplate;
import com.sys.dbmonitor.domains.notification.dto.request.AlertMetricTemplateCreateRequest;
import com.sys.dbmonitor.domains.notification.dto.request.AlertMetricTemplateUpdateRequest;
import com.sys.dbmonitor.domains.notification.dto.response.AlertMetricTemplateResponse;
import com.sys.dbmonitor.domains.notification.repository.AlertMetricTemplateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertMetricTemplateCommandService {

    private final AlertMetricTemplateRepository templateRepository;
    private final GraphRepository graphRepository;

    @Transactional
    public AlertMetricTemplateResponse create(AlertMetricTemplateCreateRequest request) {
        templateRepository.findByMetricKey(request.getMetricKey()).ifPresent(template -> {
            throw new IllegalArgumentException("이미 존재하는 메트릭 키입니다: " + request.getMetricKey());
        });

        Graph graph = graphRepository.findById(request.getGraphId())
            .orElseThrow(() -> new IllegalArgumentException("Graph not found: " + request.getGraphId()));

        AlertMetricTemplate template = AlertMetricTemplate.builder()
            .category(request.getCategory())
            .graph(graph)
            .metricKey(request.getMetricKey())
            .metricName(request.getMetricName())
            .thresholdFormat(request.getThresholdFormat())
            .defaultWarning(request.getDefaultWarning())
            .defaultDanger(request.getDefaultDanger())
            .defaultCritical(request.getDefaultCritical())
            .description(request.getDescription())
            .isActive(request.getIsActive())
            .build();

        templateRepository.save(template);
        return AlertMetricTemplateResponse.from(template);
    }

    @Transactional
    public AlertMetricTemplateResponse update(Long id, AlertMetricTemplateUpdateRequest request) {
        AlertMetricTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));

        Graph graph = null;
        if (request.getGraphId() != null) {
            graph = graphRepository.findById(request.getGraphId())
                .orElseThrow(() -> new IllegalArgumentException("Graph not found: " + request.getGraphId()));
        }

        template.updateTemplate(
            graph,
            request.getCategory(),
            request.getMetricKey(),
            request.getMetricName(),
            request.getThresholdFormat(),
            request.getDefaultWarning(),
            request.getDefaultDanger(),
            request.getDefaultCritical(),
            request.getDescription(),
            request.getIsActive()
        );

        return AlertMetricTemplateResponse.from(template);
    }

    @Transactional
    public void delete(Long id) {
        AlertMetricTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        template.markAsDeleted();
    }

    @Transactional
    public AlertMetricTemplateResponse toggleActive(Long id) {
        AlertMetricTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        template.toggleActive();
        return AlertMetricTemplateResponse.from(template);
    }
}

