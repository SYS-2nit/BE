package com.sys.dbmonitor.domains.sql.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SQL 도메인에서 사용하는 날짜/시간 파싱 유틸리티
 */
public final class SqlDateUtils {

    private SqlDateUtils() {
        // 유틸리티 클래스는 인스턴스화 불가
    }

    /**
     * 시작일을 LocalDateTime으로 변환
     * @param date 시작일 (null이면 어제 자정)
     * @return 시작일의 자정 시간
     */
    public static LocalDateTime parseStartDate(LocalDate date) {
        return date != null 
                ? date.atStartOfDay() 
                : LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * 종료일을 LocalDateTime으로 변환
     * @param date 종료일 (null이면 오늘 자정)
     * @return 종료일 다음날 자정 시간 (exclusive end)
     */
    public static LocalDateTime parseEndDate(LocalDate date) {
        return date != null 
                ? date.plusDays(1).atStartOfDay() 
                : LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * 날짜 문자열을 파싱하여 시작일 LocalDateTime으로 변환
     * @param dateStr 날짜 문자열 (yyyy-MM-dd 형식)
     * @return 시작일의 자정 시간
     */
    public static LocalDateTime parseStartDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        return LocalDate.parse(dateStr).atStartOfDay();
    }

    /**
     * 날짜 문자열을 파싱하여 종료일 LocalDateTime으로 변환
     * @param dateStr 날짜 문자열 (yyyy-MM-dd 형식)
     * @return 종료일 다음날 자정 시간 (exclusive end)
     */
    public static LocalDateTime parseEndDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        return LocalDate.parse(dateStr).plusDays(1).atStartOfDay();
    }

    /**
     * 기본 인터벌 값 반환 (null이거나 0 이하면 기본값 30)
     * @param intervalMinutes 인터벌(분)
     * @return 유효한 인터벌 값
     */
    public static int getDefaultInterval(Integer intervalMinutes) {
        return (intervalMinutes == null || intervalMinutes <= 0) ? 30 : intervalMinutes;
    }
}

