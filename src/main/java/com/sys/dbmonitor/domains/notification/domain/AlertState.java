/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.domain;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ALERT_STATE",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_ALERT_STATE", columnNames = {"INSTANCE_ID", "ALERT_EVENT_ID"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertState extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_state_seq")
    @SequenceGenerator(name = "alert_state_seq", sequenceName = "SEQ_ALERT_STATE_ID", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ALERT_EVENT_ID", nullable = false)
    private AlertEvent alertEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INSTANCE_ID", nullable = false)
    private Instance instance;

    // NULL=Normal, 1=WARNING, 2=DANGER, 3=CRITICAL
    @Column(name = "LAST_SEVERITY")
    private Integer lastSeverity;

    // 마지막으로 발송한 심각도(NULL=정상/미발송)
    @Column(name = "LAST_NOTIFIED_SEVERITY")
    private Integer lastNotifiedSeverity;

    @Column(name = "CONSECUTIVE_COUNT", nullable = false)
    private Integer consecutiveCount = 0;

    @Column(name = "LAST_CHECKED_AT")
    private LocalDateTime lastCheckedAt;

    @Column(name = "LAST_NOTIFIED_AT")
    private LocalDateTime lastNotifiedAt;

    @Builder
    public AlertState(AlertEvent alertEvent,
                      Instance instance,
                      Integer lastSeverity,
                      Integer lastNotifiedSeverity,
                      Integer consecutiveCount,
                      LocalDateTime lastCheckedAt,
                      LocalDateTime lastNotifiedAt) {
        this.alertEvent = alertEvent;
        this.instance = instance;
        this.lastSeverity = lastSeverity;
        this.lastNotifiedSeverity = lastNotifiedSeverity;
        this.consecutiveCount = consecutiveCount != null ? consecutiveCount : 0;
        this.lastCheckedAt = lastCheckedAt;
        this.lastNotifiedAt = lastNotifiedAt;
    }

    public void setLastSeverity(Integer lastSeverity) {
        this.lastSeverity = lastSeverity;
    }

    public void setLastNotifiedSeverity(Integer lastNotifiedSeverity) {
        this.lastNotifiedSeverity = lastNotifiedSeverity;
    }

    public void setConsecutiveCount(Integer consecutiveCount) {
        this.consecutiveCount = consecutiveCount;
    }

    public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public void setLastNotifiedAt(LocalDateTime lastNotifiedAt) {
        this.lastNotifiedAt = lastNotifiedAt;
    }
}


