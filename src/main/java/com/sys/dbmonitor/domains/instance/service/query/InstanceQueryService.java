package com.sys.dbmonitor.domains.instance.service.query;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.instance.dto.response.InstanceResponse;
import com.sys.dbmonitor.global.config.DynamicDataSourceFactory;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstanceQueryService {

    private final InstanceRepository instanceRepository;
    private final DynamicDataSourceFactory dynamicDataSourceFactory;

    /**
     * 타겟 DB 목록 조회 (전체)
     */
    public List<InstanceResponse> getAllTargetDatabases() {
        return instanceRepository.findAll().stream()
                .map(InstanceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 활성화된 타겟 DB 목록 조회
     */
    public List<InstanceResponse> getActiveTargetDatabases() {
        List<Instance> activeTargetDatabases = instanceRepository.findByIsActive(false)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.DB_NOT_ACTIVE,"활성화 된 DB가 없습니다."));

        return activeTargetDatabases.stream()
                .map(InstanceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 타겟 DB 상세 조회
     */
    public InstanceResponse getTargetDatabase(Long id) {
        Instance targetDatabase = instanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB를 찾을 수 없습니다."));
        return InstanceResponse.from(targetDatabase);
    }

    /**
     * 이름으로 타겟 DB 조회
     */

    public InstanceResponse getTargetDatabaseByName(String name) {
        Instance targetDatabase = instanceRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB를 찾을 수 없습니다."));
        return InstanceResponse.from(targetDatabase);
    }

    /**
     * 타겟 DB에서 데이터 조회
     */
    public List<Map<String, Object>> queryTargetDatabase(Long instanceId) {
        // 타겟 DB 존재 확인
        instanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB를 찾을 수 없습니다."));

        // 타겟 DB 데이터소스 가져오기
        DataSource dataSource = dynamicDataSourceFactory.getDataSource(instanceId);
        if (dataSource == null) {
            throw new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB가 연결되지 않았습니다. 먼저 연결해주세요.");
        }

        // JdbcTemplate을 사용하여 쿼리 실행
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        
        // Oracle 테이블 목록 조회 예시
        String sql = "SELECT TABLE_NAME, TABLESPACE_NAME, NUM_ROWS, LAST_ANALYZED " +
                     "FROM USER_TABLES " +
                     "ORDER BY TABLE_NAME";
        
        return jdbcTemplate.queryForList(sql);
    }
}

