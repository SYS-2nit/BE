package com.sys.dbmonitor.domains.notification.domain;

import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 알림 메트릭 템플릿 엔티티
 * 카테고리별로 선정된 알림 메트릭 템플릿을 저장하는 테이블.
 * 사용자가 알림 규칙을 생성할 때 선택할 수 있는 메트릭 목록을 제공합니다.
 */
@Entity
@Table(name = "ALERT_METRIC_TEMPLATE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AlertMetricTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_metric_template_seq")
    @SequenceGenerator(name = "alert_metric_template_seq", sequenceName = "SEQ_ALERT_METRIC_TEMPLATE_ID", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    /**
     * 메트릭 카테고리
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "CATEGORY", nullable = false, length = 20)
    private AlertCategory category;

    /**
     * 연결된 그래프
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GRAPH_ID", nullable = false)
    private Graph graph;

    /**
     * 메트릭 키 (GraphRegistry의 컬럼명, 예: "HOST_CPU_UTIL_PCT")
     */
    @Column(name = "METRIC_KEY", nullable = false, length = 100)
    private String metricKey;

    /**
     * 메트릭 이름 (표시용, 예: "Host CPU 사용률")
     */
    @Column(name = "METRIC_NAME", nullable = false, length = 200)
    private String metricName;

    /**
     * 기본 경고(WARNING) 임계값 (0~100 범위, 선택 사항)
     */
    @Column(name = "DEFAULT_WARNING", precision = 5, scale = 2)
    private Double defaultWarning;

    /**
     * 기본 위험(DANGER) 임계값 (0~100 범위, 선택 사항)
     */
    @Column(name = "DEFAULT_DANGER", precision = 5, scale = 2)
    private Double defaultDanger;

    /**
     * 기본 치명(CRITICAL) 임계값 (0~100 범위, 선택 사항)
     */
    @Column(name = "DEFAULT_CRITICAL", precision = 5, scale = 2)
    private Double defaultCritical;

    /**
     * 메트릭 설명 (선택 사항)
     */
    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    /**
     * 템플릿 활성화 여부 (true=활성화, false=비활성화)
     */
    @Column(name = "IS_ACTIVE", nullable = false, columnDefinition = "NUMBER(1) DEFAULT 1")
    private Boolean isActive = true;

    @Builder
    public AlertMetricTemplate(AlertCategory category, Graph graph, String metricKey, String metricName,
                               Double defaultWarning, Double defaultDanger, Double defaultCritical,
                               String description, Boolean isActive) {
        this.category = category;
        this.graph = graph;
        this.metricKey = metricKey;
        this.metricName = metricName;
        this.defaultWarning = defaultWarning;
        this.defaultDanger = defaultDanger;
        this.defaultCritical = defaultCritical;
        this.description = description;
        this.isActive = isActive != null ? isActive : true;
    }

    /**
     * 템플릿 정보 수정
     */
    public void update(Double defaultWarning, Double defaultDanger, Double defaultCritical,
                      String description, Boolean isActive) {
        if (defaultWarning != null) this.defaultWarning = defaultWarning;
        if (defaultDanger != null) this.defaultDanger = defaultDanger;
        if (defaultCritical != null) this.defaultCritical = defaultCritical;
        if (description != null) this.description = description;
        if (isActive != null) this.isActive = isActive;
    }

    /**
     * 템플릿 활성화/비활성화 토글
     */
    public void toggleActive() {
        this.isActive = !this.isActive;
    }
}

