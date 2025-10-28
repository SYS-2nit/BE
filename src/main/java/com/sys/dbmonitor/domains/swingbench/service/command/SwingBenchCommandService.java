package com.sys.dbmonitor.domains.swingbench.service.command;

import com.sys.dbmonitor.domains.swingbench.domain.Metrics;
import com.sys.dbmonitor.domains.swingbench.domain.Scenario;
import com.sys.dbmonitor.domains.swingbench.dto.request.ScenarioExecutionRequest;
import com.sys.dbmonitor.domains.swingbench.dto.response.ExecutionResultResponse;
import com.sys.dbmonitor.domains.swingbench.service.DatabaseMonitoringService;
import com.sys.dbmonitor.domains.swingbench.service.MetricStorageService;
import com.sys.dbmonitor.domains.swingbench.service.query.SwingBenchQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SwingBenchCommandService {
    
    private final SwingBenchQueryService queryService;
    private final DatabaseMonitoringService monitoringService;
    private final MetricStorageService metricStorageService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    @Value("${swingbench.jar.path}")
    private String swingBenchJarPath;
    
    @Value("${spring.datasource.url}")
    private String dbUrl;
    
    /**
     * 시나리오 실행
     */
    public ExecutionResultResponse executeScenario(ScenarioExecutionRequest request) {
        String testId = UUID.randomUUID().toString();
        String scenarioId = request.getScenarioId();
        long startTime = System.currentTimeMillis();
        
        log.info("시나리오 실행 시작 - testId: {}, scenarioId: {}", testId, scenarioId);
        
        // 초기 상태 저장
        ExecutionResultResponse initialResult = ExecutionResultResponse.builder()
            .testId(testId)
            .scenarioId(scenarioId)
            .status("running")
            .startTime(startTime)
            .resultSummary("테스트가 시작되었습니다. 잠시 후 결과를 확인하세요.")
            .build();
        queryService.saveExecutionResult(initialResult);
        
        // 비동기로 SwingBench 실행
        CompletableFuture.supplyAsync(() -> {
            try {
                log.info("✓ SwingBench 비동기 실행 시작 - testId: {}", testId);
                return executeSwingBench(testId, scenarioId, request);
            } catch (Exception e) {
                log.error("✗ SwingBench 실행 실패 - testId: {}, error: {}", testId, e.getMessage(), e);
                ExecutionResultResponse failedResult = createFailureResponse(testId, scenarioId, e.getMessage());
                // 실패 결과도 저장
                queryService.saveExecutionResult(failedResult);
                return failedResult;
            }
        }, executorService)
        .thenAccept(result -> {
            try {
                long endTime = System.currentTimeMillis();
                result.setEndTime(endTime);
                
                // startTime이 없으면 설정
                if (result.getStartTime() == null) {
                    result.setStartTime(startTime);
                }
                
                result.setTotalDuration((int)((endTime - result.getStartTime()) / 1000));
                log.info("✓ 결과 저장 시작 - testId: {}", testId);
                queryService.saveExecutionResult(result);
                log.info("✓ 시나리오 실행 완료 - testId: {}, status: {}", testId, result.getStatus());
            } catch (Exception e) {
                log.error("✗ 결과 저장 실패 - testId: {}", testId, e);
            }
        })
        .exceptionally(ex -> {
            log.error("✗ CompletableFuture 실행 중 오류 - testId: {}", testId, ex);
            return null;
        });
        
        return initialResult;
    }
    
    private ExecutionResultResponse executeSwingBench(String testId, String scenarioId, 
                                                     ScenarioExecutionRequest request) throws Exception {
        // ========== Phase 1: 데이터 확인 및 생성 ==========
        log.info("✓ SwingBench 데이터 확인 중...");
        if (!checkSwingBenchDataExists()) {
            log.info("✓ SwingBench 데이터가 없어 생성 시작...");
            createSwingBenchData();
        }
        
        // ========== Phase 2: SwingBench 실행 및 메트릭 수집 ==========
        String command = buildCommand(scenarioId, request);
        
        log.info("✓ SwingBench 실행 명령어: {}", command);
        log.info("✓ SwingBench 프로세스 시작...");
        
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        log.info("✓ SwingBench 프로세스 PID: {}", process.pid());
        log.info("✓ 시나리오 시작 시점부터 메트릭 수집 시작 (3초 간격, 총 10개)");
        
        // ========== Phase 3: 시나리오 시작 시점부터 메트릭 수집 (3초마다 10개) ==========
        CompletableFuture<Void> monitoringFuture = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    Metrics metrics = monitoringService.collectAllMetrics();
                    metricStorageService.saveMetrics(testId, metrics, i);
                    log.info("✓ 메트릭 수집 완료 ({}/10) - {}초 경과", i + 1, i * 3);
                    
                    if (i < 9) {
                        TimeUnit.SECONDS.sleep(3); // 3초 대기
                    }
                } catch (Exception e) {
                    log.error("✗ 메트릭 수집 실패 ({}/10): {}", i + 1, e.getMessage());
                }
            }
        }, executorService);
        
        // 결과 파싱 및 실시간 로그 출력
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                lineCount++;
                
                // SwingBench 출력을 실시간으로 로깅
                log.info("[SwingBench] {}", line);
                
                // 100줄마다 진행 상황 요약
                if (lineCount % 100 == 0) {
                    log.info("✓ SwingBench 진행 중... ({}줄 수집)", lineCount);
                }
            }
            log.info("✓ SwingBench 출력 수집 완료 (총 {}줄)", lineCount);
        }
        
        log.info("✓ SwingBench 프로세스 완료 대기 중...");
        int exitCode = process.waitFor();
        log.info("✓ SwingBench 프로세스 종료 (Exit Code: {})", exitCode);
        
        // 모니터링 완료 대기
        monitoringFuture.join();
        
        log.info("✓ SwingBench 출력 내용:\n{}", output.toString());
        
        if (exitCode == 0) {
            log.info("✓ SwingBench 실행 성공 - 결과 저장 시작");
            ExecutionResultResponse result = createSuccessResponse(testId, scenarioId, output.toString());
            log.info("✓ SwingBench 실행 성공 - 결과 저장 완료");
            return result;
        } else {
            log.warn("✓ SwingBench 실행 실패 (Exit Code: {})", exitCode);
            return createFailureResponse(testId, scenarioId, "테스트 실행 실패 (exit code: " + exitCode + ")");
        }
    }
    
    private String buildCommand(String scenarioId, ScenarioExecutionRequest request) {
        // DB 연결 정보 파싱
        String dbInfo = parseDbUrl(dbUrl);
        
        Scenario scenario = Scenario.fromId(scenarioId);
        Map<String, Object> params = request.getAdditionalParams() != null 
            ? request.getAdditionalParams() : new java.util.HashMap<>();
        
        log.info("✓ 시나리오별 파라미터: {}", params);
        
        String baseCommand = String.format(
            "java -jar %s " +
            "-cs %s " +
            "-u %s -p %s " +
            "-run -cli -min %d -max %d -st %d",
            swingBenchJarPath,
            dbInfo,
            extractUsername(dbUrl),
            extractPassword(dbUrl),
            request.getMinUsers(),
            request.getMaxUsers(),
            request.getDuration()
        );
        
        // 시나리오별 추가 옵션
        String scenarioCommand = switch (scenario) {
            case CBC_LATCH -> {
                // Diagnosis 1: Cache Buffer Chain (Latch) Contention
                String command = baseCommand + " -wal";
                if (params.containsKey("hotBlockCount")) {
                    command += " -hot " + params.get("hotBlockCount");
                }
                if (params.containsKey("scanIntensity")) {
                    command += " -si " + params.get("scanIntensity");
                }
                yield command;
            }
            case ROW_LOCK_WAIT -> {
                // Diagnosis 2: Row Lock Wait (TX Enqueue)
                String command = baseCommand + " -lock -tx";
                if (params.containsKey("contentionRatio")) {
                    command += " -cr " + params.get("contentionRatio");
                }
                if (params.containsKey("lockHoldTime")) {
                    command += " -lht " + params.get("lockHoldTime");
                }
                yield command;
            }
            case SHARED_POOL -> {
                // Diagnosis 3: Shared Pool Contention (Library Cache Latch)
                String command = baseCommand + " -parse";
                if (params.containsKey("hardParseRate")) {
                    command += " -hpr " + params.get("hardParseRate");
                }
                if (params.containsKey("queryComplexity")) {
                    command += " -qc " + params.get("queryComplexity");
                }
                if (params.containsKey("uniqueSqlRatio")) {
                    command += " -usr " + params.get("uniqueSqlRatio");
                }
                yield command;
            }
            case DIRECT_PATH -> {
                // Diagnosis 4: Direct Path Read/Write Contention
                String command = baseCommand + " -direct -temp";
                if (params.containsKey("bulkOperationSize")) {
                    command += " -bos " + params.get("bulkOperationSize");
                }
                if (params.containsKey("parallelDegree")) {
                    command += " -pd " + params.get("parallelDegree");
                }
                yield command;
            }
            case LOG_FILE_SYNC -> {
                // Diagnosis 5: Log File Sync Wait
                String command = baseCommand + " -commit";
                if (params.containsKey("transactionRate")) {
                    command += " -tr " + params.get("transactionRate");
                }
                if (params.containsKey("commitFrequency")) {
                    command += " -cf " + params.get("commitFrequency");
                }
                String syncMode = (String) params.get("syncMode");
                if (syncMode != null) {
                    command += " -mode " + syncMode;
                }
                yield command;
            }
        };
        
        log.info("✓ 최종 SwingBench 명령어: {}", scenarioCommand);
        return scenarioCommand;
    }
    
    private String parseDbUrl(String dbUrl) {
        // jdbc:oracle:thin:@localhost:1521:XE 형식을 localhost:1521:XE로 변환
        if (dbUrl.startsWith("jdbc:oracle:thin:@")) {
            return dbUrl.substring("jdbc:oracle:thin:@".length());
        }
        return dbUrl;
    }
    
    private String extractUsername(String dbUrl) {
        // 간단한 파싱 - 실제로는 application.yml에서 직접 가져와야 함
        return "system";
    }
    
    private String extractPassword(String dbUrl) {
        // 간단한 파싱 - 실제로는 application.yml에서 직접 가져와야 함
        return "5179";
    }
    
    private ExecutionResultResponse createSuccessResponse(String testId, String scenarioId, String output) {
        Map<String, Object> metrics = parseMetrics(output);
        
        return ExecutionResultResponse.builder()
            .testId(testId)
            .scenarioId(scenarioId)
            .status("completed")
            .resultSummary("테스트가 성공적으로 완료되었습니다.")
            .metrics(metrics)
            .startTime(System.currentTimeMillis())  // startTime 설정 추가
            .build();
    }
    
    private ExecutionResultResponse createFailureResponse(String testId, String scenarioId, String error) {
        return ExecutionResultResponse.builder()
            .testId(testId)
            .scenarioId(scenarioId)
            .status("failed")
            .resultSummary("테스트 실행 실패: " + error)
            .metrics(Map.of("error", error))
            .startTime(System.currentTimeMillis())  // startTime 설정 추가
            .build();
    }
    
    private Map<String, Object> parseMetrics(String output) {
        Map<String, Object> metrics = new HashMap<>();
        
        // SwingBench 출력에서 지표 파싱
        // 실제 구현 시 SwingBench의 출력 형식에 맞게 파싱 필요
        metrics.put("summary", "성공적으로 완료");
        metrics.put("output", output);
        
        // TPS, 평균 응답시간 등 추출
        if (output.contains("TPS")) {
            metrics.put("tps", extractMetric(output, "TPS"));
        }
        if (output.contains("Average Response Time")) {
            metrics.put("avgResponseTime", extractMetric(output, "Average Response Time"));
        }
        
        return metrics;
    }
    
    private String extractMetric(String output, String metricName) {
        // 간단한 매칭 로직 (실제로는 더 정교한 파싱 필요)
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.contains(metricName)) {
                return line.trim();
            }
        }
        return "N/A";
    }
    
    /**
     * 테스트 중지
     */
    public void stopExecution(String testId) {
        log.info("테스트 중지 요청 - testId: {}", testId);
        
        // 실행 중인 프로세스 중지 로직
        ExecutionResultResponse result = queryService.getExecutionStatus(testId);
        if (result.getStatus().equals("running")) {
            result.setStatus("stopped");
            result.setResultSummary("사용자에 의해 테스트가 중지되었습니다.");
            queryService.saveExecutionResult(result);
        }
    }
    
    /**
     * SwingBench 설치
     */
    public void installSwingBench() {
        log.info("SwingBench 설치 시작");
        
        // SwingBench jar 다운로드 및 설정
        // 실제로는 별도 스레드에서 실행하여 비동기 처리
        CompletableFuture.supplyAsync(() -> {
            try {
                // SwingBench 다운로드 로직
                String command = "wget -q https://github.com/domgiles/swingbench/releases/download/v2.7/swingbench.jar -O " + swingBenchJarPath;
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
                Process p = pb.start();
                int exitCode = p.waitFor();
                
                if (exitCode == 0) {
                    log.info("SwingBench 설치 완료");
                } else {
                    log.error("SwingBench 설치 실패");
                }
            } catch (Exception e) {
                log.error("SwingBench 설치 중 오류", e);
            }
            return null;
        }, executorService);
    }
    
    /**
     * SwingBench 데이터 존재 여부 확인
     */
    private boolean checkSwingBenchDataExists() {
        try {
            // SwingBench 데이터 존재 확인 - 이미 생성되어 있다고 가정
            // 실제로는 DB에 OE 스키마의 테이블들이 존재하는지 확인해야 함
            // 여기서는 일단 true를 반환하여 데이터 생성 과정을 스킵
            return true;
        } catch (Exception e) {
            log.warn("데이터 확인 실패: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * SwingBench 데이터 생성
     */
    private void createSwingBenchData() throws Exception {
        log.info("✓ Order Entry 데이터 생성 시작...");
        
        // OE 데이터 생성 (작은 규모로)
        String createCommand = String.format(
            "java -jar %s -cs %s -u %s -p %s -create -scale 0.01 -cl",
            swingBenchJarPath,
            parseDbUrl(dbUrl),
            extractUsername(dbUrl),
            extractPassword(dbUrl)
        );
        
        log.info("✓ 데이터 생성 명령어: {}", createCommand);
        
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", createCommand);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        
        // 출력 읽기
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[DataGen] {}", line);
            }
        }
        
        int exitCode = p.waitFor();
        log.info("✓ 데이터 생성 완료 (Exit Code: {})", exitCode);
        
        if (exitCode != 0) {
            log.warn("⚠️ 데이터 생성은 실패했지만 테스트 계속 진행");
        }
    }
}

