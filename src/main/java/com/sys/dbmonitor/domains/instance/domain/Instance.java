package com.sys.dbmonitor.domains.instance.domain;

import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@Table(name = "INSTANCE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Instance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "instance_seq")
    @SequenceGenerator(name = "instance_seq", sequenceName = "SEQ_INSTANCE_ID", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    /**
     * 타겟 DB 이름 (사용자가 지정)
     */
    @Column(name = "NAME", nullable = false, unique = true, length = 100)
    private String name;

    /**
     * 타겟 DB 연결 URL
     */
    @Column(name = "URL", nullable = false, length = 500)
    private String url;

    /**
     * 타겟 DB 사용자명
     */
    @Column(name = "USERNAME", nullable = false, length = 100)
    private String username;

    /**
     * 타겟 DB 비밀번호
     */
    @Column(name = "PASSWORD", nullable = false, length = 500)
    private String password;

    /**
     * 활성화 여부 (true: 사용 가능, false: 비활성화)
     */
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean isActive;

    // TODO :: 유저 데이터 연동하기
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder
    public Instance(String name, String url, String username, String password, Boolean isActive,  Member member) {
        this.name = name;
        this.url = url;
        this.username = username;
        this.password = password;
        this.isActive = isActive != null ? isActive : true;
        this.member = member;
    }

    /**
     * 타겟 DB 정보 수정
     */
    public void update(String name, String url, String username, String password,  Boolean isActive) {
        if (name != null) this.name = name;
        if (url != null) this.url = url;
        if (username != null) this.username = username;
        if (password != null) this.password = password;
        if (isActive != null) this.isActive = isActive;
    }

    /**
     * 활성화 상태 변경
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 비활성화 상태 변경
     */
    public void deactivate() {
        this.isActive = false;
    }
}

