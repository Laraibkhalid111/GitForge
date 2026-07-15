package com.gitforge.service;

import com.gitforge.database.DatabaseManager;
import com.gitforge.util.ThemeManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsServiceTest {

    @TempDir
    Path tempDir;

    private DatabaseService databaseService;
    private SettingsService settingsService;

    @BeforeEach
    void setUp() throws Exception {
        Path databaseFile = tempDir.resolve("settings.db");
        databaseService = new DatabaseService(DatabaseManager.getInstance());
        databaseService.initializeDatabase(databaseFile);
        settingsService = new SettingsService();
        settingsService.ensureDefaults();
    }

    @AfterEach
    void tearDown() throws Exception {
        databaseService.shutdown();
    }

    @Test
    void persistsThemeAndNotificationPreferences() throws Exception {
        SettingsService.Preferences preferences = settingsService.loadPreferences();
        preferences.setTheme(ThemeManager.THEME_LIGHT);
        preferences.setFontSize(16);
        preferences.setConfirmBeforeDelete(false);
        preferences.setNotificationsEnabled(true);
        preferences.setNotifySuccess(false);
        preferences.setDefaultBranch("develop");
        settingsService.savePreferences(preferences);

        SettingsService.Preferences reloaded = settingsService.loadPreferences();
        assertEquals(ThemeManager.THEME_LIGHT, reloaded.getTheme());
        assertEquals(16, reloaded.getFontSize());
        assertFalse(reloaded.isConfirmBeforeDelete());
        assertFalse(reloaded.isNotifySuccess());
        assertEquals("develop", reloaded.getDefaultBranch());
        assertFalse(settingsService.isConfirmBeforeDeleteEnabled());
        assertFalse(settingsService.shouldShowSuccessNotifications());
    }

    @Test
    void backsUpDatabaseFile() throws Exception {
        Path backup = tempDir.resolve("backup.db");
        Path saved = settingsService.backupDatabase(backup);
        assertTrue(Files.exists(saved));
        assertTrue(Files.size(saved) > 0);
        SettingsService.DatabaseInfo info = settingsService.loadDatabaseInfo();
        assertEquals("Connected", info.status());
        assertTrue(info.repositoryCount() >= 0);
    }
}
