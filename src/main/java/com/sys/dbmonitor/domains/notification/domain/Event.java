package com.sys.dbmonitor.domains.notification.domain;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 알림 발생 이벤트 엔티티
 * 실제로 발생한 알림 이벤트를 저장하는 테이블.
 * 메트릭이 임계값을 초과했을 때 생성되며, 심각도, 현재 값, 임계값 등을 기록합니다.
 */
@Entity
@Table(name = "EVENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_seq")
    @SequenceGenerator(name = "event_seq", sequenceName = "SEQ_EVENT_ID", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    /**
     * 이 이벤트를 발생시킨 알림 규칙
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ALERT_EVENT_ID", nullable = false)
    private AlertEvent alertEvent;

    /**
     * 알림이 발생한 인스턴스
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INSTANCE_ID", nullable = false)
    private Instance instance;

    /**
     * 알림 정책 생성자 (알림 수신자)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID", nullable = false)
    private Member member;

    /**
     * 알림 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20, columnDefinition = "VARCHAR2(20) DEFAULT 'PENDING'")
    private AlertStatus status = AlertStatus.PENDING;

    /**
     * 심각도 (1=WARNING, 2=DANGER, 3=CRITICAL)
     */
    @Column(name = "SEVERITY", nullable = false)
    private Integer severity;

    /**
     * 알림 발생 시점의 메트릭 현재 값
     */
    @Column(name = "CURRENT_VALUE", nullable = false, columnDefinition = "NUMBER(10,2)")
    private Double currentValue;

    /**
     * 초과한 임계값
     */
    @Column(name = "THRESHOLD_VALUE", nullable = false, columnDefinition = "NUMBER(10,3)")
    private Double thresholdValue;

    /**
     * 임계치 포맷
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "THRESHOLD_FORMAT", nullable = false, length = 20)
    private ThresholdFormat thresholdFormat = ThresholdFormat.PERCENT;

    /**
     * 알림 메시지
     */
    @Column(name = "MESSAGE", nullable = false, length = 100)
    private String message;

    /**
     * 알림 확인 시간 (NULL=미확인)
     */
    @Column(name = "ACKNOWLEDGED_AT")
    private LocalDateTime acknowledgedAt;

    /**
     * 알림을 확인한 사용자 (NULL=미확인)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACKNOWLEDGED_BY")
    private Member acknowledgedBy;

    /**
     * 알림 해결 시간 (NULL=미해결)
     */
    @Column(name = "RESOLVED_AT")
    private LocalDateTime resolvedAt;

    /**
     * 알림을 해결한 사용자 (NULL=미해결)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESOLVED_BY")
    private Member resolvedBy;

    /**
     * 이 이벤트의 처리 이력들
     */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgressHistory> progressHistories = new ArrayList<>();

    @Builder
    public Event(AlertEvent alertEvent, Instance instance, Member member,
                 AlertStatus status, Integer severity, Double currentValue, Double thresholdValue,
                 ThresholdFormat thresholdFormat, String message) {
        this.alertEvent = alertEvent;
        this.instance = instance;
        this.member = member;
        this.status = status != null ? status : AlertStatus.PENDING;
        this.severity = severity;
        this.currentValue = currentValue;
        this.thresholdValue = thresholdValue;
        this.thresholdFormat = thresholdFormat != null ? thresholdFormat : ThresholdFormat.PERCENT;
        this.message = message;
    }

    /**
     * 알림 확인
     */
    public void acknowledge(Member acknowledgedBy) {
        this.status = AlertStatus.CLOSED;
        this.acknowledgedAt = LocalDateTime.now();
        this.acknowledgedBy = acknowledgedBy;
    }

    /**
     * 알림 해결
     */
    public void resolve(Member resolvedBy) {
        this.status = AlertStatus.CLOSED;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = resolvedBy;
    }
}

