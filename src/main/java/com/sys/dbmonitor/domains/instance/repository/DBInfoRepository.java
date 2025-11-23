package com.sys.dbmonitor.domains.instance.repository;

import com.sys.dbmonitor.domains.instance.domain.DBInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DBInfoRepository extends JpaRepository<DBInfo, Long> {

    Optional<DBInfo> findByNameAndIsDeletedFalse(String name);

    List<DBInfo> findByIsActiveTrueAndIsDeletedFalse();

    List<DBInfo> findByIsDeletedFalse();

    Optional<DBInfo> findByIdAndIsDeletedFalse(Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * db_ip, db_username, instance.sid를 모두 비교하여 DBInfo 검색 (삭제된 것 포함)
     * 검색 결과가 가장 좁게 나오도록 모든 조건을 만족하는 경우만 반환
     * 삭제된 DBInfo도 검색하여 재활성화 가능하도록 함
     */
    @Query("SELECT d FROM DBInfo d WHERE d.id IN " +
           "(SELECT i.dbInfo.id FROM Instance i WHERE i.sid = :sid) " +
           "AND d.ip = :ip AND d.userName = :userName")
    Optional<DBInfo> findByIpAndUserNameAndInstanceSid(
            @Param("ip") String ip,
            @Param("userName") String userName,
            @Param("sid") String sid
    );
}

