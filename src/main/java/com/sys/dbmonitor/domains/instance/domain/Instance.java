package com.sys.dbmonitor.domains.instance.domain;

import com.sys.dbmonitor.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@Table(name = "instance")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Instance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "instance_seq")
    @SequenceGenerator(name = "instance_seq", sequenceName = "SEQ_INSTANCE_ID", allocationSize = 1)
    @Column(name = "ID")
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DB_ID", nullable = false)
    private DBInfo dbInfo;

    @Column(name = "SID", nullable = false, length = 100)
    private String sid;

    @Column(name = "URL", nullable = false, length = 500)
    private String url;

    @Column(name = "CONNECTION_TYPE", length = 20)
    private String connectionType; // "SID" or "SERVICE_NAME"

    @Builder
    public Instance(DBInfo dbInfo, String sid, String url, String connectionType) {
        this.dbInfo = dbInfo;
        this.sid = sid;
        this.url = url;
        this.connectionType = connectionType != null ? connectionType : "SID"; // 기본값 SID
    }

    public void update(String sid, String url, String connectionType) {
        if (sid != null) this.sid = sid;
        if (url != null) this.url = url;
        if (connectionType != null) this.connectionType = connectionType;
    }
}

