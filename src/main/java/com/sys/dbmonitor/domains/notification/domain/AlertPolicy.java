/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 알림 정책 엔티티
 * 하나의 인스턴스에 대해 하나 이상의 알림 정책을 생성할 수 있으며,
 * 각 정책은 여러 개의 알림 규칙(ALERT_EVENT)을 포함할 수 있습니다.
 * 정책 단위로 활성화/비활성화가 가능하며, 정책 생성자(MEMBER_ID)만 해당 정책의 알림을 수신합니다.
 */
@Entity
@Table(name = "ALERT_POLICY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AlertPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_policy_seq")
    @SequenceGenerator(name = "alert_policy_seq", sequenceName = "SEQ_ALERT_POLICY_ID", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    /**
     * 정책을 생성한 사용자 (정책 생성자만 알림 수신)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID", nullable = false)
    private Member member;

    /**
     * 모니터링 대상 인스턴스
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INSTANCE_ID", nullable = false)
    private Instance instance;

    /**
     * 알림 정책 이름
     */
    @Column(name = "NAME", nullable = false, length = 200)
    private String name;

    /**
     * 알림 정책 설명 (선택 사항)
     */
    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    /**
     * 정책 활성화 여부 (true=활성화, false=비활성화)
     * 비활성화 시 알림 체크 안 함
     */
    @Column(name = "IS_ACTIVE", nullable = false, columnDefinition = "NUMBER(1) DEFAULT 1")
    private Boolean isActive = true;

    /**
     * 이 정책에 속한 알림 규칙들
     */
    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AlertEvent> alertEvents = new ArrayList<>();

    /**
     * 생성 일시 (BaseEntity의 createdAt을 오버라이드하여 직접 관리)
     * @PrePersist에서 한국 시간으로 설정
     */
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시 (BaseEntity의 updatedAt을 오버라이드하여 직접 관리)
     * @PreUpdate에서 한국 시간으로 설정
     */
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    /**
     * JPA 저장 전 실행: createdAt을 한국 시간으로 직접 설정
     * BaseEntity의 @CreatedDate를 무시하고 직접 설정
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        }
    }

    /**
     * JPA 수정 전 실행: updatedAt을 한국 시간으로 직접 설정
     * BaseEntity의 @LastModifiedDate를 무시하고 직접 설정
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    @Builder
    public AlertPolicy(Member member, Instance instance, String name, String description, Boolean isActive) {
        this.member = member;
        this.instance = instance;
        this.name = name;
        this.description = description;
        this.isActive = isActive != null ? isActive : true;
    }

    /**
     * 정책 정보 수정
     */
    public void update(String name, String description, Boolean isActive) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (isActive != null) this.isActive = isActive;
    }

    /**
     * 정책 활성화/비활성화 토글
     */
    public void toggleActive() {
        this.isActive = !this.isActive;
    }
}

