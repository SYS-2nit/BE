/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.repository;

import com.sys.dbmonitor.domains.notification.domain.AlertState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlertStateRepository extends JpaRepository<AlertState, Long> {
    Optional<AlertState> findByInstanceIdAndAlertEventIdAndIsDeletedFalse(Long instanceId, Long alertEventId);
}


