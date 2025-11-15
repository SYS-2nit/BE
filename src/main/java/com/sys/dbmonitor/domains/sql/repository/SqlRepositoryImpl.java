package com.sys.dbmonitor.domains.sql.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sys.dbmonitor.domains.sql.domain.QSql;
import com.sys.dbmonitor.domains.sql.domain.Sql;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

// QueryDSL 실제 구현체
@RequiredArgsConstructor
public class SqlRepositoryImpl implements SqlRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /** =====  QueryDSL 기반 SQL 통계 필터 조회 ===== */
    @Override
    public Page<Sql> findFilteredSqlStats(
            Long instanceId,
            String keyword,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    ) {
        QSql sql = QSql.sql;

        BooleanExpression condition = sql.isDeleted.eq(false);

        if (instanceId != null) {
            condition = condition.and(sql.instanceId.eq(instanceId));
        }

        if (keyword != null && !keyword.isBlank()) {
            condition = condition.and(sql.sqlText.containsIgnoreCase(keyword));
        }

        if (start != null) {
            condition = condition.and(sql.createdAt.goe(start));
        }

        if (end != null) {
            condition = condition.and(sql.createdAt.loe(end));
        }

        List<Sql> content = queryFactory
                .selectFrom(sql)
                .where(condition)
                .orderBy(sql.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(sql.count())
                .from(sql)
                .where(condition)
                .fetchOne();

        long totalCount = total != null ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }


    /** ===== QueryDSL 기반 통계 그래프 조회 ===== */
    @Override
    public List<Sql> findForGraph(
            Long instanceId,
            String keyword,
            LocalDateTime start,
            LocalDateTime end
    ) {
        QSql sql = QSql.sql;

        BooleanExpression condition = sql.isDeleted.eq(false);

        if (instanceId != null) {
            condition = condition.and(sql.instanceId.eq(instanceId));
        }

        if (keyword != null && !keyword.isBlank()) {
            condition = condition.and(sql.sqlText.containsIgnoreCase(keyword));
        }

        if (start != null) {
            condition = condition.and(sql.createdAt.goe(start));
        }

        if (end != null) {
            condition = condition.and(sql.createdAt.lt(end));   // "< end" 그대로 반영
        }

        return queryFactory
                .selectFrom(sql)
                .where(condition)
                .orderBy(sql.createdAt.asc()) // 그래프는 시간 오름차순이 일반적
                .fetch();
    }
}
