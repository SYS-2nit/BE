/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.instance.repository;

import com.sys.dbmonitor.domains.instance.domain.DBInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DBInfoRepository extends JpaRepository<DBInfo, Long>, DBInfoRepositoryCustom {

    Optional<DBInfo> findByNameAndIsDeletedFalse(String name);

    List<DBInfo> findByIsActiveTrueAndIsDeletedFalse();

    List<DBInfo> findByIsDeletedFalse();

    Optional<DBInfo> findByIdAndIsDeletedFalse(Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}

