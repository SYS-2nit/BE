/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.diagnosis.service;

import com.sys.dbmonitor.domains.diagnosis.domain.ScenarioType;
import com.sys.dbmonitor.domains.diagnosis.dto.request.DiagnosisStartRequest;
import com.sys.dbmonitor.domains.diagnosis.dto.response.DiagnosisStatusDto;
import com.sys.dbmonitor.domains.diagnosis.dto.response.ScenarioDto;
import com.sys.dbmonitor.domains.diagnosis.dto.response.SwingBenchResultDto;
import com.sys.dbmonitor.domains.diagnosis.runners.DiagnosisRunner;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.global.common.util.PasswordEncryptionUtil;
import com.sys.dbmonitor.global.exception.BadRequestException;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisService {

    @Value("${app.encryption.key}")
    private String encryptionKey;

    private final InstanceRepository instanceRepository;
    private final Map<Long, ScenarioType> idToScenario = Arrays.stream(ScenarioType.values())
            .collect(Collectors.toMap(ScenarioType::getId, s -> s));

    @Qualifier("diagnosisExecutor")
    private final ThreadPoolTaskExecutor diagnosisExecutor;

    private final SwingBenchResultParser resultParser;

    private volatile DiagnosisRunner runner;
    private volatile Thread runnerThread;
    private volatile List<Long> selectedScenarioIds = new ArrayList<>();
    private volatile Long currentInstanceId;
    private volatile Integer currentDurationSec;

    // 최근 진단 결과 저장 (메모리 기반, 최대 10개)
    private final List<SwingBenchResultDto> recentResults = new ArrayList<>();
    private static final int MAX_RESULTS = 10;

    @Transactional(readOnly = true)
    public synchronized void startDiagnosis(DiagnosisStartRequest req) {
        if (req == null || req.scenarioIds() == null || req.scenarioIds().isEmpty()) {
            throw new BadRequestException(ExceptionMessage.INVALID_REQUEST);
        }
        if (req.durationSec() <= 0) {
            throw new BadRequestException(ExceptionMessage.INVALID_DURATION);
        }
        if (req.instanceId() == null) {
            throw new BadRequestException(ExceptionMessage.INVALID_REQUEST, "인스턴스 ID는 필수입니다.");
        }
        // 실행 중인 진단이 있는지 확인 (Java 기반 또는 외부 프로세스)
        if ((runnerThread != null && runnerThread.isAlive()) ||
            (runner != null && !runner.isStopped())) {
            throw new BadRequestException(ExceptionMessage.DIAGNOSIS_ALREADY_RUNNING);
        }
        List<ScenarioType> list = req.scenarioIds().stream()
                .map(idToScenario::get)
                .filter(s -> s != null)
                .collect(Collectors.toList());
        if (list.isEmpty()) {
            throw new BadRequestException(ExceptionMessage.INVALID_SCENARIO_IDS);
        }

        // Instance로부터 DB 연결 정보 가져오기 (DBInfo를 함께 로드)
        Instance instance = instanceRepository.findByIdWithDbInfoAndIsDeletedFalse(req.instanceId())
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "인스턴스를 찾을 수 없습니다."));

        if (instance.getDbInfo() == null) {
            throw new NotFoundException(ExceptionMessage.NOT_FOUND, "인스턴스에 연결된 DB 정보를 찾을 수 없습니다.");
        }

        // Instance URL 사용
        String dbUrl = instance.getUrl();
        if (dbUrl == null || dbUrl.isEmpty()) {
            throw new BadRequestException(ExceptionMessage.INVALID_REQUEST, "인스턴스 URL이 설정되지 않았습니다.");
        }

        // DBInfo에서 사용자명과 비밀번호 가져오기 (트랜잭션 내에서 접근)
        String dbUsername = instance.getDbInfo().getUserName();
        String encryptedPassword = instance.getDbInfo().getPassword();

        // 비밀번호 복호화
        String dbPassword;
        try {
            if (PasswordEncryptionUtil.isBcryptHash(encryptedPassword)) {
                throw new BadRequestException(ExceptionMessage.INVALID_REQUEST, 
                        "BCrypt 해시 형식의 비밀번호는 복호화할 수 없습니다. 비밀번호를 재입력하여 AES 암호화로 저장하세요.");
            }
            dbPassword = PasswordEncryptionUtil.decrypt(encryptedPassword, encryptionKey);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ExceptionMessage.INVALID_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("[Diagnosis] 비밀번호 복호화 실패: instanceId={}, error={}", req.instanceId(), e.getMessage());
            throw new BadRequestException(ExceptionMessage.INVALID_REQUEST, "비밀번호 복호화에 실패했습니다.");
        }

        // DB 연결 정보를 환경 변수로 전달
        runner = new DiagnosisRunner(list, req.durationSec(), dbUrl, dbUsername, dbPassword);
        selectedScenarioIds = new ArrayList<>(req.scenarioIds());
        currentInstanceId = req.instanceId();
        currentDurationSec = req.durationSec();

        // 모든 시나리오를 별도 프로세스로 실행 (SwingBench와 Java 기반 모두)
        runnerThread = new Thread(() -> {
            try {
                runner.run();
                // 진단 완료 후 결과 파싱 및 저장
                processDiagnosisResults();
            } catch (Exception e) {
                log.error("[Diagnosis] 진단 실행 중 오류: {}", e.getMessage(), e);
            }
        }, "diagnosis-runner");
        runnerThread.start();
        log.info("[Diagnosis] 진단 시작 (별도 프로세스) - instanceId: {}, scenarios: {}, durationSec: {}",
                req.instanceId(), selectedScenarioIds, req.durationSec());
    }

    public synchronized void stopDiagnosis() {
        if (runner != null) {
            runner.stop();
        }
        if (runnerThread != null) {
            try {
                runnerThread.interrupt();
                runnerThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        runner = null;
        runnerThread = null;
        log.info("[Diagnosis] 정지 완료");
    }

    public DiagnosisStatusDto queryStatus() {
        // Java 기반 진단은 runnerThread가 null일 수 있으므로 runner 존재 여부로 확인
        boolean running = (runnerThread != null && runnerThread.isAlive()) ||
                         (runner != null && !runner.isStopped());
        Long currentId = null;
        int remainSec = 0;
        Integer remainingSec = 0;
        Integer loopCount = null;
        if (running && runner != null) {
            if (runner.getCurrentScenario() != null) {
                currentId = runner.getCurrentScenario().getId();
            }
            if (runner.getRemainSec() != null) {
                remainSec = Math.max(0, runner.getRemainSec().get());
                remainingSec = remainSec;
            }
            if (runner.getLoopCount() != null) {
                loopCount = runner.getLoopCount().get();
            }
        }
        return new DiagnosisStatusDto(
                running,
                currentId,
                selectedScenarioIds != null ? new ArrayList<>(selectedScenarioIds) : List.of(),
                remainSec,
                remainingSec,
                loopCount
        );
    }

    public List<ScenarioDto> getScenarioList() {
        return Arrays.stream(ScenarioType.values())
                .map(ScenarioDto::from)
                .collect(Collectors.toList());
    }

    public ScenarioDto getScenarioDetail(Long id) {
        ScenarioType type = idToScenario.get(id);
        if (type == null) {
            throw new BadRequestException(ExceptionMessage.SCENARIO_NOT_FOUND);
        }
        return ScenarioDto.from(type);
    }

    /**
     * 진단 완료 후 결과 파싱 및 저장
     */
    private synchronized void processDiagnosisResults() {
        if (runner == null || currentInstanceId == null) {
            return;
        }

        String output = runner.getAndClearOutput();
        if (output == null || output.trim().isEmpty()) {
            log.debug("[Diagnosis] 수집된 출력이 없습니다.");
            return;
        }

        // 각 시나리오별로 결과 파싱
        for (Long scenarioId : selectedScenarioIds) {
            ScenarioType scenario = idToScenario.get(scenarioId);
            if (scenario == null) {
                continue;
            }

            // SwingBench 시나리오인 경우에만 파싱
            if (scenario.getCommand(60).get(0).equals("bash") &&
                scenario.getCommand(60).get(1).contains("swingbench")) {

                SwingBenchResultDto result = resultParser.parse(
                        currentInstanceId,
                        scenario.getTitle(),
                        scenarioId,
                        currentDurationSec != null ? currentDurationSec : 60,
                        output
                );

                // 결과 저장
                synchronized (recentResults) {
                    recentResults.add(0, result); // 최신 결과를 맨 앞에 추가
                    if (recentResults.size() > MAX_RESULTS) {
                        recentResults.remove(recentResults.size() - 1);
                    }
                }

                log.info("[Diagnosis] 진단 결과 저장 완료 - scenarioId: {}, TPS: {}, AvgResponse: {}ms",
                        scenarioId, result.transactionsPerSecond(), result.averageResponseTime());
            }
        }
    }

    /**
     * 최근 진단 결과 조회
     */
    public List<SwingBenchResultDto> getRecentResults() {
        synchronized (recentResults) {
            return new ArrayList<>(recentResults);
        }
    }

    /**
     * 특정 시나리오의 최신 결과 조회
     */
    public SwingBenchResultDto getLatestResult(Long scenarioId) {
        synchronized (recentResults) {
            return recentResults.stream()
                    .filter(r -> r.scenarioId().equals(scenarioId))
                    .findFirst()
                    .orElse(null);
        }
    }
}
