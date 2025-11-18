package com.sys.dbmonitor;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("컨텍스트 로딩 테스트는 통합 환경에서 별도로 검증 예정")
@SpringBootTest
@ActiveProfiles("test")  // 테스트 프로파일 사용 (없으면 기본 설정 사용)
class DbMonitorApplicationTests {

    @Test
    void contextLoads() {
        // skipped
    }

}
