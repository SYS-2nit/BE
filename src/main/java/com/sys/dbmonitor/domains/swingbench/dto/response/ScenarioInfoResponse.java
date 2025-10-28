package com.sys.dbmonitor.domains.swingbench.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioInfoResponse {
    private String scenarioId;
    private String name;
    private String description;
    private Integer order;
    private Map<String, String> inputFields; // 입력 필드 설명
}

