package com.sys.dbmonitor.domains.instance.repository;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstanceRepository extends JpaRepository<Instance, Long> {

    /**
     * 이름으로 타겟 DB 조회
     */
    Optional<Instance> findByName(String name);


    /**
     * 활성화된 타겟 DB 목록 조회
     */
    List<Instance> findByIsActiveTrue();

    /**
     * 활성화 여부에 따른 타겟 DB 목록 조회
     */
    Optional<List<Instance>> findByIsActive(Boolean isActive);

    /**
     * 이름 존재 여부 확인
     */
    boolean existsByName(String name);

    /**
     * 다른 ID를 제외하고 이름 존재 여부 확인 (수정 시 사용)
     */
    boolean existsByNameAndIdNot(String name, Long id);
}

