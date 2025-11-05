//package com.sys.dbmonitor.domains.diagnosis.service;
//
//import com.sys.dbmonitor.domains.diagnosis.domain.ScenarioType;
//import com.sys.dbmonitor.domains.diagnosis.repository.request.DiagnosisStartRequest;
//import com.sys.dbmonitor.domains.diagnosis.repository.response.DiagnosisStatusDto;
//import com.sys.dbmonitor.domains.diagnosis.repository.response.ScenarioDto;
//import com.sys.dbmonitor.domains.diagnosis.runners.DiagnosisRunner;
//import com.sys.dbmonitor.global.exception.BadRequestException;
//import com.sys.dbmonitor.global.exception.ExceptionMessage;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class DiagnosisService {
//
//    @Value("${spring.datasource.url}")
//    private String dbUrl;
//
//    @Value("${spring.datasource.username}")
//    private String dbUsername;
//
//    @Value("${spring.datasource.password}")
//    private String dbPassword;
//
//    private final Map<Long, ScenarioType> idToScenario = Arrays.stream(ScenarioType.values())
//            .collect(Collectors.toMap(ScenarioType::getId, s -> s));
//
//    private volatile DiagnosisRunner runner;
//    private volatile Thread runnerThread;
//    private volatile List<Long> selectedScenarioIds = new ArrayList<>();
//
//    public synchronized void startDiagnosis(DiagnosisStartRequest req) {
//        if (req == null || req.scenarioIds() == null || req.scenarioIds().isEmpty()) {
//            throw new BadRequestException(ExceptionMessage.INVALID_REQUEST);
//        }
//        if (req.durationSec() <= 0) {
//            throw new BadRequestException(ExceptionMessage.INVALID_DURATION);
//        }
//        if (runnerThread != null && runnerThread.isAlive()) {
//            throw new BadRequestException(ExceptionMessage.DIAGNOSIS_ALREADY_RUNNING);
//        }
//        List<ScenarioType> list = req.scenarioIds().stream()
//                .map(idToScenario::get)
//                .filter(s -> s != null)
//                .collect(Collectors.toList());
//        if (list.isEmpty()) {
//            throw new BadRequestException(ExceptionMessage.INVALID_SCENARIO_IDS);
//        }
//        // DB 연결 정보를 환경 변수로 전달
//        runner = new DiagnosisRunner(list, req.durationSec(), dbUrl, dbUsername, dbPassword);
//        runnerThread = new Thread(runner, "diagnosis-runner");
//        selectedScenarioIds = new ArrayList<>(req.scenarioIds());
//        runnerThread.start();
//        log.info("[Diagnosis] 시작 - scenarios: {}, durationSec: {}", selectedScenarioIds, req.durationSec());
//    }
//
//    public synchronized void stopDiagnosis() {
//        if (runner != null) {
//            runner.stop();
//        }
//        if (runnerThread != null) {
//            try {
//                runnerThread.interrupt();
//                runnerThread.join(2000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//        runner = null;
//        runnerThread = null;
//        log.info("[Diagnosis] 정지 완료");
//    }
//
//    public DiagnosisStatusDto queryStatus() {
//        boolean running = runnerThread != null && runnerThread.isAlive();
//        Long currentId = null;
//        int remain = 0;
//        Integer loop = null;
//        if (running && runner != null && runner.getCurrentScenario() != null) {
//            currentId = runner.getCurrentScenario().getId();
//            remain = Math.max(0, runner.getRemainSec().get());
//            loop = runner.getLoopCount().get();
//        }
//        return new DiagnosisStatusDto(
//                running,
//                currentId,
//                selectedScenarioIds != null ? new ArrayList<>(selectedScenarioIds) : List.of(),
//                remain,
//                remain,
//                loop
//        );
//    }
//
//    public List<ScenarioDto> getScenarioList() {
//        return Arrays.stream(ScenarioType.values())
//                .map(ScenarioDto::from)
//                .collect(Collectors.toList());
//    }
//
//    public ScenarioDto getScenarioDetail(Long id) {
//        ScenarioType type = idToScenario.get(id);
//        if (type == null) {
//            throw new BadRequestException(ExceptionMessage.SCENARIO_NOT_FOUND);
//        }
//        return ScenarioDto.from(type);
//    }
//}
