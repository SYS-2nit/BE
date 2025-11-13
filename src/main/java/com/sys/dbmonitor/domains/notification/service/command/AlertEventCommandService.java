package com.sys.dbmonitor.domains.notification.service.command;

import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.domains.graph.repository.GraphRepository;
import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import com.sys.dbmonitor.domains.notification.domain.AlertPolicy;
import com.sys.dbmonitor.domains.notification.dto.request.AlertEventCreateRequest;
import com.sys.dbmonitor.domains.notification.dto.request.AlertEventUpdateRequest;
import com.sys.dbmonitor.domains.notification.dto.response.AlertEventResponse;
import com.sys.dbmonitor.domains.notification.repository.AlertEventRepository;
import com.sys.dbmonitor.domains.notification.repository.AlertPolicyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlertEventCommandService {

    private final AlertEventRepository alertEventRepository;
    private final AlertPolicyRepository alertPolicyRepository;
    private final GraphRepository graphRepository;

    @Transactional
    public AlertEventResponse create(AlertEventCreateRequest request) {
        AlertPolicy policy = alertPolicyRepository.findById(request.getPolicyId())
            .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + request.getPolicyId()));

        Graph graph = graphRepository.findById(request.getGraphId())
            .orElseThrow(() -> new IllegalArgumentException("Graph not found: " + request.getGraphId()));

        AlertEvent alertEvent = AlertEvent.builder()
            .policy(policy)
            .category(request.getCategory())
            .state(request.getState())
            .name(request.getName())
            .thresholdFormat(request.getThresholdFormat())
            .warning(request.getWarning())
            .danger(request.getDanger())
            .critical(request.getCritical())
            .delayTime(request.getDelayTime())
            .days(request.getDays())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .graph(graph)
            .metricKey(request.getMetricKey())
            .metricName(request.getMetricName())
            .isReverse(request.getIsReverse())
            .build();

        alertEventRepository.save(alertEvent);
        return AlertEventResponse.from(alertEvent);
    }

    @Transactional
    public AlertEventResponse update(Long id, AlertEventUpdateRequest request) {
        AlertEvent alertEvent = alertEventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AlertEvent not found: " + id));

        AlertPolicy policy = null;
        if (request.getPolicyId() != null) {
            policy = alertPolicyRepository.findById(request.getPolicyId())
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + request.getPolicyId()));
        }
        Graph graph = null;

        if (request.getGraphId() != null) {
            graph = graphRepository.findById(request.getGraphId())
                .orElseThrow(() -> new IllegalArgumentException("Graph not found: " + request.getGraphId()));
        }

        alertEvent.updateAlert(
            policy,
            request.getCategory(),
            graph,
            request.getMetricKey(),
            request.getMetricName(),
            request.getName(),
            request.getThresholdFormat(),
            request.getWarning(),
            request.getDanger(),
            request.getCritical(),
            request.getDelayTime(),
            request.getDays(),
            request.getStartTime(),
            request.getEndTime(),
            request.getState(),
            request.getIsReverse()
        );

        return AlertEventResponse.from(alertEvent);
    }

    @Transactional
    public void delete(Long id) {
        AlertEvent alertEvent = alertEventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AlertEvent not found: " + id));
        alertEvent.markAsDeleted();
    }

    @Transactional
    public AlertEventResponse toggle(Long id) {
        AlertEvent alertEvent = alertEventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AlertEvent not found: " + id));
        alertEvent.toggleState();
        return AlertEventResponse.from(alertEvent);
    }
}

