package com.sys.dbmonitor.domains.diagnosis.service;

import com.sys.dbmonitor.domains.diagnosis.domain.ScenarioType;
import com.sys.dbmonitor.domains.diagnosis.dto.request.DiagnosisStartRequest;
import com.sys.dbmonitor.domains.diagnosis.dto.response.DiagnosisStatusDto;
import com.sys.dbmonitor.domains.diagnosis.dto.response.ScenarioDto;
import com.sys.dbmonitor.domains.diagnosis.runners.DiagnosisRunner;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.global.common.util.PasswordEncryptionUtil;
import com.sys.dbmonitor.global.exception.BadRequestException;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private volatile DiagnosisRunner runner;
    private volatile Thread runnerThread;
    private volatile List<Long> selectedScenarioIds = new ArrayList<>();

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
        if (runnerThread != null && runnerThread.isAlive()) {
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
        runnerThread = new Thread(runner, "diagnosis-runner");
        selectedScenarioIds = new ArrayList<>(req.scenarioIds());
        runnerThread.start();
        log.info("[Diagnosis] 시작 - instanceId: {}, scenarios: {}, durationSec: {}", 
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
        boolean running = runnerThread != null && runnerThread.isAlive();
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
}
