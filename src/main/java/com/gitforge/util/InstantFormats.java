package com.gitforge.util;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Helpers for persisting timestamps as ISO-8601 text in SQLite.
 */
public final class InstantFormats {

    private InstantFormats() {
    }

    public static String now() {
        return Instant.now().toString();
    }

    public static Instant parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid instant value: " + value, ex);
        }
    }

    public static String format(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
