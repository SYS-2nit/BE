/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sys.dbmonitor.domains.notification.domain.AlertCategory;
import com.sys.dbmonitor.domains.notification.domain.AlertStatus;
import com.sys.dbmonitor.domains.notification.domain.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.sys.dbmonitor.domains.notification.domain.QEvent.event;
import static com.sys.dbmonitor.domains.notification.domain.QAlertEvent.alertEvent;
import static com.sys.dbmonitor.domains.notification.domain.QAlertPolicy.alertPolicy;
import static com.sys.dbmonitor.domains.graph.domain.QGraph.graph;
import static com.sys.dbmonitor.domains.instance.domain.QInstance.instance;
import static com.sys.dbmonitor.domains.member.domain.QMember.member;

@Repository
@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Event> findByMemberId(Long memberId) {
        return queryFactory
                .selectFrom(event)
                .leftJoin(event.alertEvent, alertEvent).fetchJoin()
                .where(
                        event.member.id.eq(memberId),
                        event.isDeleted.eq(false)
                )
                .orderBy(event.createdAt.desc())
                .fetch();
    }

    @Override
    public Page<Event> findByMemberId(Long memberId, Pageable pageable) {
        JPAQuery<Event> query = queryFactory
                .selectFrom(event)
                .leftJoin(event.alertEvent, alertEvent).fetchJoin()
                .where(
                        event.member.id.eq(memberId),
                        event.isDeleted.eq(false)
                )
                .orderBy(event.createdAt.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(event.count())
                .from(event)
                .where(
                        event.member.id.eq(memberId),
                        event.isDeleted.eq(false)
                );

        List<Event> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public List<Event> findByInstanceId(Long instanceId, Long memberId) {
        return queryFactory
                .selectFrom(event)
                .where(
                        event.instance.id.eq(instanceId),
                        event.member.id.eq(memberId),
                        event.isDeleted.eq(false)
                )
                .orderBy(event.createdAt.desc())
                .fetch();
    }

    @Override
    public Page<Event> findByInstanceId(Long instanceId, Long memberId, Pageable pageable) {
        JPAQuery<Event> query = queryFactory
                .selectFrom(event)
                .where(
                        event.instance.id.eq(instanceId),
                        event.member.id.eq(memberId),
                        event.isDeleted.eq(false)
                )
                .orderBy(event.createdAt.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(event.count())
                .from(event)
                .where(
                        event.instance.id.eq(instanceId),
                        event.member.id.eq(memberId),
                        event.isDeleted.eq(false)
                );

        List<Event> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public List<Event> findByStatus(AlertStatus status, Long memberId) {
        return queryFactory
                .selectFrom(event)
                .where(
                        event.status.eq(status),
                        event.member.id.eq(memberId),
                        event.isDeleted.eq(false)
                )
                .orderBy(event.createdAt.desc())
                .fetch();
    }

    @Override
    public Page<Event> findByStatus(AlertStatus status, Long memberId, Pageable pageable) {
        JPAQuery<Event> query = queryFactory
                .selectFrom(event)
                .where(
                        event.status.eq(status),
                        event.member.id.eq(memberId),
                        event.isDeleted.eq(false)
                )
                .orderBy(event.createdAt.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(event.count())
                .from(event)
                .where(
                        event.status.eq(status),
                        event.member.id.eq(memberId),
                        event.isDeleted.eq(false)
                );

        List<Event> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public List<Event> findBySeverity(Integer severity, Long memberId) {
        return queryFactory
                .selectFrom(event)
                .where(
                        event.severity.eq(severity),
                        event.member.id.eq(memberId),
                        event.isDeleted.eq(false)
                )
                .orderBy(event.createdAt.desc())
                .fetch();
    }

    @Override
    public Page<Event> findBySeverity(Integer severity, Long memberId, Pageable pageable) {
        JPAQuery<Event> query = queryFactory
                .selectFrom(event)
                .where(
                        event.severity.eq(severity),
                        event.member.id.eq(memberId),
                        event.isDeleted.eq(false)
                )
                .orderBy(event.createdAt.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(event.count())
                .from(event)
                .where(
                        event.severity.eq(severity),
                        event.member.id.eq(memberId),
                        event.isDeleted.eq(false)
                );

        List<Event> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<Event> findByConditions(Long memberId, Long instanceId, AlertStatus status, Integer severity, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(event.isDeleted.eq(false));

        if (memberId != null) {
            builder.and(event.member.id.eq(memberId));
        }
        if (instanceId != null) {
            builder.and(event.instance.id.eq(instanceId));
        }
        if (status != null) {
            builder.and(event.status.eq(status));
        }
        if (severity != null) {
            builder.and(event.severity.eq(severity));
        }

        JPAQuery<Event> query = queryFactory
                .selectDistinct(event)
                .from(event)
                .leftJoin(event.alertEvent, alertEvent).fetchJoin()
                .where(builder)
                .orderBy(event.createdAt.desc());

        JPAQuery<Long> countQuery = queryFactory
                .select(event.countDistinct())
                .from(event)
                .leftJoin(event.alertEvent, alertEvent)
                .where(builder);

        List<Event> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public List<Event> findByAlertEventId(Long alertEventId) {
        return queryFactory
                .selectFrom(event)
                .where(
                        event.alertEvent.id.eq(alertEventId),
                        event.isDeleted.eq(false)
                )
                .orderBy(event.createdAt.desc())
                .fetch();
    }

    @Override
    public Optional<Event> findByIdAndNotDeleted(Long id) {
        Event result = queryFactory
                .selectFrom(event)
                .leftJoin(event.alertEvent, alertEvent).fetchJoin()
                .where(
                        event.id.eq(id),
                        event.isDeleted.eq(false)
                )
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public Integer findMaxSeverityByInstanceId(Long instanceId) {
        return queryFactory
                .select(event.severity.max())
                .from(event)
                .where(
                        event.instance.id.eq(instanceId),
                        event.status.eq(AlertStatus.PENDING),
                        event.isDeleted.eq(false)
                )
                .fetchOne();
    }

    @Override
    public List<Event> findFilteredEventsForPDF(
            Long memberId,
            AlertCategory category,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer severity,
            AlertStatus status,
            String readStatus
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(event.member.id.eq(memberId));
        builder.and(event.isDeleted.eq(false));

        if (category != null) {
            builder.and(alertEvent.category.eq(category));
        }
        if (startDate != null) {
            builder.and(event.createdAt.goe(startDate));
        }
        if (endDate != null) {
            builder.and(event.createdAt.loe(endDate));
        }
        if (severity != null) {
            builder.and(event.severity.eq(severity));
        }
        if (status != null) {
            builder.and(event.status.eq(status));
        }
        if (readStatus != null) {
            if ("read".equals(readStatus)) {
                builder.and(event.acknowledgedAt.isNotNull());
            } else if ("unread".equals(readStatus)) {
                builder.and(event.acknowledgedAt.isNull());
            }
        }

        return queryFactory
                .selectDistinct(event)
                .from(event)
                .leftJoin(event.alertEvent, alertEvent).fetchJoin()
                .leftJoin(alertEvent.graph, graph).fetchJoin()
                .leftJoin(event.instance, instance).fetchJoin()
                .leftJoin(event.member, member).fetchJoin()
                .where(builder)
                .orderBy(event.createdAt.desc())
                .fetch();
    }

    @Override
    public Long countEventsByDateRange(
            Long memberId,
            Long instanceId,
            AlertCategory category,
            Integer severity,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(event.member.id.eq(memberId));
        builder.and(event.isDeleted.eq(false));

        if (instanceId != null) {
            builder.and(event.instance.id.eq(instanceId));
        }
        if (category != null) {
            builder.and(alertEvent.category.eq(category));
        }
        if (severity != null) {
            builder.and(event.severity.eq(severity));
        }
        builder.and(event.createdAt.goe(startDate));
        builder.and(event.createdAt.lt(endDate));
        builder.and(alertPolicy.isActive.eq(true));
        builder.and(alertEvent.state.eq(true));
        builder.and(alertPolicy.isDeleted.eq(false));
        builder.and(alertEvent.isDeleted.eq(false));

        return queryFactory
                .select(event.count())
                .from(event)
                .leftJoin(event.alertEvent, alertEvent)
                .leftJoin(alertEvent.policy, alertPolicy)
                .where(builder)
                .fetchOne();
    }

    @Override
    public List<String> findDistinctMetricKeysByDateRange(
            Long memberId,
            Long instanceId,
            AlertCategory category,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(event.member.id.eq(memberId));
        builder.and(event.isDeleted.eq(false));

        if (instanceId != null) {
            builder.and(event.instance.id.eq(instanceId));
        }
        if (category != null) {
            builder.and(alertEvent.category.eq(category));
        }
        builder.and(event.createdAt.goe(startDate));
        builder.and(event.createdAt.lt(endDate));
        builder.and(alertPolicy.isActive.eq(true));
        builder.and(alertEvent.state.eq(true));
        builder.and(alertPolicy.isDeleted.eq(false));
        builder.and(alertEvent.isDeleted.eq(false));

        return queryFactory
                .selectDistinct(alertEvent.metricKey)
                .from(event)
                .leftJoin(event.alertEvent, alertEvent)
                .leftJoin(alertEvent.policy, alertPolicy)
                .where(builder)
                .fetch();
    }

    @Override
    public List<Event> findByInstanceIdForTest(Long instanceId) {
        return queryFactory
                .selectFrom(event)
                .where(
                        event.instance.id.eq(instanceId),
                        event.isDeleted.eq(false)
                )
                .orderBy(event.createdAt.desc())
                .fetch();
    }
}

