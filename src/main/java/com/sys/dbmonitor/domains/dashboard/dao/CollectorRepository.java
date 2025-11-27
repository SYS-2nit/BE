/**************************************************
 작성자 : 최온유
 *************************************************/
package com.sys.dbmonitor.domains.dashboard.dao;

import com.sys.dbmonitor.domains.dashboard.dto.CollectorRawDTO;

/**
 * Agentless Oracle Collector 리포지토리.
 *
 * - PL/SQL(RETURN_RESULT) 단일 호출로 총 8개 결과셋을 수집한다.
 *   #1 : GRAPH_BUNDLE (INST_ID, METRIC_NAME, VALUE_NUM)
 *   #2~#8 : 표형 데이터셋 (고정 순서 매핑)
 *           - top_sql_cpu_candidates
 *           - top_blocker_sessions
 *           - top_sql_shared_pool_candidates
 *           - tablespace_capacity_all
 *           - bgprocess_status
 *           - datafile_io_candidates
 *           - segment_top_candidates
 *
 * - 바인드 파라미터는 PL/SQL 내부 NVL 기본값으로 동작하며,
 *   필요 시 {ExecOptions} 오버로드로 전달할 수 있다(구현체가 선택적으로 지원).
 */
public interface CollectorRepository {

     /** 기본 수집(PL/SQL의 NVL 기본값 사용)
      *  @param instanceId 대상 Instance ID
      */
     CollectorRawDTO collectSnapshot(Long instanceId);
}
