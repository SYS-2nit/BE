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
     * SQL 데이터 등록 (테스트용 하드코딩)
     */
    @Transactional
    public Sql createSql(SqlCreateRequest request) {
        // 실제 연결 시에는 아래 코드 사용
        /*
        Sql sql = SqlData.builder()
                .instanceId(request.instanceId())
                .field3(request.field3())
                .field4(request.field4())
                .field5(request.field5())
                .isDeleted(false)
                .build();

        Sql saved = sqlRepository.save(sql);
        log.info("[SQL] 데이터 등록 완료: id={}, instanceId={}", saved.getId(), saved.getInstanceId());
        return saved;
        */

        // 지금은 DB 없이 하드코딩된 객체 반환
        Sql dummy = Sql.builder()
                .instanceId(request.instanceId())
                .field3(request.field3())
                .field4(request.field4())
                .field5(request.field5()).build();

        log.info("[SQL] (더미) 데이터 등록 완료: instanceId={}, field3={}", dummy.getInstanceId(), dummy.getField3());
        return dummy;
    }

    /**
     * SQL 데이터 수정
     */
    @Transactional
    public Sql updateSql(Sql sql, SqlCreateRequest request) {
        sql.update(request.field3(), request.field4(), request.field5());
        log.info("[SQL] 데이터 수정 완료: id={}, field3={}", sql.getId(), sql.getField3());
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
