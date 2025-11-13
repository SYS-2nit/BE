package com.sys.dbmonitor.domains.notification.domain;

import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 알림 규칙 엔티티
 * 각 알림 규칙은 하나의 그래프와 메트릭에 연결되며,
 * 임계값(WARNING/DANGER/CRITICAL), 누적 시간, 요일/시간대 등의 조건을 포함합니다.
 * 하나의 알림 정책(ALERT_POLICY)에 여러 알림 규칙이 포함될 수 있습니다.
 */
@Entity
@Table(name = "ALERT_EVENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AlertEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_event_seq")
    @SequenceGenerator(name = "alert_event_seq", sequenceName = "SEQ_ALERT_EVENT_ID", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    /**
     * 이 알림 규칙이 속한 알림 정책
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POLICY_ID", nullable = false)
    private AlertPolicy policy;

    /**
     * 알림 카테고리
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "CATEGORY", nullable = false, length = 20)
    private AlertCategory category;

    /**
     * 알림 규칙 활성화 상태 (true=활성화, false=비활성화)
     */
    @Column(name = "STATE", nullable = false, columnDefinition = "NUMBER(1) DEFAULT 1")
    private Boolean state = true;

    /**
     * 알림 규칙 이름
     */
    @Column(name = "NAME", nullable = false, length = 128)
    private String name;

    /**
     * 임계치 입력 포맷 (예: %, MS, MBPS, COUNT)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "THRESHOLD_FORMAT", nullable = false, length = 20, columnDefinition = "VARCHAR2(20) DEFAULT 'PERCENT'")
    private ThresholdFormat thresholdFormat = ThresholdFormat.PERCENT;

    /**
     * 경고(WARNING) 임계값
     */
    @Column(name = "WARNING", nullable = false, columnDefinition = "NUMBER(10,3) DEFAULT 50")
    private Double warning = 50.0;

    /**
     * 위험(DANGER) 임계값
     */
    @Column(name = "DANGER", nullable = false, columnDefinition = "NUMBER(10,3) DEFAULT 70")
    private Double danger = 70.0;

    /**
     * 치명(CRITICAL) 임계값
     */
    @Column(name = "CRITICAL", nullable = false, columnDefinition = "NUMBER(10,3) DEFAULT 90")
    private Double critical = 90.0;

    /**
     * 누적 시간 옵션
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "DELAY_TIME", nullable = false, length = 20, columnDefinition = "VARCHAR2(20) DEFAULT '1M'")
    private DelayTime delayTime = DelayTime.ONE_MINUTE;

    /**
     * 알림 수신 요일 (비트마스크, 0~127, 기본값: 127=모든 요일)
     * 1=일요일, 2=월요일, 4=화요일, 8=수요일, 16=목요일, 32=금요일, 64=토요일
     */
    @Column(name = "DAYS", nullable = false, columnDefinition = "NUMBER(3) DEFAULT 127")
    private Integer days = 127;

    /**
     * 알림 수신 시작 시간 (HH:mm 형식, NULL이면 제한 없음)
     */
    @Column(name = "START_TIME", length = 5)
    private String startTime;

    /**
     * 알림 수신 종료 시간 (HH:mm 형식, NULL이면 제한 없음)
     */
    @Column(name = "END_TIME", length = 5)
    private String endTime;

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
     * 역방향 메트릭 여부 (false=정상, true=역방향, 높을수록 문제가 아닌 경우)
     */
    @Column(name = "IS_REVERSE", nullable = false, columnDefinition = "NUMBER(1) DEFAULT 0")
    private Boolean isReverse = false;

    /**
     * 이 알림 규칙으로 발생한 이벤트들
     */
    @OneToMany(mappedBy = "alertEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Event> events = new ArrayList<>();

    @Builder
    public AlertEvent(AlertPolicy policy, AlertCategory category, Boolean state, String name,
                      ThresholdFormat thresholdFormat,
                      Double warning, Double danger, Double critical,
                      DelayTime delayTime, Integer days, String startTime, String endTime,
                      Graph graph, String metricKey, String metricName, Boolean isReverse) {
        this.policy = policy;
        this.category = category;
        this.state = state != null ? state : true;
        this.name = name;
        this.thresholdFormat = thresholdFormat != null ? thresholdFormat : ThresholdFormat.PERCENT;
        this.warning = warning != null ? warning : 50.0;
        this.danger = danger != null ? danger : 70.0;
        this.critical = critical != null ? critical : 90.0;
        this.delayTime = delayTime != null ? delayTime : DelayTime.ONE_MINUTE;
        this.days = days != null ? days : 127;
        this.startTime = startTime;
        this.endTime = endTime;
        this.graph = graph;
        this.metricKey = metricKey;
        this.metricName = metricName;
        this.isReverse = isReverse != null ? isReverse : false;
    }

    public void updateAlert(AlertPolicy policy,
                            AlertCategory category,
                            Graph graph,
                            String metricKey,
                            String metricName,
                            String name,
                            ThresholdFormat thresholdFormat,
                            Double warning,
                            Double danger,
                            Double critical,
                            DelayTime delayTime,
                            Integer days,
                            String startTime,
                            String endTime,
                            Boolean state,
                            Boolean isReverse) {
        if (policy != null) {
            this.policy = policy;
        }
        if (category != null) {
            this.category = category;
        }
        if (graph != null) {
            this.graph = graph;
        }
        if (metricKey != null) {
            this.metricKey = metricKey;
        }
        if (metricName != null) {
            this.metricName = metricName;
        }
        if (name != null) {
            this.name = name;
        }
        if (thresholdFormat != null) {
            this.thresholdFormat = thresholdFormat;
        }
        if (warning != null) {
            this.warning = warning;
        }
        if (danger != null) {
            this.danger = danger;
        }
        if (critical != null) {
            this.critical = critical;
        }
        if (delayTime != null) {
            this.delayTime = delayTime;
        }
        if (days != null) {
            this.days = days;
        }
        if (startTime != null) {
            this.startTime = startTime;
        }
        if (endTime != null) {
            this.endTime = endTime;
        }
        if (state != null) {
            this.state = state;
        }
        if (isReverse != null) {
            this.isReverse = isReverse;
        }
    }

    /**
     * 알림 규칙 활성화/비활성화 토글
     */
    public void toggleState() {
        this.state = !this.state;
    }
}

