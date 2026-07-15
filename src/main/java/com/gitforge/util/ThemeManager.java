package com.gitforge.util;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.Objects;

/**
 * Applies light/dark/system themes and root font-size preferences.
 */
public final class ThemeManager {

    public static final String THEME_DARK = "dark";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_SYSTEM = "system";

    private static String activeTheme = THEME_DARK;
    private static int fontSizePx = 13;

    private ThemeManager() {
    }

    public static String getActiveTheme() {
        return activeTheme;
    }

    public static int getFontSizePx() {
        return fontSizePx;
    }

    public static void apply(Scene scene, String themePreference, int fontSize) {
        if (scene == null) {
            return;
        }
        activeTheme = themePreference == null || themePreference.isBlank() ? THEME_DARK : themePreference.trim().toLowerCase();
        fontSizePx = Math.max(11, Math.min(20, fontSize));
        String stylesheet = resolveStylesheet(activeTheme);
        scene.getStylesheets().setAll(stylesheet);
        if (scene.getRoot() != null) {
            applyFontSize(scene.getRoot(), fontSizePx);
        }
    }

    public static void applyFontSize(Parent root, int fontSize) {
        if (root == null) {
            return;
        }
        fontSizePx = Math.max(11, Math.min(20, fontSize));
        root.setStyle("-fx-font-size: " + fontSizePx + "px;");
    }

    public static String resolveStylesheet(String themePreference) {
        String resolved = resolveEffectiveTheme(themePreference);
        String resource = THEME_LIGHT.equals(resolved) ? "/css/light-theme.css" : "/css/dark-theme.css";
        return Objects.requireNonNull(
                ThemeManager.class.getResource(resource),
                resource + " not found"
        ).toExternalForm();
    }

    public static String resolveEffectiveTheme(String themePreference) {
        String pref = themePreference == null ? THEME_DARK : themePreference.trim().toLowerCase();
        if (THEME_SYSTEM.equals(pref)) {
            try {
                ColorScheme scheme = Platform.getPreferences().getColorScheme();
                return scheme == ColorScheme.LIGHT ? THEME_LIGHT : THEME_DARK;
            } catch (Throwable ignored) {
                return THEME_DARK;
            }
        }
        if (THEME_LIGHT.equals(pref)) {
            return THEME_LIGHT;
        }
        return THEME_DARK;
    }

    public static String currentDialogStylesheet() {
        return resolveStylesheet(activeTheme);
    }
}
