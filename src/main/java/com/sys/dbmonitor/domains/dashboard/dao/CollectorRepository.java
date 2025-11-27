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

     /**
      * 바인드 옵션을 전달하는 수집 오버로드.
      * 구현체에서 미지원일 경우 기본 수집으로 폴백된다.
      */
//     default CollectorRawDTO collectSnapshot(ExecOptions opts) {
//          // 현재 구현은 기본 호출로 폴백
//          return collectSnapshot();
//     }
//
//     /** 수집 바인드 옵션 컨테이너 (필요 항목만 설정) */
//     final class ExecOptions {
//          /** 최근 후보 조회 분(min), 예: 1 */
//          public Integer lookbackMin;
//          /** Top-N, 예: 5 */
//          public Integer topN;
//          /** 후보 최대 건수, 예: 200 */
//          public Integer maxCandidates;
//          /** 인스턴스 필터: 'ALL' | '1,2' 등 */
//          public String instFilter;
//
//          public ExecOptions() {}
//
//          public ExecOptions lookbackMin(Integer v) { this.lookbackMin = v; return this; }
//          public ExecOptions topN(Integer v)        { this.topN = v; return this; }
//          public ExecOptions maxCandidates(Integer v){ this.maxCandidates = v; return this; }
//          public ExecOptions instFilter(String v)   { this.instFilter = v; return this; }
//     }
}
