/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.instance.domain;

import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * DB 정보 엔티티
 * 데이터베이스 연결 정보를 저장하는 테이블
 */
@Entity
@Table(name = "db_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DBInfo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "db_info_seq")
    @SequenceGenerator(name = "db_info_seq", sequenceName = "SEQ_DB_INFO_ID", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    /**
     * 유저 ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID", nullable = false)
    private Member member;

    /**
     * 데이터베이스 타입 (Oracle, MySQL 등)
     */
    @Column(name = "TYPE", nullable = true, length = 64)
    private String type;

    /**
     * 버전
     */
    @Column(name = "VERSION", nullable = true, length = 10)
    private String version;

    /**
     * DB 이름 (사용자 커스텀 이름)
     */
    @Column(name = "NAME", nullable = false, length = 128)
    private String name;

    /**
     * Host IP 주소
     */
    @Column(name = "IP", nullable = false, length = 200)
    private String ip;

    /**
     * PORT 번호
     */
    @Column(name = "PORT", nullable = false)
    private Integer port;

    /**
     * DB 접속 계정
     */
    @Column(name = "USER_NAME", nullable = false, length = 100)
    private String userName;

    /**
     * DB 접속 비밀번호 (암호화되어 저장)
     */
    @Column(name = "PASSWORD", nullable = false, length = 255)
    private String password;

    /**
     * 활성화 여부
     */
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean isActive;

    /**
     * 마지막 접속 시간
     */
    @Column(name = "FINAL_AT")
    private LocalDateTime finalAt;

    @Builder
    public DBInfo(Member member, String type, String version, String name, String ip, 
                  Integer port, String userName, String password, Boolean isActive, LocalDateTime finalAt) {
        this.member = member;
        this.type = type;
        this.version = version;
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.isActive = isActive != null ? isActive : true;
        this.finalAt = finalAt;
    }

    /**
     * DB 정보 수정
     */
    public void update(String type, String version, String name, String ip, Integer port, 
                      String userName, String password, Boolean isActive) {
        if (type != null) this.type = type;
        if (version != null) this.version = version;
        if (name != null) this.name = name;
        if (ip != null) this.ip = ip;
        if (port != null) this.port = port;
        if (userName != null) this.userName = userName;
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

    /**
     * 마지막 접속 시간 업데이트
     */
    public void updateFinalAt() {
        this.finalAt = LocalDateTime.now();
    }

    /**
     * JDBC URL 생성 (SID 형식: jdbc:oracle:thin:@host:port:sid)
     */
    public String generateJdbcUrl(String identifier) {
            return String.format("jdbc:oracle:thin:@%s:%d:%s", this.ip, this.port, identifier);
    }

    /**
     * JDBC URL 생성 (Service Name 형식: jdbc:oracle:thin:@host:port/serviceName)
     */
    public String generateJdbcUrlForServiceName(String serviceName) {
        return String.format("jdbc:oracle:thin:@%s:%d/%s", this.ip, this.port, serviceName);
    }

    /**
     * JDBC URL 생성 (connectionType에 따라 자동 선택)
     * @param identifier SID 또는 서비스 이름
     * @param connectionType "SID" 또는 "SERVICE_NAME"
     */
    public String generateJdbcUrl(String identifier, String connectionType) {
        if ("SERVICE_NAME".equalsIgnoreCase(connectionType)) {
            return generateJdbcUrlForServiceName(identifier);
        } else {
            return generateJdbcUrl(identifier);
        }
    }

    /**
     * JDBC URL 생성 (Service Name 형식: jdbc:oracle:thin:@host:port/serviceName)
     * Instance 테이블에 저장할 URL 형식 (하위 호환성 유지)
     * @deprecated connectionType을 사용하는 generateJdbcUrl(String, String) 사용 권장
     */
    @Deprecated
    public String generateJdbcUrlForInstance(String identifier) {
        return String.format("jdbc:oracle:thin:@%s:%d/%s", this.ip, this.port, identifier);
    }
}

