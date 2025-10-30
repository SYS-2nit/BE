package com.sys.dbmonitor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.sys.dbmonitor")
@MapperScan(basePackages = "com.sys.dbmonitor", annotationClass = org.apache.ibatis.annotations.Mapper.class)
@EntityScan(basePackages = "com.sys.dbmonitor")
@EnableJpaAuditing
public class DbMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbMonitorApplication.class, args);
    }

}
