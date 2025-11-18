package com.sys.dbmonitor.domains.sql.dto.response;

import java.util.List;

public record SqlComparePageResponse(
        List<SqlResponse> baseList,
        List<SqlResponse> compareList
) {}
