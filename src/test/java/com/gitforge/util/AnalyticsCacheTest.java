package com.gitforge.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsCacheTest {

    private AnalyticsCache<String, Integer> cache;

    @BeforeEach
    void setUp() {
        cache = new AnalyticsCache<>();
    }

    @Test
    void putGetContainsRemoveAndClear() {
        cache.put("a", 1);
        cache.put("b", 2);
        assertEquals(2, cache.size());
        assertTrue(cache.contains("a"));
        assertEquals(1, cache.get("a").orElseThrow());

        cache.put("a", 9);
        assertEquals(9, cache.get("a").orElseThrow());
        assertTrue(cache.remove("b"));
        assertFalse(cache.contains("b"));

        cache.clear();
        assertTrue(cache.isEmpty());
        assertFalse(cache.get("a").isPresent());
    }
}
