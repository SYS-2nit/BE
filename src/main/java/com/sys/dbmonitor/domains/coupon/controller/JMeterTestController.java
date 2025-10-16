package com.sys.dbmonitor.domains.coupon.controller;

import com.sys.dbmonitor.global.common.response.ApiResponse;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class JMeterTestController {

    @Value("${jmeter.home:/usr/local/jmeter}")
    private String jmeterHome;

    // 테스트 실행 상태 저장
    private final Map<String, TestStatus> testStatusMap = new ConcurrentHashMap<>();

    /**
     * JMeter 부하 테스트 실행
     */
    @PostMapping("/start-load-test")
    public ApiResponse<Map<String, Object>> startLoadTest(
            @RequestParam(defaultValue = "v1") String apiVersion,
            @RequestParam(defaultValue = "100") int threads,
            @RequestParam(defaultValue = "10") int rampUp,
            @RequestParam(defaultValue = "1") int loops
    ) {
        String testId = UUID.randomUUID().toString().substring(0, 8);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String resultsDir = "test-results/" + timestamp + "_" + testId;

        log.info("╔═══════════════════════════════════════════════════");
        log.info("║ JMeter 부하 테스트 시작");
        log.info("║ Test ID: {}", testId);
        log.info("║ API Version: {}", apiVersion);
        log.info("║ Threads: {}", threads);
        log.info("║ Ramp-up: {}초", rampUp);
        log.info("║ Loops: {}", loops);
        log.info("╚═══════════════════════════════════════════════════");

        // 테스트 상태 저장
        TestStatus status = new TestStatus();
        status.setTestId(testId);
        status.setStatus("RUNNING");
        status.setStartTime(LocalDateTime.now());
        status.setApiVersion(apiVersion);
        status.setThreads(threads);
        testStatusMap.put(testId, status);

        // 비동기로 JMeter 실행
        new Thread(() -> executeJMeter(testId, apiVersion, threads, rampUp, loops, resultsDir)).start();

        Map<String, Object> response = new HashMap<>();
        response.put("testId", testId);
        response.put("status", "RUNNING");
        response.put("message", "부하 테스트가 시작되었습니다.");
        response.put("statusUrl", "/api/test/status/" + testId);

        return ApiResponse.ok(202, response, "테스트가 시작되었습니다.");
    }

    /**
     * 테스트 상태 조회
     */
    @GetMapping("/status/{testId}")
    public ApiResponse<TestStatus> getTestStatus(@PathVariable String testId) {
        TestStatus status = testStatusMap.get(testId);
        if (status == null) {
            throw new NotFoundException(ExceptionMessage.TEST_NOT_FOUND);
        }
        return ApiResponse.ok(200, status, "테스트 상태 조회 완료");
    }

    /**
     * 실행 중인 모든 테스트 조회
     */
    @GetMapping("/status")
    public ApiResponse<List<TestStatus>> getAllTestStatus() {
        return ApiResponse.ok(200, new ArrayList<>(testStatusMap.values()), "전체 테스트 상태 조회");
    }

    /**
     * JMeter 실행 (비동기)
     */
    private void executeJMeter(String testId, String apiVersion, int threads,
                               int rampUp, int loops, String resultsDir) {
        TestStatus status = testStatusMap.get(testId);

        try {
            // 결과 디렉토리 생성
            new File(resultsDir).mkdirs();

            // JMeter 명령어 구성
            List<String> command = new ArrayList<>();
            command.add(jmeterHome + "/bin/jmeter");
            command.add("-n");
            command.add("-t");
            command.add("src/test/jmeter/coupon_load_test_" + apiVersion + ".jmx");
            command.add("-l");
            command.add(resultsDir + "/results.jtl");
            command.add("-e");
            command.add("-o");
            command.add(resultsDir + "/report");
            command.add("-Jthreads=" + threads);
            command.add("-Jrampup=" + rampUp);
            command.add("-Jloops=" + loops);

            log.info("JMeter 명령어: {}", String.join(" ", command));

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // 출력 로그 읽기
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[JMeter] {}", line);
                }
            }

            int exitCode = process.waitFor();

            // 상태 업데이트
            status.setStatus(exitCode == 0 ? "COMPLETED" : "FAILED");
            status.setEndTime(LocalDateTime.now());
            status.setExitCode(exitCode);
            status.setReportPath(resultsDir + "/report/index.html");

            log.info("╔═══════════════════════════════════════════════════");
            log.info("║ JMeter 테스트 완료");
            log.info("║ Test ID: {}", testId);
            log.info("║ Exit Code: {}", exitCode);
            log.info("║ 리포트: {}", status.getReportPath());
            log.info("╚═══════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("JMeter 실행 중 에러 발생 - testId: {}", testId, e);
            status.setStatus("ERROR");
            status.setEndTime(LocalDateTime.now());
            status.setErrorMessage(e.getMessage());
        }
    }

    /**
     * 테스트 상태 DTO
     */
    @lombok.Data
    public static class TestStatus {
        private String testId;
        private String status;  // RUNNING, COMPLETED, FAILED, ERROR
        private String apiVersion;
        private Integer threads;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer exitCode;
        private String reportPath;
        private String errorMessage;
    }
}