package com.sys.dbmonitor.domains.dashboard.dao;

import com.sys.dbmonitor.domains.dashboard.dto.response.CollectorRaw;

public interface CollectorRepository {

     // PL/SQL(RETURN_RESULT) 단일 호출로 9개 결과셋 수집
    CollectorRaw collectSnapshot();
}

