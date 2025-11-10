package com.sys.dbmonitor.domains.sql.domain;

import com.sys.dbmonitor.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "sql_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Sql extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instance_id", nullable = false)
    private Long instanceId;

    @Column(name = "field3", length = 255)
    private String field3;

    @Column(name = "field4", length = 255)
    private String field4;

    @Column(name = "field5", length = 255)
    private String field5;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Builder
    public Sql(Long instanceId, String field3, String field4, String field5, Boolean isDeleted) {
        this.instanceId = instanceId;
        this.field3 = field3;
        this.field4 = field4;
        this.field5 = field5;
        this.isDeleted = isDeleted != null ? isDeleted : false;
    }

    /** 업데이트 로직 */
    public void update(String field3, String field4, String field5) {
        if (field3 != null) this.field3 = field3;
        if (field4 != null) this.field4 = field4;
        if (field5 != null) this.field5 = field5;
        touchUpdatedAt();
    }

    /** 소프트 삭제 로직 */
    public void delete() {
        this.isDeleted = true;
        touchUpdatedAt();
    }

    /** 수동 업데이트 시간 반영 */
    public void touchUpdatedAt() {
        super.setUpdatedAt(LocalDateTime.now()); // 이제 에러 사라짐 ✅
    }
}
