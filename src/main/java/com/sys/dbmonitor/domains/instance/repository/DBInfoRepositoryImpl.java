/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.instance.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sys.dbmonitor.domains.instance.domain.DBInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.sys.dbmonitor.domains.instance.domain.QDBInfo.dBInfo;
import static com.sys.dbmonitor.domains.instance.domain.QInstance.instance;


@Repository
@RequiredArgsConstructor
public class DBInfoRepositoryImpl implements DBInfoRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<DBInfo> findByIpAndUserNameAndInstanceSid(String ip, String userName, String sid) {
        DBInfo result = queryFactory
                .selectFrom(dBInfo)
                .where(
                        dBInfo.id.in(
                                queryFactory
                                        .select(instance.dbInfo.id)
                                        .from(instance)
                                        .where(instance.sid.eq(sid))
                        ),
                        dBInfo.ip.eq(ip),
                        dBInfo.userName.eq(userName)
                )
                .fetchOne();
        return Optional.ofNullable(result);
    }
}

