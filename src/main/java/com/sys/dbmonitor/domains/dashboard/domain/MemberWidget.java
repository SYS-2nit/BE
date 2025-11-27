/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.dashboard.domain;

import com.sys.dbmonitor.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "member_widget")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MemberWidget extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_widget_seq")
    @SequenceGenerator(name = "member_widget_seq", sequenceName = "SEQ_MEMBER_WIDGET_ID", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "MEMBER_ID", nullable = false)
    private Long memberId;

    @Column(name = "GRAPH_ID", nullable = false)
    private Long graphId;

    @Column(name = "POSITION", nullable = false)
    private Integer position;

    @Builder
    public MemberWidget(Long memberId, Long graphId, Integer position) {
        this.memberId = memberId;
        this.graphId = graphId;
        this.position = position;
    }
    
    public void updatePosition(Integer position) {
        if (position != null) {
            this.position = position;
        }
    }

    public void updateGraph(Long graphId) {
        if (graphId != null) {
            this.graphId = graphId;
        }
    }

    public void updateWidget(Long graphId, Integer position) {
        updateGraph(graphId);
        updatePosition(position);
    }
}
