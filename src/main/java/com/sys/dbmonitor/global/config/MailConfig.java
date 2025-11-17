package com.sys.dbmonitor.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 이메일 설정
 * 
 * JavaMailSender 빈을 수동으로 생성합니다.
 * application.yml의 spring.mail 설정을 사용합니다.
 */
@Slf4j
@Configuration
public class MailConfig {

    @Value("${spring.mail.host:smtp.naver.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        
        // 이메일 설정이 비어있으면 경고 로그 출력
        if (username == null || username.trim().isEmpty()) {
            log.warn("[MailConfig] 이메일 username이 설정되지 않았습니다. 이메일 전송이 실패할 수 있습니다.");
        } else {
            log.info("[MailConfig] 이메일 username 설정됨: {}", username);
        }
        
        if (password == null || password.trim().isEmpty()) {
            log.warn("[MailConfig] 이메일 password가 설정되지 않았습니다. 이메일 전송이 실패할 수 있습니다.");
        } else {
            log.info("[MailConfig] 이메일 password 설정됨: {} (길이: {})", 
                password.length() > 0 ? "***" : "비어있음", password.length());
        }
        
        // SMTP 속성 설정
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        
        // 포트에 따라 SSL/TLS 설정 분기
        if (port == 465) {
            // SSL 사용 (네이버 권장)
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.required", "true");
            props.put("mail.smtp.ssl.trust", host);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.fallback", "false");
            log.info("[MailConfig] SSL 모드 사용 (포트 465)");
        } else {
            // STARTTLS 사용 (포트 587)
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            log.info("[MailConfig] STARTTLS 모드 사용 (포트 {})", port);
        }
        
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.debug", "true"); // 디버깅을 위해 활성화
        
        log.info("[MailConfig] JavaMailSender 빈 생성 완료: host={}, port={}, username={}, passwordLength={}", 
            host, port, username, password != null ? password.length() : 0);
        log.info("[MailConfig] SMTP 속성: {}", props);
        
        return mailSender;
    }
}

