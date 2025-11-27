/*
******************************************************************
작성자: 최영준
******************************************************************
*/
package com.sys.dbmonitor.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 설정
 * 
 * @Async 어노테이션을 사용하기 위한 설정
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring의 기본 ThreadPoolTaskExecutor를 사용
    // 필요시 커스텀 Executor 설정 가능
}


