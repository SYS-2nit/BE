package com.sys.dbmonitor.domains.sql.service.command;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.request.SqlCreateRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SqlCommandService {

    private static final Logger log = LoggerFactory.getLogger(SqlCommandService.class);

    // 🔒 DB 연결 후 주석 해제 예정
    // private final SqlRepository sqlRepository;

    /**
     * SQL 데이터 등록 (현재는 더미 생성 후 반환)
     */
    @Transactional
    public Sql createSql(SqlCreateRequest request) {
        Sql sql = Sql.builder()
                .instanceId(request.instanceId())
                .sqlId(request.sqlId())
                .planHashValue(request.planHashValue())
                .bufferGetsDelta(request.bufferGetsDelta())
                .cpuUsDelta(request.cpuUsDelta())
                .diskReadsDelta(request.diskReadsDelta())
                .elapsedUsDelta(request.elapsedUsDelta())
                .executionsDelta(request.executionsDelta())
                .waitTimeUsDelta(request.waitTimeUsDelta())
                .waitUserIoUsDelta(null)
                .waitConcurrencyUsDelta(null)
                .waitApplicationUsDelta(null)
                .waitClusterUsDelta(null)
                .waitPlsqlUsDelta(null)
                .waitJavaUsDelta(null)
                .sqlText(request.sqlText())
                .build();
        log.info("[SQL] (더미) 데이터 생성: instanceId={}, sqlId={}, textLen={}",
                sql.getInstanceId(), sql.getSqlId(),
                sql.getSqlText() != null ? sql.getSqlText().length() : 0);
        return sql;
    }

    /**
     * SQL 데이터 수정
     */
    @Transactional
    public Sql updateSql(Sql sql, SqlCreateRequest request) {
        sql.updateSqlText(request.sqlText());
        log.info("[SQL] 데이터 수정 완료: id={}, sqlId={}", sql.getId(), sql.getSqlId());
        return sql;
    }

    /**
     * SQL 데이터 삭제 (soft delete)
     */
    @Transactional
    public void deleteSql(Sql sql) {
        sql.delete();
        log.info("[SQL] 데이터 삭제 완료: id={}", sql.getId());
    }
}
