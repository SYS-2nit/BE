package com.sys.dbmonitor.domains.instance.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.sys.dbmonitor.domains.instance.domain.QDBInfo.dBInfo;
import static com.sys.dbmonitor.domains.instance.domain.QInstance.instance;


@Repository
@RequiredArgsConstructor
public class InstanceRepositoryImpl implements InstanceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Instance> findByIdWithDbInfoAndIsDeletedFalse(Long id) {
        Instance result = queryFactory
                .selectFrom(instance)
                .join(instance.dbInfo, dBInfo).fetchJoin()
                .where(
                        instance.id.eq(id),
                        instance.isDeleted.eq(false)
                )
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public List<Instance> findByDbInfoName(String name) {
        return queryFactory
                .selectFrom(instance)
                .join(instance.dbInfo, dBInfo)
                .where(
                        dBInfo.name.eq(name),
                        instance.isDeleted.eq(false)
                )
                .fetch();
    }

    @Override
    public Optional<Instance> findSidInstancesByDbInfoId(Long dbInfoId) {
        Instance result = queryFactory
                .selectFrom(instance)
                .join(instance.dbInfo, dBInfo)
                .where(
                        dBInfo.id.eq(dbInfoId),
                        instance.connectionType.eq("SID"),
                        instance.isDeleted.eq(false)
                )
                .orderBy(instance.id.asc())
                .fetchFirst();
        return Optional.ofNullable(result);
    }
}

