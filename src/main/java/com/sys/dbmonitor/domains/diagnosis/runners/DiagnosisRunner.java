package com.sys.dbmonitor.domains.diagnosis.runners;

import com.sys.dbmonitor.domains.diagnosis.domain.ScenarioType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DiagnosisRunner implements Runnable {
    private final List<ScenarioType> scenarios;
    private final int durationSec;
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Getter
    private volatile ScenarioType currentScenario;
    @Getter
    private final AtomicInteger remainSec = new AtomicInteger(0);
    @Getter
    private final AtomicInteger loopCount = new AtomicInteger(0);

    // 현재 실행 프로세스 전역 참조
    private volatile Process currentProcess;

    public DiagnosisRunner(List<ScenarioType> scenarios, int durationSec) {
        this.scenarios = scenarios;
        this.durationSec = durationSec;
    }

    @Override
    public void run() {
        if (scenarios == null || scenarios.isEmpty()) {
            log.warn("진단 실행 시나리오가 비어 있습니다.");
            return;
        }
        int idx = 0;
        while (running.get()) {
            currentScenario = scenarios.get(idx);
            log.info("[Diagnosis] 시나리오 시작: {} ({}초)", currentScenario.getTitle(), durationSec);
            remainSec.set(durationSec);

            List<String> cmd = currentScenario.getCommand(durationSec);
            try {
                runScenarioCmd(cmd, durationSec);
            } catch (Exception ex) {
                log.error("[Diagnosis] 시나리오 {} 실행 중 오류: {}", currentScenario.getTitle(), ex.getMessage(), ex);
            }
            log.info("[Diagnosis] 시나리오 종료: {}", currentScenario.getTitle());

            // 라운드 로빈
            idx = (idx + 1) % scenarios.size();
            if (idx == 0) loopCount.incrementAndGet();
        }
        log.info("[Diagnosis] 실행 스레드 종료");
    }

    private void runScenarioCmd(List<String> cmd, int durationSec) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        currentProcess = pb.start();

        // 로그 리더(비동기)
        Thread logT = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[진단 로그] {}", line);
                }
            } catch (Exception ignore) {}
        }, "scenario-log-reader");
        logT.setDaemon(true);
        logT.start();

        try {
            boolean finished = currentProcess.waitFor(durationSec, TimeUnit.SECONDS);
            if (!finished) currentProcess.destroy();
        } catch (InterruptedException e) {
            currentProcess.destroyForcibly();
            Thread.currentThread().interrupt();
        }
        currentProcess = null;
    }

    public void stop() {
        running.set(false);
        if (currentProcess != null) currentProcess.destroyForcibly();
    }
}
