package com.demo.common.util;

import java.util.UUID;

public final class CorrelationIds {
    private CorrelationIds() {
    }

    public static String resolve(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return candidate.trim();
    }
}
