/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class RunOnceAlertCheckRequest {
    private Long instanceId;
    // finals map: metricKey -> value (Double/Number/String)
    private Map<String, Object> finals;
}


