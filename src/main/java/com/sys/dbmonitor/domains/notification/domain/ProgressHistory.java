package com.sys.dbmonitor.domains.notification.domain;

import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 알림 처리 이력 엔티티
 * 알림 이벤트의 처리 이력을 저장하는 테이블.
 * 사용자가 알림을 확인하거나 해결할 때, 또는 추가 조치 사항을 기록할 때 사용됩니다.
 */
@Entity
@Table(name = "PROGRESS_HISTORY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ProgressHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "progress_history_seq")
    @SequenceGenerator(name = "progress_history_seq", sequenceName = "SEQ_PROGRESS_HISTORY_ID", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    /**
     * 처리 이력이 속한 알림 이벤트
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EVENT_ID", nullable = false)
    private Event event;

    /**
     * 처리 이력 내용
     */
    @Column(name = "CONTENT", length = 256)
    private String content;

    /**
     * 이력을 작성한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CREATED_BY", nullable = false)
    private Member createdBy;

    @Builder
    public ProgressHistory(Event event, String content, Member createdBy) {
        this.event = event;
        this.content = content;
        this.createdBy = createdBy;
    }

    /**
     * 처리 이력 내용 수정
     */
    public void updateContent(String content) {
        if (content != null) {
            this.content = content;
        }
    }
}

