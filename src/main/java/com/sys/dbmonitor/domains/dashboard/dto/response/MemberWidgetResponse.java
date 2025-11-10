package com.sys.dbmonitor.domains.dashboard.dto.response;

import java.util.List;

/**
 * 멤버 위젯 설정 응답
 */
public record MemberWidgetResponse(
        List<WidgetInfo> widgets
) {
    /**
     * 위젯 정보
     */
    public record WidgetInfo(
            Long id,
            Long graphId,
            Integer position
    ) {}
}

