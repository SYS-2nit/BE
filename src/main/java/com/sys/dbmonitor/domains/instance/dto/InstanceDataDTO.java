package com.sys.dbmonitor.domains.instance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record InstanceDataDTO(
    String cpuUsage,
    String sessionCount,
    String activeSessionCount,
    String lockWait,
    String pga,
    String sga
) {
}