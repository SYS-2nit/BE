package com.sys.dbmonitor.domains.diagnosis.runners;

import com.sys.dbmonitor.domains.diagnosis.domain.ScenarioType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DiagnosisRunner implements Runnable {
    private final List<ScenarioType> scenarios;
    private final int durationSec;
    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Getter
    private volatile ScenarioType currentScenario;
    @Getter
    private final AtomicInteger remainSec = new AtomicInteger(0);
    @Getter
    private final AtomicInteger loopCount = new AtomicInteger(0);

    // 현재 실행 프로세스 전역 참조
    private volatile Process currentProcess;

    // SwingBench 출력 수집용
    @Getter
    private final StringBuilder outputCollector = new StringBuilder();

    public DiagnosisRunner(List<ScenarioType> scenarios, int durationSec, String dbUrl, String dbUsername, String dbPassword) {
        this.scenarios = scenarios;
        this.durationSec = durationSec;
        this.dbUrl = dbUrl;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    @Override
    public void run() {
        if (scenarios == null || scenarios.isEmpty()) {
            log.warn("진단 실행 시나리오가 비어 있습니다.");
            return;
        }
        int idx = 0;
            currentScenario = scenarios.get(idx);
            log.info("[Diagnosis] 시나리오 시작: {} ({}초)", currentScenario.getTitle(), durationSec);
            remainSec.set(durationSec);

            List<String> cmd = currentScenario.getCommand(durationSec);
            try {
                // 모든 시나리오를 별도 프로세스로 실행 (SwingBench와 Java 기반 모두)
                runScenarioCmd(cmd, durationSec);
            } catch (Exception ex) {
                log.error("[Diagnosis] 시나리오 {} 실행 중 오류: {}", currentScenario.getTitle(), ex.getMessage(), ex);
            }
            log.info("[Diagnosis] 시나리오 종료: {}", currentScenario.getTitle());

            // 라운드 로빈
            idx = (idx + 1) % scenarios.size();
            if (idx == 0) loopCount.incrementAndGet();
        log.info("[Diagnosis] 실행 스레드 종료");
    }

    private void runScenarioCmd(List<String> cmd, int durationSec) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        // 작업 디렉토리를 프로젝트 루트로 설정
        String projectRoot = System.getProperty("user.dir");
        pb.directory(new File(projectRoot));
        // DB 연결 정보를 환경 변수로 전달
        Map<String, String> env = pb.environment();
        env.put("WHA_DB_URL", dbUrl);
        env.put("WHA_DB_USERNAME", dbUsername);
        env.put("WHA_DB_PASSWORD", dbPassword);
        currentProcess = pb.start();

        // 로그 리더(비동기) - 출력 수집 포함
        Thread logT = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[진단 로그] {}", line);
                    // SwingBench 출력 수집 (최대 50KB)
                    synchronized (outputCollector) {
                        if (outputCollector.length() < 50000) {
                            outputCollector.append(line).append("\n");
                        }
                    }
                }
            } catch (Exception ignore) {}
        }, "scenario-log-reader");
        logT.setDaemon(true);
        logT.start();

        // 남은 시간을 실시간으로 업데이트하는 스레드
        Thread countdownThread = new Thread(() -> {
            for (int i = durationSec; i > 0 && running.get(); i--) {
                remainSec.set(i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "countdown-thread");
        countdownThread.setDaemon(true);
        countdownThread.start();

        try {
            // 프로세스 종료 여부와 관계없이 설정된 시간만큼 대기
            // 프로세스가 즉시 종료되어도 대시보드 관찰을 위해 설정 시간만큼 유지
            boolean finished = currentProcess.waitFor(durationSec, TimeUnit.SECONDS);
            
            if (finished) {
                // 프로세스가 설정 시간 전에 종료된 경우
                int exitCode = currentProcess.exitValue();
                if (exitCode == 0) {
                    // 정상 종료인 경우 INFO 레벨로 로그
                    log.info("[Diagnosis] 진단 프로세스가 정상 종료됨 (exit code: 0). 설정된 시간({}초)까지 대기합니다.", durationSec);
                } else {
                    // 비정상 종료인 경우 WARN 레벨로 로그
                    log.warn("[Diagnosis] 진단 프로세스가 비정상 종료됨 (exit code: {}). 설정된 시간({}초)까지 대기합니다.", exitCode, durationSec);
                }
                // countdownThread가 남은 시간을 처리하므로 추가 대기 불필요
            } else {
                // 설정 시간 동안 실행 중이면 강제 종료
                currentProcess.destroy();
                remainSec.set(0);
                log.info("[Diagnosis] 진단 프로세스가 설정 시간({}초) 동안 실행되어 강제 종료합니다.", durationSec);
            }
            
            // countdownThread가 남은 시간을 카운트다운하도록 대기
            // 프로세스가 조기 종료되어도 카운트다운은 계속됨
            countdownThread.join(durationSec * 1000L);
            
        } catch (InterruptedException e) {
            currentProcess.destroyForcibly();
            countdownThread.interrupt();
            Thread.currentThread().interrupt();
        } finally {
            currentProcess = null;
        }
    }


    public void stop() {
        running.set(false);
        if (currentProcess != null) {
            currentProcess.destroyForcibly();
        }
    }

    /**
     * 실행 중인지 확인
     */
    public boolean isStopped() {
        return !running.get();
    }

    /**
     * 수집된 출력 반환 및 초기화
     */
    public String getAndClearOutput() {
        synchronized (outputCollector) {
            String output = outputCollector.toString();
            outputCollector.setLength(0);
            return output;
        }
    }
}
