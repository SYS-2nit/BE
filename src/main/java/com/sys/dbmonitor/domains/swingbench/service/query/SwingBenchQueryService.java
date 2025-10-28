package com.sys.dbmonitor.domains.swingbench.service.query;

import com.sys.dbmonitor.domains.swingbench.domain.Scenario;
import com.sys.dbmonitor.domains.swingbench.dto.response.ExecutionResultResponse;
import com.sys.dbmonitor.domains.swingbench.dto.response.ScenarioInfoResponse;
import com.sys.dbmonitor.global.exception.NotFoundException;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SwingBenchQueryService {
    
    private final Map<String, ExecutionResultResponse> executionResults = new ConcurrentHashMap<>();
    
    /**
     * 모든 시나리오 목록 조회
     */
    public List<ScenarioInfoResponse> getAllScenarios() {
        return List.of(
            ScenarioInfoResponse.builder()
                .scenarioId(Scenario.CBC_LATCH.getId())
                .name(Scenario.CBC_LATCH.getName())
                .description("동일한 블록에 대한 동시 읽기로 인한 Latch 경합을 시뮬레이션합니다.")
                .order(Scenario.CBC_LATCH.getOrder())
                .inputFields(Map.of(
                    "hotBlockCount", "핫 블록 수",
                    "concurrentReaders", "동시 읽기 세션 수",
                    "scanIntensity", "스캔 강도 (1-10)",
                    "duration", "테스트 지속 시간 (초)"
                ))
                .build(),
            ScenarioInfoResponse.builder()
                .scenarioId(Scenario.ROW_LOCK_WAIT.getId())
                .name(Scenario.ROW_LOCK_WAIT.getName())
                .description("같은 Row를 업데이트하는 트랜잭션 간 경합을 시뮬레이션합니다.")
                .order(Scenario.ROW_LOCK_WAIT.getOrder())
                .inputFields(Map.of(
                    "contentionRatio", "경합 비율 (%)",
                    "lockHoldTime", "잠금 유지 시간 (초)",
                    "transactionMixUpdate", "Update 트랜잭션 비율 (%)",
                    "transactionMixInsert", "Insert 트랜잭션 비율 (%)",
                    "duration", "테스트 지속 시간 (초)"
                ))
                .build(),
            ScenarioInfoResponse.builder()
                .scenarioId(Scenario.SHARED_POOL.getId())
                .name(Scenario.SHARED_POOL.getName())
                .description("비효율적인 쿼리로 인한 하드 파싱 및 Shared Pool 경합을 시뮬레이션합니다.")
                .order(Scenario.SHARED_POOL.getOrder())
                .inputFields(Map.of(
                    "hardParseRate", "하드 파싱 비율 (%)",
                    "queryComplexity", "쿼리 복잡도 (1-10)",
                    "uniqueSqlRatio", "고유 SQL 비율 (%)",
                    "duration", "테스트 지속 시간 (초)"
                ))
                .build(),
            ScenarioInfoResponse.builder()
                .scenarioId(Scenario.DIRECT_PATH.getId())
                .name(Scenario.DIRECT_PATH.getName())
                .description("대량 데이터 처리 시 버퍼 캐시 우회로 인한 I/O 대기를 시뮬레이션합니다.")
                .order(Scenario.DIRECT_PATH.getOrder())
                .inputFields(Map.of(
                    "tempTablespaceUsage", "임시 테이블스페이스 사용 (%)",
                    "bulkOperationSize", "대량 작업 크기 (rows)",
                    "sortMemorySize", "정렬 메모리 크기 (MB)",
                    "parallelDegree", "병렬도",
                    "duration", "테스트 지속 시간 (초)"
                ))
                .build(),
            ScenarioInfoResponse.builder()
                .scenarioId(Scenario.LOG_FILE_SYNC.getId())
                .name(Scenario.LOG_FILE_SYNC.getName())
                .description("커밋 빈도 과다로 인한 Redo Log 동기화 대기를 시뮬레이션합니다.")
                .order(Scenario.LOG_FILE_SYNC.getOrder())
                .inputFields(Map.of(
                    "transactionRate", "초당 트랜잭션 수",
                    "commitFrequency", "커밋 빈도 (rows마다)",
                    "syncMode", "동기 모드 (AUTO/MANUAL)",
                    "duration", "테스트 지속 시간 (초)"
                ))
                .build()
        );
    }
    
    /**
     * 특정 시나리오 조회
     */
    public ScenarioInfoResponse getScenario(String scenarioId) {
        return getAllScenarios().stream()
            .filter(s -> s.getScenarioId().equals(scenarioId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException(ExceptionMessage.SCENARIO_NOT_FOUND));
    }
    
    /**
     * 실행 상태 조회
     */
    public ExecutionResultResponse getExecutionStatus(String testId) {
        return executionResults.getOrDefault(testId,
            ExecutionResultResponse.builder()
                .testId(testId)
                .status("not_found")
                .resultSummary("실행 기록을 찾을 수 없습니다.")
                .build()
        );
    }
    
    /**
     * 결과 조회
     */
    public ExecutionResultResponse getResult(String testId) {
        return getExecutionStatus(testId);
    }
    
    /**
     * 실행 이력 조회
     */
    public List<ExecutionResultResponse> getExecutionHistory() {
        return executionResults.values().stream()
            .collect(Collectors.toList());
    }
    
    /**
     * QueryService에서 결과 저장 (CommandService에서 사용)
     */
    public void saveExecutionResult(ExecutionResultResponse result) {
        executionResults.put(result.getTestId(), result);
    }
}

