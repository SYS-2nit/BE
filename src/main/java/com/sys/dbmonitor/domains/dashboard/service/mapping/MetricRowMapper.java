// src/main/java/com/sys/dbmonitor/domains/dashboard/service/mapping/MetricRowMapper.java
package com.sys.dbmonitor.domains.dashboard.service.mapping;

import com.sys.dbmonitor.domains.dashboard.domain.MetricData;
import com.sys.dbmonitor.domains.dashboard.util.MetricGet;
import com.sys.dbmonitor.domains.dashboard.util.NameConv;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

public final class MetricRowMapper {
    private static final Set<String> STRING_COLS = Set.of(
        // SQL_ID, NAME류, *_tablespace_name*, *_seg*, *_compression*, max_ts_name 등
        "TOP_SQL_BY_CPU_SQL_ID_01","TOP_SQL_BY_CPU_SQL_ID_02","TOP_SQL_BY_CPU_SQL_ID_03","TOP_SQL_BY_CPU_SQL_ID_04","TOP_SQL_BY_CPU_SQL_ID_05",
        "TOP_SQL_BY_SHARED_POOL_SQL_ID_01","TOP_SQL_BY_SHARED_POOL_SQL_ID_02","TOP_SQL_BY_SHARED_POOL_SQL_ID_03","TOP_SQL_BY_SHARED_POOL_SQL_ID_04","TOP_SQL_BY_SHARED_POOL_SQL_ID_05",
        "1_DATA_FILE_NAME","2_DATA_FILE_NAME","3_DATA_FILE_NAME","4_DATA_FILE_NAME","5_DATA_FILE_NAME",
        "1_DATA_TABLESPACE_NAME","2_DATA_TABLESPACE_NAME","3_DATA_TABLESPACE_NAME","4_DATA_TABLESPACE_NAME","5_DATA_TABLESPACE_NAME",
        "SYSTEM_TABLESPACE_NAME","SYSAUX_TABLESPACE_NAME","UNDOTBS1_TABLESPACE_NAME","USERS_TABLESPACE_NAME",
        "SYSTEM_TABLESPACE_NAME_INC","SYSAUX_TABLESPACE_NAME_INC","UNDOTBS1_TABLESPACE_NAME_INC","USERS_TABLESPACE_NAME_INC",
        "1_OWNER_SEG","2_OWNER_SEG","3_OWNER_SEG","4_OWNER_SEG","5_OWNER_SEG",
        "1_TABLESPACE_NAME_SEG","2_TABLESPACE_NAME_SEG","3_TABLESPACE_NAME_SEG","4_TABLESPACE_NAME_SEG","5_TABLESPACE_NAME_SEG",
        "1_COMPRESSION_SEG","2_COMPRESSION_SEG","3_COMPRESSION_SEG","4_COMPRESSION_SEG","5_COMPRESSION_SEG",
        "MAX_TS_NAME"
    );

    private static final Map<String,String> ALIAS = new HashMap<>();
    static {
        // 숫자로 시작하는 키 → 자바 필드명 별칭
        ALIAS.put("1_DATA_FILE_NAME","dataFileName01");
        ALIAS.put("2_DATA_FILE_NAME","dataFileName02");
        ALIAS.put("3_DATA_FILE_NAME","dataFileName03");
        ALIAS.put("4_DATA_FILE_NAME","dataFileName04");
        ALIAS.put("5_DATA_FILE_NAME","dataFileName05");

        ALIAS.put("1_DATA_TABLESPACE_NAME","dataTablespaceName01");
        ALIAS.put("2_DATA_TABLESPACE_NAME","dataTablespaceName02");
        ALIAS.put("3_DATA_TABLESPACE_NAME","dataTablespaceName03");
        ALIAS.put("4_DATA_TABLESPACE_NAME","dataTablespaceName04");
        ALIAS.put("5_DATA_TABLESPACE_NAME","dataTablespaceName05");

        ALIAS.put("1_DATA_IO_SHARE_PCT","dataIoSharePct01");
        ALIAS.put("2_DATA_IO_SHARE_PCT","dataIoSharePct02");
        ALIAS.put("3_DATA_IO_SHARE_PCT","dataIoSharePct03");
        ALIAS.put("4_DATA_IO_SHARE_PCT","dataIoSharePct04");
        ALIAS.put("5_DATA_IO_SHARE_PCT","dataIoSharePct05");

        ALIAS.put("1_OWNER_SEG","ownerSeg01");
        ALIAS.put("2_OWNER_SEG","ownerSeg02");
        ALIAS.put("3_OWNER_SEG","ownerSeg03");
        ALIAS.put("4_OWNER_SEG","ownerSeg04");
        ALIAS.put("5_OWNER_SEG","ownerSeg05");

        ALIAS.put("1_TABLESPACE_NAME_SEG","tablespaceNameSeg01");
        ALIAS.put("2_TABLESPACE_NAME_SEG","tablespaceNameSeg02");
        ALIAS.put("3_TABLESPACE_NAME_SEG","tablespaceNameSeg03");
        ALIAS.put("4_TABLESPACE_NAME_SEG","tablespaceNameSeg04");
        ALIAS.put("5_TABLESPACE_NAME_SEG","tablespaceNameSeg05");

        ALIAS.put("1_SIZE_GB_SEG","sizeGbSeg01");
        ALIAS.put("2_SIZE_GB_SEG","sizeGbSeg02");
        ALIAS.put("3_SIZE_GB_SEG","sizeGbSeg03");
        ALIAS.put("4_SIZE_GB_SEG","sizeGbSeg04");
        ALIAS.put("5_SIZE_GB_SEG","sizeGbSeg05");

        ALIAS.put("1_COMPRESSION_SEG","compressionSeg01");
        ALIAS.put("2_COMPRESSION_SEG","compressionSeg02");
        ALIAS.put("3_COMPRESSION_SEG","compressionSeg03");
        ALIAS.put("4_COMPRESSION_SEG","compressionSeg04");
        ALIAS.put("5_COMPRESSION_SEG","compressionSeg05");
        
        // Library Cache Reloads
        ALIAS.put("LIBRARY_CACHE_RELOADS_PER_SEC", "libcacheReloadPerSec");
        ALIAS.put("LIBCACHE_RELOAD_PER_S", "libcacheReloadPerSec");
        ALIAS.put("libcache_reload_per_s", "libcacheReloadPerSec");
        
        // FRA Usage
        ALIAS.put("FRA_USAGE_PCT", "fraUsagePct");
        ALIAS.put("fra_usage_pct", "fraUsagePct");
        
        // Session 한도/급증
        ALIAS.put("SESSION_USAGE_PCT", "sessionUsagePct");
        ALIAS.put("session_usage_pct", "sessionUsagePct");
        ALIAS.put("SESSION_HEADROOM", "sessionHeadroom");
        ALIAS.put("session_headroom", "sessionHeadroom");
        ALIAS.put("SESSION_GROWTH_RATE_PER_MIN", "sessionGrowthRatePerMin");
        ALIAS.put("session_growth_rate_per_min", "sessionGrowthRatePerMin");
        ALIAS.put("SESSION_BREACH_ETA_MIN", "sessionBreachEtaMin");
        ALIAS.put("session_breach_eta_min", "sessionBreachEtaMin");
        
        // 테이블스페이스 사용률
        ALIAS.put("SYSTEM_TS_USAGE_PCT", "systemTsUsagePct");
        ALIAS.put("system_ts_usage_pct", "systemTsUsagePct");
        ALIAS.put("SYSAUX_TS_USAGE_PCT", "sysauxTsUsagePct");
        ALIAS.put("sysaux_ts_usage_pct", "sysauxTsUsagePct");
        ALIAS.put("USERS_TS_USAGE_PCT", "usersTsUsagePct");
        ALIAS.put("users_ts_usage_pct", "usersTsUsagePct");
        ALIAS.put("UNDO_TS_USAGE_PCT", "undoTsUsagePct");
        ALIAS.put("undo_ts_usage_pct", "undoTsUsagePct");
        ALIAS.put("TEMP_TS_USAGE_PCT", "tempTsUsagePct");
        ALIAS.put("temp_ts_usage_pct", "tempTsUsagePct");
        
        // 테이블스페이스 사용량 (MB)
        ALIAS.put("SYSTEM_TS_USED_MB", "systemTsUsedMb");
        ALIAS.put("system_ts_used_mb", "systemTsUsedMb");
        ALIAS.put("SYSAUX_TS_USED_MB", "sysauxTsUsedMb");
        ALIAS.put("sysaux_ts_used_mb", "sysauxTsUsedMb");
        ALIAS.put("USERS_TS_USED_MB", "usersTsUsedMb");
        ALIAS.put("users_ts_used_mb", "usersTsUsedMb");
        ALIAS.put("UNDO_TS_USED_MB", "undoTsUsedMb");
        ALIAS.put("undo_ts_used_mb", "undoTsUsedMb");
        ALIAS.put("TEMP_TS_USED_MB", "tempTsUsedMb");
        ALIAS.put("temp_ts_used_mb", "tempTsUsedMb");
        
        // 테이블스페이스 여유량 (MB)
        ALIAS.put("SYSTEM_TS_FREE_MB", "systemTsFreeMb");
        ALIAS.put("system_ts_free_mb", "systemTsFreeMb");
        ALIAS.put("SYSAUX_TS_FREE_MB", "sysauxTsFreeMb");
        ALIAS.put("sysaux_ts_free_mb", "sysauxTsFreeMb");
        ALIAS.put("USERS_TS_FREE_MB", "usersTsFreeMb");
        ALIAS.put("users_ts_free_mb", "usersTsFreeMb");
        ALIAS.put("UNDO_TS_FREE_MB", "undoTsFreeMb");
        ALIAS.put("undo_ts_free_mb", "undoTsFreeMb");
        ALIAS.put("TEMP_TS_FREE_MB", "tempTsFreeMb");
        ALIAS.put("temp_ts_free_mb", "tempTsFreeMb");
        
        // 백그라운드 프로세스 상태
        ALIAS.put("LGWR_PID", "lgwrPid");
        ALIAS.put("lgwr_pid", "lgwrPid");
        ALIAS.put("LGWR_ACTIVE", "lgwrActive");
        ALIAS.put("lgwr_active", "lgwrActive");
        ALIAS.put("DBWR_PID", "dbwrPid");
        ALIAS.put("dbwr_pid", "dbwrPid");
        ALIAS.put("DBWR_ACTIVE", "dbwrActive");
        ALIAS.put("dbwr_active", "dbwrActive");
        ALIAS.put("PMON_PID", "pmonPid");
        ALIAS.put("pmon_pid", "pmonPid");
        ALIAS.put("PMON_ACTIVE", "pmonActive");
        ALIAS.put("pmon_active", "pmonActive");
        ALIAS.put("SMON_PID", "smonPid");
        ALIAS.put("smon_pid", "smonPid");
        ALIAS.put("SMON_ACTIVE", "smonActive");
        ALIAS.put("smon_active", "smonActive");
        ALIAS.put("CKPT_PID", "ckptPid");
        ALIAS.put("ckpt_pid", "ckptPid");
        ALIAS.put("CKPT_ACTIVE", "ckptActive");
        ALIAS.put("ckpt_active", "ckptActive");
        ALIAS.put("ARCN_PID", "arcnPid");
        ALIAS.put("arcn_pid", "arcnPid");
        ALIAS.put("ARCN_ACTIVE", "arcnActive");
        ALIAS.put("arcn_active", "arcnActive");
    }

    /**
     * finals 맵에서 주어진 컬럼들만 뽑아 MetricData 객체에 채워 넣는다.
     * - 컬럼명(문서의 지표 키) → 엔티티 필드명으로 변환(resolveFieldName)
     * - 필드 타입은 간단 규칙으로 결정:
     *     · STRING_COLS 에 포함된 컬럼은 String
     *     · 그 외는 Double (숫자 지표)
     * - 리플렉션으로 세터(setXxx)를 찾아 호출
     * - finals 에 값이 없거나 세터가 없으면 조용히 건너뜀(필요 시 로그 추가)
     */
    public static void fillColumns(Map<String,Object> finals, MetricData row, List<String> columns) {
        Class<?> cls = MetricData.class;

        for (String col : columns) {
            // 1) "AAS_ONCPU_SESSIONS" 같은 지표 키 → 엔티티 필드명으로 매핑
            //    예) AAS_ONCPU_SESSIONS → aasOncpuSessions
            String field  = resolveFieldName(col);

            // 2) 세터 메서드 이름 생성: "set" + 대문자화 첫글자 + 나머지
            //    예) aasOncpuSessions → setAasOncpuSessions
            String setter = "set" + Character.toUpperCase(field.charAt(0)) + field.substring(1);

            try {
                // 3) 타입 분기: 문자열 지표인지 숫자(Double) 지표인지
                if (STRING_COLS.contains(col.toUpperCase())) {
                    // 문자열 지표: setXxx(String) 호출
                    Method m = cls.getMethod(setter, String.class);
                    m.invoke(row, MetricGet.s(finals, col)); // finals→String 안전 변환
                } else {
                    // 숫자 지표: setXxx(Double) 호출
                    Method m = cls.getMethod(setter, Double.class);
                    m.invoke(row, MetricGet.d(finals, col)); // finals→Double 안전 변환
                }

            } catch (NoSuchMethodException nsme) {
                // 4) 엔티티에 해당 필드/세터가 아직 없거나 컬럼 오타인 경우
                //    현재는 무시. 필요하면 여기서 warn 로그를 남겨 추적 가능.
            } catch (Exception ignore) {
                // 5) 변환/호출 과정의 예외(예: 타입 캐스팅 문제)는 일단 무시.
                //    운영 단계에서는 최소 warn 로그 권장.
            }
        }
    }


    private static String resolveFieldName(String col) {
        String u = col.toUpperCase();
        if (ALIAS.containsKey(u)) return ALIAS.get(u);
        return NameConv.snakeToCamel(col);
    }

    /** 공통 collectedAt 추출 */
    public static LocalDateTime resolveCollectedAt(Map<String,Object> finals) {
        return MetricGet.ts(finals, "COLLECT_EPOCH_MS", "COLLECTED_AT");
    }
}
