package com.sys.dbmonitor.domains.sql.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "그래프 시리즈 응답(시간 단위, 하루 24포인트)")
public record SqlGraphSeriesResponse(
		String metric,
		String interval, // always "hour"
		List<Point> series
) {
	public record Point(Instant t, long v) { }
}


