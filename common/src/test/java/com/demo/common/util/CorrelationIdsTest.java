package com.demo.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdsTest {
    @Test
    void resolvesExistingValueOrGeneratesOne() {
        assertThat(CorrelationIds.resolve(" corr-123 ")).isEqualTo("corr-123");
        assertThat(CorrelationIds.resolve(null)).isNotBlank();
    }
}
