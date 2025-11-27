package com.sys.dbmonitor.domains.sql.service.query;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.response.PlanHistoryListResponse;
import com.sys.dbmonitor.domains.sql.dto.response.PlanHistoryDetailResponse;
import com.sys.dbmonitor.domains.sql.repository.SqlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**************************************************
 작성자 : 오수경
 *************************************************/

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
            Long afterHash,
            String time
    ) {
        LocalDateTime createdAt = LocalDateTime.parse(time);

        // BEFORE row 조회: createdAt 이하 중 가장 최근
        Sql before = sqlRepository
                .findTopBySqlIdAndPlanHashValueAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                        sqlId,
                        beforeHash,
                        createdAt
                )
                .orElse(null);

        // AFTER row 조회: createdAt 이상 중 가장 빠른
        Sql after = sqlRepository
                .findTopBySqlIdAndPlanHashValueAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
                        sqlId,
                        afterHash,
                        createdAt
                )
                .orElse(null);

        return new PlanHistoryDetailResponse(
                beforeHash,
                before != null ? before.getPlanTextClob() : "-- NO PLAN FOUND --",
                afterHash,
                after != null ? after.getPlanTextClob() : "-- NO PLAN FOUND --"
        );
    }
}
