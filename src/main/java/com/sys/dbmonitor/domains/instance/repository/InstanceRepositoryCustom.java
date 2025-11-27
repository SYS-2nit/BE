/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.instance.repository;

import com.sys.dbmonitor.domains.instance.domain.Instance;

import java.util.List;
import java.util.Optional;

public interface InstanceRepositoryCustom {

    /**
     * ID로 조회 (DBInfo와 함께, 삭제되지 않은 것만)
     */
    Optional<Instance> findByIdWithDbInfoAndIsDeletedFalse(Long id);

    /**
     * DBInfo 이름으로 조회 (삭제되지 않은 것만)
     */
    List<Instance> findByDbInfoName(String name);

    /**
     * DBInfo ID로 SID 인스턴스 조회 (삭제되지 않은 것만)
     */
    Optional<Instance> findSidInstancesByDbInfoId(Long dbInfoId);
}

