package com.sys.dbmonitor.domains.sql.service.command;

<<<<<<< HEAD
import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.request.SqlCreateRequest;
=======
>>>>>>> dev
import com.sys.dbmonitor.domains.sql.dto.response.SqlResponse;
import com.sys.dbmonitor.domains.sql.repository.SqlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SqlCommandService {

    private final SqlRepository sqlRepository;

<<<<<<< HEAD
    /** SQL 등록 */
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
                .waitUserIoUsDelta(request.waitUserIoUsDelta())
                .waitConcurrencyUsDelta(request.waitConcurrencyUsDelta())
                .waitApplicationUsDelta(request.waitApplicationUsDelta())
                .waitClusterUsDelta(request.waitClusterUsDelta())
                .waitPlsqlUsDelta(request.waitPlsqlUsDelta())
                .waitJavaUsDelta(request.waitJavaUsDelta())
                .sqlText(request.sqlText())
                .build();
        return sqlRepository.save(sql);
    }

    /** SQL 수정 */
    public Sql updateSql(Long id, SqlCreateRequest request) {
        Sql sql = sqlRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 SQL 데이터가 존재하지 않습니다. id=" + id));

        Sql updated = Sql.builder()
                .sqlText(request.sqlText())
                .cpuUsDelta(request.cpuUsDelta())
                .elapsedUsDelta(request.elapsedUsDelta())
                .executionsDelta(request.executionsDelta())
                .build();

        sql.updateFrom(updated);
        return sqlRepository.save(sql);
    }
=======
>>>>>>> dev

    /** SQL 리스트 */
    @Transactional(readOnly = true)
    public List<SqlResponse> getActiveSqlList() {
        return sqlRepository.findByIsDeletedFalse().stream()
                .map(SqlResponse::from)
                .collect(Collectors.toList());
    }
<<<<<<< HEAD

    /** SQL 삭제 */
    public void deleteSql(Long id) {
        Sql sql = sqlRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 SQL 데이터가 존재하지 않습니다. id=" + id));
        sql.softDelete();
        sqlRepository.save(sql);
    }
=======
>>>>>>> dev
}
