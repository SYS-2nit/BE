package com.sys.dbmonitor.domains.sql.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sys.dbmonitor.domains.sql.domain.Sql;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SQL 통계 목록 행")
public record SqlRowResponse(
		Long id,
		String sqlId,
		Long planHashValue,
		String sqlText,
		Long elapsedUsDelta,
		@Schema(description = "Avg Elapsed")
		@JsonProperty("Avg Elapsed")
		Long avgElapsedUs,
		Long waitTimeUsDelta,
		Long executionsDelta,
		Long logicalReads,   // bufferGetsDelta
		Long physicalReads,  // diskReadsDelta
		Long cpuUsDelta
) {
	public static SqlRowResponse from(Sql s) {
		return new SqlRowResponse(
				s.getId(),
				s.getSqlId(),
				s.getPlanHashValue(),
				s.getSqlText(),
				s.getElapsedUsDelta(),
				s.getAvgElapsedUs(),
				s.getWaitTimeUsDelta(),
				s.getExecutionsDelta(),
				s.getBufferGetsDelta(),
				s.getDiskReadsDelta(),
				s.getCpuUsDelta()
		);
	}
}


