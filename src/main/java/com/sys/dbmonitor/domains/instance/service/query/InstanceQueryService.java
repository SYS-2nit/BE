package com.sys.dbmonitor.domains.instance.service.query;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.dao.InstanceRepository;
import com.sys.dbmonitor.domains.instance.dto.response.InstanceResponse;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstanceQueryService {

    private final InstanceRepository targetDatabaseRepository;

    /**
     * 타겟 DB 목록 조회 (전체)
     */
    @Transactional(readOnly = true)
    public List<InstanceResponse> getAllTargetDatabases() {
        return targetDatabaseRepository.findAll().stream()
                .map(InstanceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 활성화된 타겟 DB 목록 조회
     */
    @Transactional(readOnly = true)
    public List<InstanceResponse> getActiveTargetDatabases() {
        List<Instance> activeTargetDatabases = targetDatabaseRepository.findByIsActive(false)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.DB_NOT_ACTIVE,"활성화 된 DB가 없습니다."));

        return activeTargetDatabases.stream()
                .map(InstanceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 타겟 DB 상세 조회
     */
    @Transactional(readOnly = true)
    public InstanceResponse getTargetDatabase(Long id) {
        Instance targetDatabase = targetDatabaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB를 찾을 수 없습니다."));
        return InstanceResponse.from(targetDatabase);
    }

    /**
     * 이름으로 타겟 DB 조회
     */
    @Transactional(readOnly = true)
    public InstanceResponse getTargetDatabaseByName(String name) {
        Instance targetDatabase = targetDatabaseRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB를 찾을 수 없습니다."));
        return InstanceResponse.from(targetDatabase);
    }
}

