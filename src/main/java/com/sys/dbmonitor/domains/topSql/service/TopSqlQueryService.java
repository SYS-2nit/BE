package com.sys.dbmonitor.domains.topSql.service;

import com.sys.dbmonitor.domains.topSql.dto.TopSqlRowDTO;
import com.sys.dbmonitor.domains.topSql.dto.TopSqlTrendRowDTO;
import com.sys.dbmonitor.domains.topSql.repository.TopSqlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TopSqlQueryService {

    private final TopSqlRepository topSqlRepository;

    public List<TopSqlRowDTO> getTopSql(Long instanceId,
                                        LocalDateTime startTs,
                                        LocalDateTime endTs,
                                        String metric,
                                        int topN) {
        return topSqlRepository.findTopSql(instanceId, startTs, endTs, metric, topN);
    }

    public List<TopSqlTrendRowDTO> getTopSqlTrend(Long instanceId,
                                                  LocalDateTime startTs,
                                                  LocalDateTime endTs,
                                                  String metric,
                                                  int topN) {
        return topSqlRepository.findTopSqlTrend(instanceId, startTs, endTs, metric, topN);
    }
}

