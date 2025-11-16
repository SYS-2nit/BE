package com.sys.dbmonitor.domains.sql.service.query;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.response.PlanHistoryListResponse;
import com.sys.dbmonitor.domains.sql.dto.response.PlanHistoryDetailResponse;
import com.sys.dbmonitor.domains.sql.repository.SqlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanHistoryService {

    private final SqlRepository sqlRepository;

    /**
     * Plan Change Timeline 조회
     */
    public List<PlanHistoryListResponse> getPlanHistoryList(String sqlId) {

        List<Sql> history = sqlRepository.findBySqlIdOrderByCreatedAtAsc(sqlId);
        List<PlanHistoryListResponse> result = new ArrayList<>();

        for (int i = 1; i < history.size(); i++) {

            Sql before = history.get(i - 1);
            Sql after = history.get(i);

            result.add(
                    new PlanHistoryListResponse(
                            after.getCreatedAt().toString(),
                            sqlId,
                            before.getPlanHashValue(),
                            after.getPlanHashValue(),
                            after.getSqlText()
                    )
            );
        }

        return result;
    }

    /**
     * 특정 before/after plan hash 에 대한 상세 조회
     */
    public PlanHistoryDetailResponse getPlanHistoryDetail(
            String sqlId,
            Long beforeHash,
            Long afterHash
    ) {

        Sql before = sqlRepository
                .findTopBySqlIdAndPlanHashValueOrderByCreatedAtDesc(sqlId, beforeHash)
                .orElse(null);

        Sql after = sqlRepository
                .findTopBySqlIdAndPlanHashValueOrderByCreatedAtDesc(sqlId, afterHash)
                .orElse(null);

        return new PlanHistoryDetailResponse(
                beforeHash,
                before != null ? before.getPlanTextClob() : "-- NO PLAN FOUND --",
                afterHash,
                after != null ? after.getPlanTextClob() : "-- NO PLAN FOUND --"
        );
    }
}
