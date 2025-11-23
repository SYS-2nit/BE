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
public interface InstanceRepository extends JpaRepository<Instance, Long> {

    List<Instance> findByIsDeletedFalse();

    List<Instance> findByDbInfoAndIsDeletedFalse(DBInfo dbInfo);

    List<Instance> findByDbInfoIdAndIsDeletedFalse(Long dbInfoId);

    Optional<Instance> findByIdAndIsDeletedFalse(Long id);

    @Query("SELECT i FROM Instance i JOIN FETCH i.dbInfo WHERE i.id = :id AND i.isDeleted = false")
    Optional<Instance> findByIdWithDbInfoAndIsDeletedFalse(Long id);

    boolean existsByDbInfoIdAndSid(Long dbInfoId, String sid);

    Optional<Instance> findByIdAndDbInfoIdAndIsDeletedFalse(Long id, Long dbInfoId);

    @Query("SELECT i FROM Instance i WHERE i.dbInfo.name = :name AND i.isDeleted = false")
    List<Instance> findByDbInfoName(String name);

    /**
     * DBInfo ID와 SID로 삭제된 Instance를 포함하여 검색
     */
    Optional<Instance> findByDbInfoIdAndSid(Long dbInfoId, String sid);


    @Query("SELECT i FROM Instance i WHERE i.dbInfo.id = :dbInfoId AND i.connectionType = 'SID' AND i.isDeleted = false ORDER BY i.id ASC")
    Optional<Instance> findSidInstancesByDbInfoId(@org.springframework.data.repository.query.Param("dbInfoId") Long dbInfoId);
}

