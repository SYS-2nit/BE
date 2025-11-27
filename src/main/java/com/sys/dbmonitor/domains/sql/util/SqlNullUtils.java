package com.sys.dbmonitor.domains.sql.util;

/**************************************************
 작성자 : 오수경
 *************************************************/

/**
 * SQL 도메인에서 사용하는 NULL 처리 유틸리티
 */
public final class SqlNullUtils {

    private SqlNullUtils() {
        // 유틸리티 클래스는 인스턴스화 불가
    }

    /**
     * NULL 값을 0L로 변환 (Oracle NVL 함수와 동일)
     * @param value 변환할 값
     * @return value가 null이면 0L, 아니면 value
     */
    public static Long nvl(Long value) {
        return value != null ? value : 0L;
    }
}

