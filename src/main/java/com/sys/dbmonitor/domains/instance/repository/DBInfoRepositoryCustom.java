package com.sys.dbmonitor.domains.instance.repository;

import com.sys.dbmonitor.domains.instance.domain.DBInfo;

import java.util.Optional;

public interface DBInfoRepositoryCustom {

    /**
     * IP, UserName, Instance SID로 DBInfo 검색 (삭제된 것 포함)
     */
    Optional<DBInfo> findByIpAndUserNameAndInstanceSid(String ip, String userName, String sid);
}

