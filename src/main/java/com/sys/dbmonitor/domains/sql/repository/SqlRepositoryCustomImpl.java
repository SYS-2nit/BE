package com.sys.dbmonitor.domains.sql.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sys.dbmonitor.domains.sql.domain.QSql;
import com.sys.dbmonitor.domains.sql.domain.Sql;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class SqlRepositoryCustomImpl implements SqlRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Sql> findForGraph(
            Long instanceId,
            String filter,
            LocalDateTime start,
            LocalDateTime end
    ) {

        QSql sql = QSql.sql;

        return queryFactory
                .selectFrom(sql)
                .where(
                        sql.isDeleted.eq(false),
                        instanceId != null ? sql.instanceId.eq(instanceId) : null,
                        filter != null && !filter.isEmpty() ? sql.sqlText.containsIgnoreCase(filter) : null,
                        sql.createdAt.goe(start),
                        sql.createdAt.lt(end)
                )
                .fetch();
    }

    @Override
    public List<Sql> findAllForStats(
            Long instanceId,
            LocalDateTime start,
            LocalDateTime end
    ) {

        QSql sql = QSql.sql;

        return queryFactory
                .selectFrom(sql)
                .where(
                        sql.isDeleted.eq(false),
                        instanceId != null ? sql.instanceId.eq(instanceId) : null,
                        sql.createdAt.goe(start),
                        sql.createdAt.lt(end)
                )
                .fetch();
    }
}
