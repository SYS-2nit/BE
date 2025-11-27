/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.dto.response;

import com.sys.dbmonitor.domains.notification.domain.AlertMetricTemplate;
import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "알림 메트릭 템플릿 응답")
public class AlertMetricTemplateResponse {

    private Long id;
    private String category;
    private Long graphId;
    private String graphName;
    private String metricKey;
    private String metricName;
    private ThresholdFormat thresholdFormat;
    private Double defaultWarning;
    private Double defaultDanger;
    private Double defaultCritical;
    private String description;
    private Boolean isActive;

    public static AlertMetricTemplateResponse from(AlertMetricTemplate template) {
        return AlertMetricTemplateResponse.builder()
            .id(template.getId())
            .category(template.getCategory() != null ? template.getCategory().name() : null)
            .graphId(template.getGraph() != null ? template.getGraph().getId() : null)
            .graphName(template.getGraph() != null ? template.getGraph().getName() : null)
            .metricKey(template.getMetricKey())
            .metricName(template.getMetricName())
            .thresholdFormat(template.getThresholdFormat())
            .defaultWarning(template.getDefaultWarning())
            .defaultDanger(template.getDefaultDanger())
            .defaultCritical(template.getDefaultCritical())
            .description(template.getDescription())
            .isActive(template.getIsActive())
            .build();
    }
}

