package com.sys.dbmonitor.domains.sql.service.command;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.request.SqlCreateRequest;
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


    /** SQL 리스트 */
    @Transactional(readOnly = true)
    public List<SqlResponse> getActiveSqlList() {
        return sqlRepository.findByIsDeletedFalse().stream()
                .map(SqlResponse::from)
                .collect(Collectors.toList());
    }
}
