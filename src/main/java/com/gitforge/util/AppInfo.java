package com.gitforge.util;

import com.gitforge.repository.SettingsRepository;

/**
 * Application branding and version details for the shell chrome.
 */
public final class AppInfo {

    public static final String APP_NAME = "GitForge";
    public static final String APP_TAGLINE = "Desktop Git Visualizer";
    public static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";
    public static final String COPYRIGHT = "Simulated Git operations powered by SQLite";

    private AppInfo() {
    }

    public static String resolveVersion() {
        try {
            return new SettingsRepository().findByKey("app.version")
                    .map(setting -> setting.getValue() == null || setting.getValue().isBlank()
                            ? DEFAULT_VERSION
                            : setting.getValue().trim())
                    .orElse(DEFAULT_VERSION);
        } catch (Exception ex) {
            return DEFAULT_VERSION;
        }
    }

    public static String displayVersion() {
        return APP_NAME + " " + resolveVersion();
    }
}
