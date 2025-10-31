package com.sys.dbmonitor.domains.diagnosis.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Function;

@Getter
@RequiredArgsConstructor
public enum ScenarioType {
    CPU_SATURATION(
            1L,
            "CPU 포화(OLTP 과부하)",
            "동시 세션 급증으로 CPU 코어선 근접",
            List.of("CPU", "Session"),
            "SwingBench Order Entry: users=2x코어, thinkTime=0",
            durationSec -> List.of("bash", "swingbench/scenarios/cpu_stress.sh", String.valueOf(durationSec))
    ),
    LIBCACHE_PRESSURE(
            2L,
            "라이브러리 캐시 압박",
            "하드파스 증가, 라이브러리 캐시 reload 상승",
            List.of("Memory", "CPU"),
            "바인드 없이 literal SQL 반복 (cursor_sharing=EXACT)",
            durationSec -> List.of("bash", "swingbench/scenarios/libcache_pressure.sh", String.valueOf(durationSec))
    ),
    BUFFER_CACHE_MISS(
            3L,
            "Buffer Cache 미스율 급등",
            "Physical Reads 증가, 캐시 미스율 상승",
            List.of("Memory", "I/O"),
            "대용량 테이블 Full Scan 반복",
            durationSec -> List.of("bash", "swingbench/scenarios/buffer_cache_miss.sh", String.valueOf(durationSec))
    ),
    REDO_SPIKE(
            4L,
            "Redo 폭증/Archive 압박",
            "대량 DML로 redo 생성 급증",
            List.of("I/O", "Storage"),
            "쓰기 집중 시나리오, 짧은 커밋 간격",
            durationSec -> List.of("bash", "swingbench/scenarios/redo_spike.sh", String.valueOf(durationSec))
    ),
    UNDO_PRESSURE(
            5L,
            "Undo 포화(Long TX)",
            "롱런 트랜잭션으로 UNDO 사용률 급등",
            List.of("Storage", "Session"),
            "대용량 UPDATE/DELETE, 커밋 지연",
            durationSec -> List.of("bash", "swingbench/scenarios/undo_pressure.sh", String.valueOf(durationSec))
    ),
    TEMP_SPILL(
            6L,
            "Temp 스필 급증(PGA 부족)",
            "Sort/Hash 스필 발생",
            List.of("Memory", "I/O"),
            "대형 Sort/Hash Join 반복 + 낮은 PGA",
            durationSec -> List.of("bash", "swingbench/scenarios/temp_spill.sh", String.valueOf(durationSec))
    ),
    CHECKPOINT_DBWR_SPIKE(
            7L,
            "체크포인트/DBWR 스파이크",
            "Dirty Buffer 대량 기록",
            List.of("I/O", "Redo"),
            "단시간 대량 DML 후 유휴, 작은 로그 파일",
            durationSec -> List.of("bash", "swingbench/scenarios/checkpoint_dbwr.sh", String.valueOf(durationSec))
    ),
    LOCK_CONTENTION(
            8L,
            "잠금 경합(TX/TM Lock)",
            "동시 UPDATE/DDL로 대기 증가",
            List.of("Session"),
            "동일 로우 범위 동시 UPDATE 및 동시 DDL",
            durationSec -> List.of("bash", "swingbench/scenarios/lock_contention.sh", String.valueOf(durationSec))
    ),
    LOGON_STORM(
            9L,
            "연결 폭주(Logons/s↑)",
            "로그인 급증으로 세션 생성/종료 반복",
            List.of("Session", "CPU"),
            "클라이언트에서 초당 신규 세션 생성/종료",
            durationSec -> List.of("bash", "swingbench/scenarios/logon_storm.sh", String.valueOf(durationSec))
    ),
    TABLESPACE_THRESHOLD(
            10L,
            "스토리지 용량 임계",
            "테이블스페이스 사용률 상승",
            List.of("Storage"),
            "대량 INSERT로 USER TS 사용률 상승",
            durationSec -> List.of("bash", "swingbench/scenarios/tablespace_threshold.sh", String.valueOf(durationSec))
    );

    private final Long id;
    private final String title;
    private final String summary;
    private final List<String> affectedDashboards;
    private final String reproduction;
    private final Function<Integer, List<String>> commandBuilder;

    public List<String> getCommand(int durationSec) {
        return commandBuilder.apply(durationSec);
    }
}
