/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.AlertEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.sys.dbmonitor.domains.notification.domain.QAlertEvent.alertEvent;
import static com.sys.dbmonitor.domains.notification.domain.QAlertPolicy.alertPolicy;
import static com.sys.dbmonitor.domains.graph.domain.QGraph.graph;

@Repository
@RequiredArgsConstructor
public class AlertEventRepositoryImpl implements AlertEventRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AlertEvent> findByPolicyId(Long policyId) {
        return queryFactory
                .selectFrom(alertEvent)
                .where(
                        alertEvent.policy.id.eq(policyId),
                        alertEvent.isDeleted.eq(false)
                )
                .orderBy(alertEvent.createdAt.desc())
                .fetch();
    }

    @Override
    public List<AlertEvent> findActiveEventsByInstanceId(Long instanceId, Long memberId) {
        return queryFactory
                .selectFrom(alertEvent)
                .join(alertEvent.policy, alertPolicy)
                .where(
                        alertPolicy.instance.id.eq(instanceId),
                        alertPolicy.member.id.eq(memberId),
                        alertPolicy.isActive.eq(true),
                        alertEvent.state.eq(true),
                        alertPolicy.isDeleted.eq(false),
                        alertEvent.isDeleted.eq(false)
                )
                .fetch();
    }

    @Override
    public List<AlertEvent> findActiveEventsByInstanceIdForBatch(Long instanceId) {
        return queryFactory
                .selectFrom(alertEvent)
                .join(alertEvent.policy, alertPolicy)
                .where(
                        alertPolicy.instance.id.eq(instanceId),
                        alertPolicy.isActive.eq(true),
                        alertEvent.state.eq(true),
                        alertPolicy.isDeleted.eq(false),
                        alertEvent.isDeleted.eq(false)
                )
                .fetch();
    }

    @Override
    public List<AlertEvent> findActiveEventsByCategory(AlertCategory category, Long memberId) {
        return queryFactory
                .selectFrom(alertEvent)
                .join(alertEvent.policy, alertPolicy)
                .where(
                        alertEvent.category.eq(category),
                        alertPolicy.member.id.eq(memberId),
                        alertPolicy.isActive.eq(true),
                        alertEvent.state.eq(true),
                        alertPolicy.isDeleted.eq(false),
                        alertEvent.isDeleted.eq(false)
                )
                .fetch();
    }

    @Override
    public List<AlertEvent> findActiveEventsByInstanceIdAndCategory(Long instanceId, AlertCategory category, Long memberId) {
        return queryFactory
                .selectFrom(alertEvent)
                .join(alertEvent.policy, alertPolicy)
                .where(
                        alertPolicy.instance.id.eq(instanceId),
                        alertEvent.category.eq(category),
                        alertPolicy.member.id.eq(memberId),
                        alertPolicy.isActive.eq(true),
                        alertEvent.state.eq(true),
                        alertPolicy.isDeleted.eq(false),
                        alertEvent.isDeleted.eq(false)
                )
                .fetch();
    }

    @Override
    public Optional<AlertEvent> findByIdAndNotDeleted(Long id) {
        AlertEvent result = queryFactory
                .selectFrom(alertEvent)
                .leftJoin(alertEvent.graph, graph).fetchJoin()
                .where(
                        alertEvent.id.eq(id),
                        alertEvent.isDeleted.eq(false)
                )
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<AlertEvent> findByIdIncludingDeleted(Long id) {
        AlertEvent result = queryFactory
                .selectFrom(alertEvent)
                .leftJoin(alertEvent.graph, graph).fetchJoin()
                .where(alertEvent.id.eq(id))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public List<AlertEvent> findActiveByGraphIdAndInstanceId(Long graphId, Long instanceId, Long memberId) {
        return queryFactory
                .selectFrom(alertEvent)
                .join(alertEvent.policy, alertPolicy)
                .where(
                        alertEvent.graph.id.eq(graphId),
                        alertPolicy.instance.id.eq(instanceId),
                        alertPolicy.member.id.eq(memberId),
                        alertPolicy.isActive.eq(true),
                        alertEvent.state.eq(true),
                        alertPolicy.isDeleted.eq(false),
                        alertEvent.isDeleted.eq(false)
                )
                .fetch();
    }
}

