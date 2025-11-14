package com.sys.dbmonitor.domains.notification.support;

import com.sys.dbmonitor.domains.notification.domain.ThresholdFormat;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThresholdFormatUtilsTest {

    @Test
    void formatPercent() {
        String formatted = ThresholdFormatUtils.formatValue(85.456, ThresholdFormat.PERCENT);
        assertThat(formatted).isEqualTo("85.46%");
        assertThat(ThresholdFormatUtils.getUnit(ThresholdFormat.PERCENT)).isEqualTo("%");
    }

    @Test
    void formatMilliseconds() {
        String formatted = ThresholdFormatUtils.formatValue(35.0, ThresholdFormat.MS);
        assertThat(formatted).isEqualTo("35.00 ms");
        assertThat(ThresholdFormatUtils.getUnit(ThresholdFormat.MS)).isEqualTo("ms");
    }

    @Test
    void formatThroughput() {
        String formatted = ThresholdFormatUtils.formatValue(120.125, ThresholdFormat.MBPS);
        assertThat(formatted).isEqualTo("120.13 MB/s");
        assertThat(ThresholdFormatUtils.getUnit(ThresholdFormat.MBPS)).isEqualTo("MB/s");
    }

    @Test
    void formatCountInteger() {
        String formatted = ThresholdFormatUtils.formatValue(4.0, ThresholdFormat.COUNT);
        assertThat(formatted).isEqualTo("4회");
    }

    @Test
    void formatCountDecimal() {
        String formatted = ThresholdFormatUtils.formatValue(2.25, ThresholdFormat.COUNT);
        assertThat(formatted).isEqualTo("2.25회");
        assertThat(ThresholdFormatUtils.getUnit(ThresholdFormat.COUNT)).isEqualTo("회");
    }

    @Test
    void nullFormatFallsBack() {
        String formatted = ThresholdFormatUtils.formatValue(12.3, null);
        assertThat(formatted).isEqualTo("12.30");
        assertThat(ThresholdFormatUtils.getUnit(null)).isEmpty();
    }
}

