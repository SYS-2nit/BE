/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.dto.response;

import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import com.sys.dbmonitor.domains.notification.domain.DelayTime;
import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import com.sys.dbmonitor.domains.notification.support.ThresholdFormatUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "알림 규칙 응답")
public class AlertEventResponse {

    private Long id;
    private Long policyId;
    private String category;
    private String name;
    private Long graphId;
    private String graphName;
    private String metricKey;
    private String metricName;
    private ThresholdFormat thresholdFormat;
    private Double warning;
    private Double danger;
    private Double critical;
    private Double thresholdValue;
    private String formattedWarning;
    private String formattedDanger;
    private String formattedCritical;
    private String formattedThresholdValue;
    private DelayTime delayTime;
    private Integer days;
    private String startTime;
    private String endTime;
    private Boolean state;
    private Boolean isReverse;

    public static AlertEventResponse from(AlertEvent alertEvent) {
        return AlertEventResponse.builder()
            .id(alertEvent.getId())
            .policyId(alertEvent.getPolicy() != null ? alertEvent.getPolicy().getId() : null)
            .category(alertEvent.getCategory() != null ? alertEvent.getCategory().name() : null)
            .name(alertEvent.getName())
            .graphId(alertEvent.getGraph() != null ? alertEvent.getGraph().getId() : null)
            .graphName(alertEvent.getGraph() != null ? alertEvent.getGraph().getName() : null)
            .metricKey(alertEvent.getMetricKey())
            .metricName(alertEvent.getMetricName())
            .thresholdFormat(alertEvent.getThresholdFormat())
            .warning(alertEvent.getWarning())
            .danger(alertEvent.getDanger())
            .critical(alertEvent.getCritical())
            .thresholdValue(alertEvent.getCritical())
            .formattedWarning(ThresholdFormatUtils.formatValue(alertEvent.getWarning(), alertEvent.getThresholdFormat()))
            .formattedDanger(ThresholdFormatUtils.formatValue(alertEvent.getDanger(), alertEvent.getThresholdFormat()))
            .formattedCritical(ThresholdFormatUtils.formatValue(alertEvent.getCritical(), alertEvent.getThresholdFormat()))
            .formattedThresholdValue(ThresholdFormatUtils.formatValue(alertEvent.getCritical(), alertEvent.getThresholdFormat()))
            .delayTime(alertEvent.getDelayTime())
            .days(alertEvent.getDays())
            .startTime(alertEvent.getStartTime())
            .endTime(alertEvent.getEndTime())
            .state(alertEvent.getState())
            .isReverse(alertEvent.getIsReverse())
            .build();
    }
}

