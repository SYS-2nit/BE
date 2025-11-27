/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.instance.repository;

import com.sys.dbmonitor.domains.instance.domain.DBInfo;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstanceRepository extends JpaRepository<Instance, Long>, InstanceRepositoryCustom {

    List<Instance> findByIsDeletedFalse();

    List<Instance> findByDbInfoAndIsDeletedFalse(DBInfo dbInfo);

    List<Instance> findByDbInfoIdAndIsDeletedFalse(Long dbInfoId);

    Optional<Instance> findByIdAndIsDeletedFalse(Long id);

    boolean existsByDbInfoIdAndSid(Long dbInfoId, String sid);

    Optional<Instance> findByIdAndDbInfoIdAndIsDeletedFalse(Long id, Long dbInfoId);

    /**
     * DBInfo ID와 SID로 삭제된 Instance를 포함하여 검색
     */
    Optional<Instance> findByDbInfoIdAndSid(Long dbInfoId, String sid);
}

