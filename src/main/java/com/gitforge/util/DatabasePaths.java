package com.gitforge.util;

import java.nio.file.Path;

/**
 * Resolves filesystem paths used by the data layer.
 */
public final class DatabasePaths {

    private static final String APP_DIR = ".gitforge";
    private static final String DB_FILE = "gitforge.db";

    private DatabasePaths() {
    }

    public static Path defaultDatabaseFile() {
        return Path.of(System.getProperty("user.home"), APP_DIR, DB_FILE);
    }

    public static Path defaultDatabaseDirectory() {
        return defaultDatabaseFile().getParent();
    }
}
