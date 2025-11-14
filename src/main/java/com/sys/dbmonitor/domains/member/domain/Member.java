package com.sys.dbmonitor.domains.member.domain;

import com.sys.dbmonitor.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq")
    @SequenceGenerator(name = "member_seq", sequenceName = "SEQ_MEMBER_ID", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    /**
     * 비밀번호 (BCrypt 해시)
     */
    @Column(nullable = false, length = 500)
    private String password;

    /**
     * 이메일
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * 회사명
     */
    @Column(nullable = false, length = 100)
    private String company;

    /**
     * Slack 주소 (nullable)
     */
    @Column(length = 500)
    private String slackAddress;

    /**
     * 경고 채널 (nullable)
     */
    @Column(length = 200)
    private String warningChannel;

    /**
     * 심각 채널 (nullable)
     */
    @Column(length = 200)
    private String criticalChannel;

    @Builder
    public Member(String username, String password, String email, String company) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.company = company;
    }

    /**
     * 기본 정보 수정 (username, email, company)
     */
    public void update(String username, String email, String company) {
        if (username != null) this.username = username;
        if (email != null) this.email = email;
        if (company != null) this.company = company;
    }


    public void updateAddress(String email, String slackAddress, String warningChannel, String criticalChannel) {
        // 1. email을 먼저 업데이트 (최신 값 보장)
        if (email != null) {
            this.email = email;
        }
        
        // 2. slackAddress 업데이트
        if (slackAddress != null) {
            this.slackAddress = slackAddress;
        }
        
        // 3. warningChannel 처리 (email이 최신화된 후 처리)
        if (warningChannel != null) {
            if (warningChannel.equals("email")) {
                // email이 최신화된 후이므로 this.email은 최신 값임
                this.warningChannel = this.email;
            } else {
                this.warningChannel = warningChannel;
            }
        }
        
        // 4. criticalChannel 업데이트
        if (criticalChannel != null) {
            this.criticalChannel = criticalChannel;
        }
    }
}