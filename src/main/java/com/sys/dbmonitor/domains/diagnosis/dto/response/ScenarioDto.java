/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.diagnosis.dto.response;

import com.sys.dbmonitor.domains.diagnosis.domain.ScenarioType;

import java.util.List;

public record ScenarioDto(
        Long id,
        String title,
        String summary,
        List<String> affectedDashboards,
        String reproduction
) {
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
