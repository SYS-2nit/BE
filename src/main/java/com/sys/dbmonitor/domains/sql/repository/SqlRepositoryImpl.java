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

@RequiredArgsConstructor
public class SqlRepositoryImpl implements SqlRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /** QueryDSL 기반 SQL 통계 조회 + 페이징 */
    @Override
    public Page<Sql> findFilteredSqlStats(
            Long instanceId,
            String keyword,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    ) {

        QSql sql = QSql.sql;

        // 동적 where 조건 생성
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

        // 콘텐츠 조회 (페이징)
        List<Sql> content = queryFactory
                .selectFrom(sql)
                .where(condition)
                .orderBy(sql.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // total count 조회
        Long total = queryFactory
                .select(sql.count())
                .from(sql)
                .where(condition)
                .fetchOne();

        // null-safe 처리
        long totalCount = total != null ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }
}
