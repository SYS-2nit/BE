package com.sys.dbmonitor.domains.diagnosis.dto.response;

import com.sys.dbmonitor.domains.diagnosis.domain.ScenarioType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioDto {
    private Long id;
    private String title;
    private String summary;
    private List<String> affectedDashboards;
    private String reproduction;

    public static ScenarioDto from(ScenarioType type) {
        return new ScenarioDto(
                type.getId(),
                type.getTitle(),
                type.getSummary(),
                type.getAffectedDashboards(),
                type.getReproduction()
        );
    }
}
