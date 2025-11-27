/**************************************************
 작성자 : 오수경
 *************************************************/

package com.sys.dbmonitor.domains.sql.service.query;

import com.sys.dbmonitor.domains.sql.domain.MetricType;
import com.sys.dbmonitor.domains.sql.dto.request.SqlCompareRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlStatsQueryRequest;
import com.sys.dbmonitor.domains.sql.dto.response.SqlComparePageResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlStatsPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

/**
 * SQL 비교 조회 서비스
 */
@Service
@RequiredArgsConstructor
public class SqlCompareService {

    private final SqlStatsService sqlStatsService;

    /**
     * Top SQL 비교 조회
     */
    public SqlComparePageResponse getSqlCompareStats(@Valid SqlCompareRequest req) {
        // base 구간 (하루 기준)
        LocalDate baseStart = req.baseDate();
        LocalDate baseEnd = req.baseDate();

        // compare 구간 (하루 기준)
        LocalDate compStart = req.compareDate();
        LocalDate compEnd = req.compareDate();

        // 기준 구간 SQL 조회
        SqlStatsQueryRequest baseReq = new SqlStatsQueryRequest(
                req.instanceId(),
                baseStart,
                baseEnd,
                MetricType.ELAPSED.getName(),
                "DESC"
        );
        SqlStatsPageResponse base = sqlStatsService.getSqlStats(baseReq);

        // 비교 구간 SQL 조회
        SqlStatsQueryRequest compareReq = new SqlStatsQueryRequest(
                req.instanceId(),
                compStart,
                compEnd,
                MetricType.ELAPSED.getName(),
                "DESC"
        );
        SqlStatsPageResponse compare = sqlStatsService.getSqlStats(compareReq);

        // 비교 결과 응답
        return new SqlComparePageResponse(
                base.content(),
                compare.content()
        );
    }
}

