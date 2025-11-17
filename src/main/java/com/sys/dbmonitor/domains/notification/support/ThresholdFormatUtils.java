package com.sys.dbmonitor.domains.notification.support;

import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;

/**
 * 임계치 포맷에 따른 값 표기를 담당하는 유틸리티.
 */
public final class ThresholdFormatUtils {

    private ThresholdFormatUtils() {
    }

    /**
     * 포맷에 맞춰 값을 문자열로 변환한다.
     */
    public static String formatValue(Double value, ThresholdFormat format) {
        if (value == null) {
            return "-";
        }
        if (format == null) {
            return String.format("%.2f", value);
        }
        return switch (format) {
            case PERCENT -> String.format("%.2f%%", value);
            case MS -> String.format("%.2f ms", value);
            case MBPS -> String.format("%.2f MB/s", value);
            case COUNT -> isInteger(value)
                ? String.format("%.0f회", value)
                : String.format("%.2f회", value);
        };
    }

    /**
     * 단위 표시만 필요할 때 사용.
     */
    public static String getUnit(ThresholdFormat format) {
        if (format == null) {
            return "";
        }
        return switch (format) {
            case PERCENT -> "%";
            case MS -> "ms";
            case MBPS -> "MB/s";
            case COUNT -> "회";
        };
    }

    private static boolean isInteger(Double value) {
        return Math.abs(value - Math.rint(value)) < 1e-9;
    }
}

