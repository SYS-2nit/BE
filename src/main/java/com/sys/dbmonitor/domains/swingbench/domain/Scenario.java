package com.sys.dbmonitor.domains.swingbench.domain;

import lombok.Getter;
import java.util.Arrays;

@Getter
public enum Scenario {
    // 진단 1: Cache Buffer Chain (Latch) Contention
    CBC_LATCH("diagnosis1", "Cache Buffer Chain (Latch) Contention", "cbc_latch", 1),
    
    // 진단 2: Row Lock Wait (TX Enqueue)
    ROW_LOCK_WAIT("diagnosis2", "Row Lock Wait (TX Enqueue)", "row_lock", 2),
    
    // 진단 3: Shared Pool Contention (Library Cache Latch)
    SHARED_POOL("diagnosis3", "Shared Pool Contention (Library Cache Latch)", "shared_pool", 3),
    
    // 진단 4: Direct Path Read/Write Contention
    DIRECT_PATH("diagnosis4", "Direct Path Read/Write Contention", "direct_path", 4),
    
    // 진단 5: Log File Sync Wait
    LOG_FILE_SYNC("diagnosis5", "Log File Sync Wait", "log_sync", 5);
    
    private final String id;
    private final String name;
    private final String code;
    private final int order;
    
    Scenario(String id, String name, String code, int order) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.order = order;
    }
    
    public static Scenario fromId(String id) {
        return Arrays.stream(values())
            .filter(s -> s.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown scenario ID: " + id));
    }
    
    public static Scenario fromCode(String code) {
        return Arrays.stream(values())
            .filter(s -> s.getCode().equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown scenario code: " + code));
    }
}

