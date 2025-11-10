package com.sys.dbmonitor.domains.dashboard.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.List;


public record MemberWidgetSaveRequest(
        @NotNull(message = "그래프 ID 목록은 필수입니다.")
        List<WidgetConfig> widgets
) {

    public record WidgetConfig(
            @NotNull(message = "그래프 ID는 필수입니다.")
            Long graphId,

            @NotNull(message = "위치는 필수입니다.")
            @Min(value = 1, message = "위치는 1 이상이어야 합니다.")
            @Max(value = 9, message = "위치는 9 이하여야 합니다.")
            Integer position
    ) {}
}

