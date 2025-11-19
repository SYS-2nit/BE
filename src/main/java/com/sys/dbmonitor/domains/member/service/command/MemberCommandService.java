package com.sys.dbmonitor.domains.member.service.command;

import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.member.repository.MemberRepository;
import com.sys.dbmonitor.domains.member.dto.request.MemberAddressUpdateRequest;
import com.sys.dbmonitor.domains.member.dto.request.MemberCreateRequest;
import com.sys.dbmonitor.domains.member.dto.request.MemberUpdateRequest;
import com.sys.dbmonitor.domains.member.dto.request.NotificationSettingsUpdateRequest;
import com.sys.dbmonitor.domains.member.dto.request.NotificationTestRequest;
import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import com.sys.dbmonitor.domains.notification.domain.AlertPolicy;
import com.sys.dbmonitor.domains.notification.domain.AlertStatus;
import com.sys.dbmonitor.domains.notification.domain.Event;
import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import com.sys.dbmonitor.domains.notification.service.command.EmailAlertService;
import com.sys.dbmonitor.domains.notification.service.command.SlackAlertService;
import com.sys.dbmonitor.global.exception.BadRequestException;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberCommandService {

    private static final Logger log = LoggerFactory.getLogger(MemberCommandService.class);

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailAlertService emailAlertService;
    private final SlackAlertService slackAlertService;

    /**
     * 회원 등록 (이메일, Slack 데이터 제외)
     */
    @Transactional
    public Member createMember(MemberCreateRequest request) {
        // 사용자명 중복 확인
        if (memberRepository.existsByUsername(request.username())) {
            throw new BadRequestException(ExceptionMessage.DUPLICATE_VALUE, "이미 존재하는 사용자명입니다.");
        }

        // 이메일 중복 확인
        if (memberRepository.existsByEmail(request.email())) {
            throw new BadRequestException(ExceptionMessage.DUPLICATE_VALUE, "이미 존재하는 이메일입니다.");
        }

        // 엔티티 생성 (비밀번호는 BCrypt 해시로 저장)
        Member member = Member.builder()
                .username(request.username())
                .email(request.email())
                .company(request.company())
                .build();

        Member saved = memberRepository.save(member);
        log.info("[Member] 회원 등록 완료: id={}, username={}", saved.getId(), saved.getUsername());

        return saved;
    }

    /**
     * 회원 수정 (username, email, company만 수정 가능)
     */
    @Transactional
    public Member updateMember(Long id, MemberUpdateRequest request) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "회원을 찾을 수 없습니다."));

        // 사용자명 중복 확인 (자신 제외)
        if (request.username() != null && !request.username().equals(member.getUsername())) {
            if (memberRepository.existsByUsernameAndIdNot(request.username(), id)) {
                throw new BadRequestException(ExceptionMessage.DUPLICATE_VALUE, "이미 존재하는 사용자명입니다.");
            }
        }

        // 이메일 중복 확인 (자신 제외)
        if (request.email() != null && !request.email().equals(member.getEmail())) {
            if (memberRepository.existsByEmailAndIdNot(request.email(), id)) {
                throw new BadRequestException(ExceptionMessage.DUPLICATE_VALUE, "이미 존재하는 이메일입니다.");
            }
        }

        // 정보 업데이트
        member.update(request.username(), request.email(), request.company());

        Member saved = memberRepository.save(member);
        log.info("[Member] 회원 수정 완료: id={}, username={}", saved.getId(), saved.getUsername());

        return saved;
    }

    /**
     * 회원 삭제 (soft delete)
     */
    @Transactional
    public void deleteMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "회원을 찾을 수 없습니다."));

        member.markAsDeleted();
        memberRepository.save(member);

        log.info("[Member] 회원 삭제 완료: id={}, username={}", member.getId(), member.getUsername());
    }

    /**
     * 주소 정보 업데이트 (email, slack 업데이트)
     * null 값은 기존 값 유지, 새로운 값은 대체
     */
    @Transactional
    public Member updateMemberAddress(MemberAddressUpdateRequest request, Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "회원을 찾을 수 없습니다."));

        // 주소 정보 업데이트 (null 값은 기존 값 유지)
        // 기존 API 호환성을 위해 dangerChannel은 null로 전달
        member.updateAddress(
                request.email(),
                request.slackAddress(),
                request.warningChannel(),
                null, // dangerChannel
                request.criticalChannel()
        );

        Member saved = memberRepository.save(member);
        log.info("[Member] 회원 주소 정보 업데이트 완료: id={}, email={}, slackAddress={}, warningChannel={}, criticalChannel={}",
                saved.getId(), saved.getEmail(), saved.getSlackAddress(), saved.getWarningChannel(), saved.getCriticalChannel());

        return saved;
    }

    /**
     * 알림 설정 업데이트 (email, slackAddress, warningChannel, dangerChannel, criticalChannel)
     * 
     * @param request 알림 설정 업데이트 요청
     * @param userId 회원 ID
     * @return 업데이트된 회원
     */
    @Transactional
    public Member updateNotificationSettings(NotificationSettingsUpdateRequest request, Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "회원을 찾을 수 없습니다."));

        // 빈 문자열을 null로 변환 (프론트에서 "선택하세요" 등으로 전송된 경우)
        String warningChannel = normalizeChannel(request.warningChannel());
        String dangerChannel = normalizeChannel(request.dangerChannel());
        String criticalChannel = normalizeChannel(request.criticalChannel());

        // 알림 설정 업데이트
        member.updateAddress(
                request.email(),
                request.slackAddress(),
                warningChannel,
                dangerChannel,
                criticalChannel
        );

        Member saved = memberRepository.save(member);
        log.info("[Member] 알림 설정 업데이트 완료: id={}, email={}, slackAddress={}, warningChannel={}, dangerChannel={}, criticalChannel={}",
                saved.getId(), saved.getEmail(), saved.getSlackAddress(), 
                saved.getWarningChannel(), saved.getDangerChannel(), saved.getCriticalChannel());

        return saved;
    }

    /**
     * 채널 값을 정규화 (빈 문자열, "선택하세요" 등을 null로 변환)
     * 
     * @param channel 원본 채널 값
     * @return 정규화된 채널 값 (null, "email", "slack", "all" 중 하나)
     */
    private String normalizeChannel(String channel) {
        if (channel == null) {
            return null;
        }
        String trimmed = channel.trim();
        if (trimmed.isEmpty() || "선택하세요".equals(trimmed)) {
            return null;
        }
        return trimmed;
    }

    /**
     * 알림 테스트 전송
     * 
     * @param userId 회원 ID
     * @param request 테스트 요청 (테스트할 채널 목록)
     * @return 테스트 결과 메시지
     */
    @Transactional(readOnly = true)
    public String testNotification(Long userId, NotificationTestRequest request) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "회원을 찾을 수 없습니다."));

        // 테스트용 Event 생성 (간단한 방식으로 직접 필드 설정)
        // 실제 Event는 @ManyToOne 관계가 필요하므로, 테스트용으로는 간단한 객체 생성
        // EmailAlertService와 SlackAlertService는 Event의 일부 필드만 사용하므로
        // 필요한 필드만 있는 간단한 테스트 이벤트 객체 생성
        Event testEvent = createTestEvent();

        StringBuilder result = new StringBuilder();
        java.util.List<String> channels = request.channels() != null ? request.channels() : java.util.Collections.emptyList();

        if (channels.isEmpty()) {
            // 채널이 지정되지 않으면 email과 slack 모두 테스트
            channels = java.util.Arrays.asList("email", "slack");
        }

        for (String channel : channels) {
            try {
                String channelLower = channel != null ? channel.toLowerCase().trim() : "";
                
                if ("all".equals(channelLower) || "both".equals(channelLower)) {
                    // 전체 선택 시: 이메일과 Slack 둘 다 테스트 (각각 주소가 있는 경우에만)
                    boolean emailSent = false;
                    boolean slackSent = false;
                    
                    // 이메일 테스트
                    if (member.getEmail() != null && !member.getEmail().trim().isEmpty()) {
                        try {
                            emailAlertService.sendEmail(member.getEmail(), testEvent);
                            emailSent = true;
                            result.append("이메일 테스트 전송 완료: ").append(member.getEmail()).append(". ");
                        } catch (Exception e) {
                            log.error("[Member] 이메일 테스트 전송 실패: userId={}, error={}", userId, e.getMessage(), e);
                            result.append("이메일 테스트 전송 실패: ").append(e.getMessage()).append(". ");
                        }
                    } else {
                        result.append("이메일 주소가 설정되지 않아 이메일 테스트를 건너뜁니다. ");
                    }
                    
                    // Slack 테스트
                    if (member.getSlackAddress() != null && !member.getSlackAddress().trim().isEmpty()) {
                        try {
                            slackAlertService.sendSlack(member.getSlackAddress(), testEvent);
                            slackSent = true;
                            result.append("Slack 테스트 전송 완료: ").append(member.getSlackAddress()).append(". ");
                        } catch (Exception e) {
                            log.error("[Member] Slack 테스트 전송 실패: userId={}, error={}", userId, e.getMessage(), e);
                            result.append("Slack 테스트 전송 실패: ").append(e.getMessage()).append(". ");
                        }
                    } else {
                        result.append("Slack 웹훅 URL이 설정되지 않아 Slack 테스트를 건너뜁니다. ");
                    }
                    
                    if (!emailSent && !slackSent) {
                        result.append("이메일과 Slack 주소가 모두 설정되지 않아 테스트를 수행할 수 없습니다. ");
                    }
                } else if ("email".equalsIgnoreCase(channelLower)) {
                    if (member.getEmail() == null || member.getEmail().trim().isEmpty()) {
                        result.append("이메일 주소가 설정되지 않았습니다. ");
                        continue;
                    }
                    emailAlertService.sendEmail(member.getEmail(), testEvent);
                    result.append("이메일 테스트 전송 완료: ").append(member.getEmail()).append(". ");
                } else if ("slack".equalsIgnoreCase(channelLower)) {
                    if (member.getSlackAddress() == null || member.getSlackAddress().trim().isEmpty()) {
                        result.append("Slack 웹훅 URL이 설정되지 않았습니다. ");
                        continue;
                    }
                    slackAlertService.sendSlack(member.getSlackAddress(), testEvent);
                    result.append("Slack 테스트 전송 완료: ").append(member.getSlackAddress()).append(". ");
                } else {
                    result.append("알 수 없는 채널: ").append(channel).append(". ");
                }
            } catch (Exception e) {
                log.error("[Member] 알림 테스트 전송 실패: userId={}, channel={}, error={}", userId, channel, e.getMessage(), e);
                result.append(channel).append(" 테스트 전송 실패: ").append(e.getMessage()).append(". ");
            }
        }

        log.info("[Member] 알림 테스트 완료: userId={}, result={}", userId, result.toString());
        return result.toString().trim();
    }

    /**
     * 테스트용 Event 생성
     * 실제 DB 저장 없이 테스트 알림 전송용으로만 사용
     */
    private Event createTestEvent() {
        // 테스트용 더미 AlertPolicy 생성
        AlertPolicy testPolicy = AlertPolicy.builder()
                .name("테스트 정책")
                .member(null) // 테스트용이므로 null 허용
                .instance(null) // 테스트용이므로 null 허용
                .build();

        // 테스트용 AlertEvent 생성
        AlertEvent testAlertEvent = AlertEvent.builder()
                .policy(testPolicy)
                .category(AlertCategory.CPU)
                .name("테스트 알림 규칙")
                .metricName("테스트 메트릭")
                .metricKey("test_metric")
                .thresholdFormat(ThresholdFormat.PERCENT)
                .warning(80.0)
                .danger(90.0)
                .critical(95.0)
                .build();

        // 테스트용 Event 생성
        // instance와 member는 null이어도 EmailAlertService와 SlackAlertService에서 사용하지 않음
        Event testEvent = Event.builder()
                .alertEvent(testAlertEvent)
                .instance(null) // 테스트용이므로 null 허용 (EmailAlertService/SlackAlertService에서 사용 안 함)
                .member(null) // 테스트용이므로 null 허용 (EmailAlertService/SlackAlertService에서 사용 안 함)
                .status(AlertStatus.PENDING)
                .severity(1) // WARNING
                .currentValue(100.0)
                .thresholdValue(80.0)
                .thresholdFormat(ThresholdFormat.PERCENT)
                .message("[테스트] 알림 테스트 메시지입니다.")
                .build();

        return testEvent;
    }
}
