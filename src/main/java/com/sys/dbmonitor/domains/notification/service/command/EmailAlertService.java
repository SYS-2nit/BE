/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.service.command;

import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.notification.domain.Event;
import com.sys.dbmonitor.domains.notification.support.ThresholdFormatUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * 이메일 알림 서비스
 * 
 * WARNING, DANGER 심각도의 알림을 이메일로 전송합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAlertService {

    private final JavaMailSender mailSender;
    
    // MailConfig에서 주입받은 username 사용 (발신자 이메일)
    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:}")
    private String username;

    /**
     * 이메일 알림 전송
     * 
     * @param member 수신자
     * @param event 알림 이벤트
     */
    public void sendEmail(Member member, Event event) {
        if (member.getEmail() == null || member.getEmail().trim().isEmpty()) {
            log.warn("[EmailAlert] 이메일 주소가 없어 전송 불가: memberId={}, eventId={}", member.getId(), event.getId());
            return;
        }
        sendEmail(member.getEmail(), event);
    }

    /**
     * 이메일 알림 전송 (이메일 주소 직접 지정)
     * 테스트용으로 이메일 주소를 직접 받아서 전송
     * 
     * @param email 수신자 이메일 주소
     * @param event 알림 이벤트
     */
    public void sendEmail(String email, Event event) {
        sendEmail(email, event, null);
    }

    /**
     * 이메일 알림 전송 (이메일 주소 및 SMTP 설정 직접 지정)
     * 테스트용으로 SMTP 설정을 직접 받아서 전송
     * 
     * @param email 수신자 이메일 주소
     * @param event 알림 이벤트
     * @param smtpHost SMTP 서버 주소
     * @param smtpPort SMTP 포트
     * @param smtpUsername SMTP 사용자명
     * @param smtpPassword SMTP 비밀번호
     * @param smtpFromEmail 발신자 이메일 (선택적)
     */
    public void sendEmailWithSmtp(String email, Event event, String smtpHost, int smtpPort, 
                                  String smtpUsername, String smtpPassword, String smtpFromEmail) {
        SmtpConfig smtpConfig = new SmtpConfig(smtpHost, smtpPort, smtpUsername, smtpPassword, smtpFromEmail);
        sendEmail(email, event, smtpConfig);
    }

    /**
     * 이메일 알림 전송 (이메일 주소 및 SMTP 설정 직접 지정)
     * 테스트용으로 SMTP 설정을 직접 받아서 전송
     * 
     * @param email 수신자 이메일 주소
     * @param event 알림 이벤트
     * @param smtpConfig SMTP 설정 (null이면 기본 JavaMailSender 사용)
     */
    private void sendEmail(String email, Event event, SmtpConfig smtpConfig) {
        if (email == null || email.trim().isEmpty()) {
            log.warn("[EmailAlert] 이메일 주소가 없어 전송 불가: email={}, eventId={}", email, event.getId());
            return;
        }

        try {
            // SMTP 설정이 있으면 동적으로 JavaMailSender 생성, 없으면 기본 사용
            JavaMailSender sender = (smtpConfig != null) ? createMailSender(smtpConfig) : mailSender;
            
            // 디버깅: 사용되는 SMTP 설정 로그
            if (sender instanceof org.springframework.mail.javamail.JavaMailSenderImpl) {
                org.springframework.mail.javamail.JavaMailSenderImpl impl = 
                    (org.springframework.mail.javamail.JavaMailSenderImpl) sender;
                log.info("[EmailAlert] SMTP 설정 확인 - host={}, port={}, username={}, passwordLength={}", 
                    impl.getHost(), impl.getPort(), impl.getUsername(), 
                    impl.getPassword() != null ? impl.getPassword().length() : 0);
            }
            
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 발신자 설정
            String fromEmail = (smtpConfig != null && smtpConfig.fromEmail != null) 
                ? smtpConfig.fromEmail 
                : (username != null && !username.isEmpty() ? username : "noreply@dbmonitor.com");
            helper.setFrom(fromEmail, "DB Monitor");
            log.debug("[EmailAlert] 발신자 설정: {}", fromEmail);
            
            // 수신자 설정
            helper.setTo(email.trim());
            log.debug("[EmailAlert] 수신자 설정: {}", email);
            
            // 제목 설정
            String severityText = getSeverityText(event.getSeverity());
            helper.setSubject(String.format("[%s] DB 모니터링 알림: %s", severityText, event.getAlertEvent().getMetricName()));

            // 본문 설정 (HTML 형식)
            String htmlContent = buildEmailContent(event);
            helper.setText(htmlContent, true);

            log.info("[EmailAlert] 이메일 전송 시도: from={}, to={}, subject={}", 
                fromEmail, email, helper.getMimeMessage().getSubject());
            
            // 이메일 전송
            sender.send(message);
            
            log.info("[EmailAlert] 이메일 전송 완료: email={}, eventId={}, severity={}, smtpHost={}", 
                email, event.getId(), event.getSeverity(), 
                (smtpConfig != null ? smtpConfig.host : "default"));
        } catch (MailException e) {
            log.error("[EmailAlert] 이메일 전송 실패: email={}, eventId={}, error={}, cause={}", 
                email, event.getId(), e.getMessage(), 
                e.getCause() != null ? e.getCause().getMessage() : "none", e);
            throw e; // 테스트에서 오류 확인을 위해 예외 재throw
        } catch (Exception e) {
            log.error("[EmailAlert] 이메일 전송 중 예외 발생: email={}, eventId={}, error={}", 
                email, event.getId(), e.getMessage(), e);
            throw new RuntimeException("이메일 전송 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * SMTP 설정으로 JavaMailSender 생성
     */
    private JavaMailSender createMailSender(SmtpConfig config) {
        org.springframework.mail.javamail.JavaMailSenderImpl sender = new org.springframework.mail.javamail.JavaMailSenderImpl();
        
        sender.setHost(config.host);
        sender.setPort(config.port);
        sender.setUsername(config.username);
        sender.setPassword(config.password);
        
        java.util.Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        
        // 포트에 따라 SSL/TLS 설정 분기
        if (config.port == 465) {
            // SSL 사용 (네이버 권장)
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.required", "true");
            props.put("mail.smtp.ssl.trust", config.host);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.fallback", "false");
            log.info("[EmailAlert] 사용자 지정 SMTP - SSL 모드 사용 (포트 465)");
        } else {
            // STARTTLS 사용 (포트 587)
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            log.info("[EmailAlert] 사용자 지정 SMTP - STARTTLS 모드 사용 (포트 {})", config.port);
        }
        
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.debug", "true"); // 디버깅을 위해 활성화
        
        log.info("[EmailAlert] 사용자 지정 SMTP 설정: host={}, port={}, username={}", 
            config.host, config.port, config.username);
        
        return sender;
    }

    /**
     * SMTP 설정 DTO
     */
    public static class SmtpConfig {
        public String host;
        public int port;
        public String username;
        public String password;
        public String fromEmail; // 선택적

        public SmtpConfig(String host, int port, String username, String password) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.fromEmail = username; // 기본값은 username과 동일
        }

        public SmtpConfig(String host, int port, String username, String password, String fromEmail) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.fromEmail = fromEmail;
        }
    }

    /**
     * 이메일 본문 생성 (HTML)
     */
    private String buildEmailContent(Event event) {
        String severityText = getSeverityText(event.getSeverity());
        String severityColor = getSeverityColor(event.getSeverity());
        
        String formattedCurrent = ThresholdFormatUtils.formatValue(event.getCurrentValue(), event.getThresholdFormat());
        String formattedThreshold = ThresholdFormatUtils.formatValue(event.getThresholdValue(), event.getThresholdFormat());

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: %s; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; border-top: none; }
                    .alert-info { background-color: white; padding: 15px; margin: 10px 0; border-left: 4px solid %s; }
                    .metric-value { font-size: 24px; font-weight: bold; color: %s; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    table { width: 100%%; border-collapse: collapse; margin: 15px 0; }
                    th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }
                    th { background-color: #f2f2f2; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>DB 모니터링 알림</h2>
                        <p>심각도: %s</p>
                    </div>
                    <div class="content">
                        <div class="alert-info">
                            <h3>%s</h3>
                            <p class="metric-value">현재 값: %s</p>
                            <p>임계값: %s</p>
                        </div>
                        <table>
                            <tr>
                                <th>항목</th>
                                <th>내용</th>
                            </tr>
                            <tr>
                                <td>인스턴스 ID</td>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <td>메트릭</td>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <td>알림 규칙</td>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <td>발생 시간</td>
                                <td>%s</td>
                            </tr>
                        </table>
                        <p style="margin-top: 20px;">
                            <strong>알림 메시지:</strong><br>
                            %s
                        </p>
                    </div>
                    <div class="footer">
                        <p>이 이메일은 자동으로 생성되었습니다. DB Monitor 시스템에서 발송되었습니다.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            severityColor,
            severityColor,
            severityColor,
            severityText,
            event.getAlertEvent().getMetricName(),
            formattedCurrent,
            formattedThreshold,
            event.getInstance() != null ? String.valueOf(event.getInstance().getId()) : "테스트",
            event.getAlertEvent().getMetricName(),
            event.getAlertEvent().getName(),
            event.getCreatedAt(),
            event.getMessage()
        );
    }

    /**
     * 심각도 텍스트 반환
     */
    private String getSeverityText(Integer severity) {
        if (severity == null) {
            return "알 수 없음";
        }
        return switch (severity) {
            case 0 -> "복구 (RECOVERY)";
            case 1 -> "경고 (WARNING)";
            case 2 -> "위험 (DANGER)";
            case 3 -> "치명 (CRITICAL)";
            default -> "알 수 없음";
        };
    }

    /**
     * 심각도 색상 반환
     */
    private String getSeverityColor(Integer severity) {
        if (severity == null) {
            return "#666666";
        }
        return switch (severity) {
            case 0 -> "#28a745"; // 초록색 (RECOVERY)
            case 1 -> "#FFA500"; // 주황색 (WARNING)
            case 2 -> "#FF4500"; // 주황빨강 (DANGER)
            case 3 -> "#DC143C"; // 빨강 (CRITICAL)
            default -> "#666666"; // 회색
        };
    }
}

