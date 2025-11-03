package com.sys.dbmonitor.domains.dashboard.dao;

import com.sys.dbmonitor.domains.dashboard.dto.CollectorRawDTO;

public interface CollectorRepository {

     // PL/SQL(RETURN_RESULT) 단일 호출로 9개 결과셋 수집
     CollectorRawDTO collectSnapshot();
}

