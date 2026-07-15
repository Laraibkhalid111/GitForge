package com.gitforge.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Display formatting helpers for the UI layer.
 */
public final class DateDisplays {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    .withZone(ZoneId.systemDefault());

    private DateDisplays() {
    }

    public static String formatDateTime(Instant instant) {
        return instant == null ? "—" : DATE_TIME.format(instant);
    }

    public static String formatDate(Instant instant) {
        return instant == null ? "—" : DATE.format(instant);
    }
}
