package com.sys.dbmonitor.domains.instance.repository;

import com.sys.dbmonitor.domains.instance.domain.DBInfo;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstanceRepository extends JpaRepository<Instance, Long> {

    List<Instance> findByIsDeletedFalse();

    List<Instance> findByDbInfoAndIsDeletedFalse(DBInfo dbInfo);

    List<Instance> findByDbInfoIdAndIsDeletedFalse(Long dbInfoId);

    Optional<Instance> findByIdAndIsDeletedFalse(Long id);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Instance i JOIN FETCH i.dbInfo WHERE i.id = :id AND i.isDeleted = false")
    Optional<Instance> findByIdWithDbInfoAndIsDeletedFalse(Long id);

    boolean existsByDbInfoIdAndSid(Long dbInfoId, String sid);

    Optional<Instance> findByIdAndDbInfoIdAndIsDeletedFalse(Long id, Long dbInfoId);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Instance i WHERE i.dbInfo.name = :name AND i.isDeleted = false")
    List<Instance> findByDbInfoName(String name);
}

