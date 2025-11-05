package com.sys.dbmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EntityScan(basePackages = "com.sys.dbmonitor")
@EnableJpaAuditing  // JPA Auditing 기능 활성화 (@CreatedDate, @LastModifiedDate 등 사용)
public class DbMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbMonitorApplication.class, args);
    }

}
