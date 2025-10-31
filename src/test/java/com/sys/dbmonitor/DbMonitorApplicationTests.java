package com.sys.dbmonitor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")  // 테스트 프로파일 사용 (없으면 기본 설정 사용)
class DbMonitorApplicationTests {

    @Test
    void contextLoads() {
        // Spring Context가 정상적으로 로드되는지 확인
    }

}
