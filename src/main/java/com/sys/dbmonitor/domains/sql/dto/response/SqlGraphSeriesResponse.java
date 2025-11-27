package com.sys.dbmonitor.domains.sql.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**************************************************
 작성자 : 오수경
 *************************************************/

@Schema(description = "SQL 그래프 데이터 응답")
public record SqlGraphSeriesResponse(
        String metric,
        String startDate,
        String endDate,
        List<Bucket> buckets
) {

    @Schema(description = "시간대 bucket 데이터 (interval 단위)")
    public record Bucket(
            String timeLabel, // 예: "2025-11-10 01:00"
            Long value         // metric 값 합계
    ) {}
}
