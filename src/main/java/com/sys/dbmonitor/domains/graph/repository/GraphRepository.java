/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.graph.repository;

import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.domains.graph.domain.GraphCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GraphRepository extends JpaRepository<Graph, Long> {

    Optional<Graph> findByName(String name);

    List<Graph> findByCategory(GraphCategory category);
    
    List<Graph> findByCategoryIn(List<GraphCategory> categories);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Graph> findByIdBetween(Long startId, Long endId);
}

