/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.domains.dashboard.repository;

import com.sys.dbmonitor.domains.dashboard.domain.MemberWidget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberWidgetRepository extends JpaRepository<MemberWidget, Long> {
    
    /**
     * 멤버 ID별 목록 조회
     */
    @Query("SELECT mw FROM MemberWidget mw " +
           "WHERE mw.memberId = :memberId " +
           "AND mw.isDeleted = false " +
           "ORDER BY mw.position ASC")
    List<MemberWidget> findByMemberIdOrderByPosition(@Param("memberId") Long memberId);

    /**
     * 멤버 ID와 그래프 ID로 위젯 조회
     */
    @Query("SELECT mw FROM MemberWidget mw " +
           "WHERE mw.memberId = :memberId " +
           "AND mw.graphId = :graphId " +
           "AND mw.isDeleted = false")
    Optional<MemberWidget> findByMemberIdAndGraphId(
            @Param("memberId") Long memberId,
            @Param("graphId") Long graphId
    );

    /**
     * 멤버 ID와 위치로 위젯 조회
     */
    @Query("SELECT mw FROM MemberWidget mw " +
           "WHERE mw.memberId = :memberId " +
           "AND mw.position = :position " +
           "AND mw.isDeleted = false")
    Optional<MemberWidget> findByMemberIdAndPosition(
            @Param("memberId") Long memberId,
            @Param("position") Integer position
    );

    /**
     * 멤버의 모든 위젯 삭제 (소프트 삭제)
     */
    @Modifying
    @Query("UPDATE MemberWidget mw SET mw.isDeleted = true " +
           "WHERE mw.memberId = :memberId AND mw.isDeleted = false")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}

