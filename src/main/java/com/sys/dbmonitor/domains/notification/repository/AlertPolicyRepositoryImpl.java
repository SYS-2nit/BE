/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sys.dbmonitor.domains.notification.domain.AlertPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.sys.dbmonitor.domains.notification.domain.QAlertPolicy.alertPolicy;

@Repository
@RequiredArgsConstructor
public class AlertPolicyRepositoryImpl implements AlertPolicyRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AlertPolicy> findByMemberId(Long memberId) {
        return queryFactory
                .selectFrom(alertPolicy)
                .where(
                        alertPolicy.member.id.eq(memberId),
                        alertPolicy.isDeleted.eq(false)
                )
                .orderBy(alertPolicy.createdAt.desc())
                .fetch();
    }

    @Override
    public List<AlertPolicy> findByInstanceId(Long instanceId) {
        return queryFactory
                .selectFrom(alertPolicy)
                .where(
                        alertPolicy.instance.id.eq(instanceId),
                        alertPolicy.isDeleted.eq(false)
                )
                .orderBy(alertPolicy.createdAt.desc())
                .fetch();
    }

    @Override
    public List<AlertPolicy> findByMemberIdAndInstanceId(Long memberId, Long instanceId) {
        return queryFactory
                .selectFrom(alertPolicy)
                .where(
                        alertPolicy.member.id.eq(memberId),
                        alertPolicy.instance.id.eq(instanceId),
                        alertPolicy.isDeleted.eq(false)
                )
                .orderBy(alertPolicy.createdAt.desc())
                .fetch();
    }

    @Override
    public List<AlertPolicy> findActivePolicies() {
        return queryFactory
                .selectFrom(alertPolicy)
                .where(
                        alertPolicy.isActive.eq(true),
                        alertPolicy.isDeleted.eq(false)
                )
                .fetch();
    }

    @Override
    public List<AlertPolicy> findActivePoliciesByInstanceId(Long instanceId) {
        return queryFactory
                .selectFrom(alertPolicy)
                .where(
                        alertPolicy.instance.id.eq(instanceId),
                        alertPolicy.isActive.eq(true),
                        alertPolicy.isDeleted.eq(false)
                )
                .fetch();
    }

    @Override
    public Optional<AlertPolicy> findByIdAndNotDeleted(Long id) {
        AlertPolicy result = queryFactory
                .selectFrom(alertPolicy)
                .where(
                        alertPolicy.id.eq(id),
                        alertPolicy.isDeleted.eq(false)
                )
                .fetchOne();
        return Optional.ofNullable(result);
    }
}

