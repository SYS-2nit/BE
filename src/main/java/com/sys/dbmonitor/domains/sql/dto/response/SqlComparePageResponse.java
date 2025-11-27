package com.sys.dbmonitor.domains.sql.dto.response;

import java.util.List;

/**************************************************
 작성자 : 오수경
 *************************************************/

public record SqlComparePageResponse(
        List<SqlResponse> baseList,
        List<SqlResponse> compareList
) {}
