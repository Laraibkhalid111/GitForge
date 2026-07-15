package com.gitforge.service;

import com.gitforge.database.DatabaseManager;
import com.gitforge.model.Repository;
import com.gitforge.model.Settings;
import com.gitforge.repository.RepositoryRepository;
import com.gitforge.repository.SettingsRepository;
import com.gitforge.util.DatabasePaths;
import com.gitforge.util.ThemeManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application preferences persisted in the settings table.
 */
public class SettingsService {

    public static final String KEY_THEME = "theme";
    public static final String KEY_APP_VERSION = "app.version";
    public static final String KEY_DEFAULT_REPOSITORY = "general.default_repository_id";
    public static final String KEY_AUTO_REFRESH = "general.auto_refresh";
    public static final String KEY_CONFIRM_DELETE = "general.confirm_before_delete";
    public static final String KEY_DEFAULT_BRANCH = "general.default_branch";
    public static final String KEY_FONT_SIZE = "appearance.font_size";
    public static final String KEY_NOTIFICATIONS_ENABLED = "notifications.enabled";
    public static final String KEY_NOTIFY_SUCCESS = "notifications.success";
    public static final String KEY_NOTIFY_WARNING = "notifications.warning";
    public static final String KEY_NOTIFY_ERROR = "notifications.error";
    public static final String KEY_AUTHOR = "app.author";

    private final SettingsRepository settingsRepository;
    private final RepositoryRepository repositoryRepository;

    public SettingsService() {
        this(new SettingsRepository(), new RepositoryRepository());
    }

    public SettingsService(SettingsRepository settingsRepository, RepositoryRepository repositoryRepository) {
        this.settingsRepository = settingsRepository;
        this.repositoryRepository = repositoryRepository;
    }

    public void ensureDefaults() throws SQLException {
        putIfAbsent(KEY_THEME, ThemeManager.THEME_DARK);
        putIfAbsent(KEY_APP_VERSION, "1.0.0-SNAPSHOT");
        putIfAbsent(KEY_DEFAULT_REPOSITORY, "");
        putIfAbsent(KEY_AUTO_REFRESH, "false");
        putIfAbsent(KEY_CONFIRM_DELETE, "true");
        putIfAbsent(KEY_DEFAULT_BRANCH, "main");
        putIfAbsent(KEY_FONT_SIZE, "13");
        putIfAbsent(KEY_NOTIFICATIONS_ENABLED, "true");
        putIfAbsent(KEY_NOTIFY_SUCCESS, "true");
        putIfAbsent(KEY_NOTIFY_WARNING, "true");
        putIfAbsent(KEY_NOTIFY_ERROR, "true");
        putIfAbsent(KEY_AUTHOR, "GitForge Team");
    }

    public Preferences loadPreferences() throws SQLException {
        ensureDefaults();
        Preferences prefs = new Preferences();
        prefs.setTheme(getString(KEY_THEME, ThemeManager.THEME_DARK));
        prefs.setDefaultRepositoryId(parseLong(getString(KEY_DEFAULT_REPOSITORY, "")));
        prefs.setAutoRefresh(getBoolean(KEY_AUTO_REFRESH, false));
        prefs.setConfirmBeforeDelete(getBoolean(KEY_CONFIRM_DELETE, true));
        prefs.setDefaultBranch(getString(KEY_DEFAULT_BRANCH, "main"));
        prefs.setFontSize(parseInt(getString(KEY_FONT_SIZE, "13"), 13));
        prefs.setNotificationsEnabled(getBoolean(KEY_NOTIFICATIONS_ENABLED, true));
        prefs.setNotifySuccess(getBoolean(KEY_NOTIFY_SUCCESS, true));
        prefs.setNotifyWarning(getBoolean(KEY_NOTIFY_WARNING, true));
        prefs.setNotifyError(getBoolean(KEY_NOTIFY_ERROR, true));
        prefs.setAuthor(getString(KEY_AUTHOR, "GitForge Team"));
        prefs.setAppVersion(getString(KEY_APP_VERSION, "1.0.0-SNAPSHOT"));
        return prefs;
    }

    public void savePreferences(Preferences preferences) throws SQLException {
        Objects.requireNonNull(preferences, "preferences");
        put(KEY_THEME, normalizeTheme(preferences.getTheme()));
        put(KEY_DEFAULT_REPOSITORY, preferences.getDefaultRepositoryId() == null
                ? ""
                : Long.toString(preferences.getDefaultRepositoryId()));
        put(KEY_AUTO_REFRESH, Boolean.toString(preferences.isAutoRefresh()));
        put(KEY_CONFIRM_DELETE, Boolean.toString(preferences.isConfirmBeforeDelete()));
        put(KEY_DEFAULT_BRANCH, blankToDefault(preferences.getDefaultBranch(), "main"));
        put(KEY_FONT_SIZE, Integer.toString(Math.max(11, Math.min(20, preferences.getFontSize()))));
        put(KEY_NOTIFICATIONS_ENABLED, Boolean.toString(preferences.isNotificationsEnabled()));
        put(KEY_NOTIFY_SUCCESS, Boolean.toString(preferences.isNotifySuccess()));
        put(KEY_NOTIFY_WARNING, Boolean.toString(preferences.isNotifyWarning()));
        put(KEY_NOTIFY_ERROR, Boolean.toString(preferences.isNotifyError()));
        put(KEY_AUTHOR, blankToDefault(preferences.getAuthor(), "GitForge Team"));
    }

    public boolean isConfirmBeforeDeleteEnabled() {
        try {
            return getBoolean(KEY_CONFIRM_DELETE, true);
        } catch (SQLException ex) {
            return true;
        }
    }

    public boolean shouldShowSuccessNotifications() {
        return notificationAllowed(KEY_NOTIFY_SUCCESS);
    }

    public boolean shouldShowWarningNotifications() {
        return notificationAllowed(KEY_NOTIFY_WARNING);
    }

    public boolean shouldShowErrorNotifications() {
        return notificationAllowed(KEY_NOTIFY_ERROR);
    }

    public List<Repository> listRepositories() throws SQLException {
        return repositoryRepository.findAll();
    }

    public DatabaseInfo loadDatabaseInfo() throws SQLException, IOException {
        Path path = DatabaseManager.getInstance().getDatabasePath();
        if (path == null) {
            path = DatabasePaths.defaultDatabaseFile();
        }
        boolean exists = Files.exists(path);
        long size = exists ? Files.size(path) : 0L;
        int repositories = repositoryRepository.countAll();
        return new DatabaseInfo(
                exists ? "Connected" : "Missing",
                path.toAbsolutePath().normalize().toString(),
                repositories,
                size
        );
    }

    public Path backupDatabase(Path destinationFile) throws IOException {
        Path source = DatabaseManager.getInstance().getDatabasePath();
        if (source == null) {
            source = DatabasePaths.defaultDatabaseFile();
        }
        if (!Files.exists(source)) {
            throw new IOException("Database file not found: " + source);
        }
        Files.createDirectories(destinationFile.getParent());
        Files.copy(source, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        return destinationFile;
    }

    public Path restoreDatabase(Path sourceBackup) throws IOException, SQLException {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Path target = databaseManager.getDatabasePath();
        if (target == null) {
            target = DatabasePaths.defaultDatabaseFile();
        }
        if (!Files.exists(sourceBackup)) {
            throw new IOException("Backup file not found: " + sourceBackup);
        }
        Files.createDirectories(target.getParent());

        // Close the live connection before overwriting the file, then reopen.
        databaseManager.shutdown();
        try {
            Files.copy(sourceBackup, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            databaseManager.initializeAt(target);
            throw ex;
        }
        databaseManager.initializeAt(target);
        AnalyticsService.invalidateSharedCache();
        return target;
    }

    public void put(String key, String value) throws SQLException {
        Optional<Settings> existing = settingsRepository.findByKey(key);
        if (existing.isPresent()) {
            Settings settings = existing.get();
            settings.setValue(value);
            settings.setUpdatedAt(Instant.now());
            settingsRepository.update(settings);
        } else {
            settingsRepository.create(new Settings(key, value));
        }
    }

    public String getString(String key, String defaultValue) throws SQLException {
        return settingsRepository.findByKey(key)
                .map(Settings::getValue)
                .filter(value -> value != null && !value.isBlank())
                .orElse(defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) throws SQLException {
        String value = getString(key, Boolean.toString(defaultValue));
        return Boolean.parseBoolean(value);
    }

    private void putIfAbsent(String key, String value) throws SQLException {
        if (settingsRepository.findByKey(key).isEmpty()) {
            settingsRepository.create(new Settings(key, value));
        }
    }

    private boolean notificationAllowed(String key) {
        try {
            if (!getBoolean(KEY_NOTIFICATIONS_ENABLED, true)) {
                return false;
            }
            return getBoolean(key, true);
        } catch (SQLException ex) {
            return true;
        }
    }

    private static String normalizeTheme(String theme) {
        if (theme == null) {
            return ThemeManager.THEME_DARK;
        }
        String value = theme.trim().toLowerCase();
        if (ThemeManager.THEME_LIGHT.equals(value) || ThemeManager.THEME_SYSTEM.equals(value)) {
            return value;
        }
        return ThemeManager.THEME_DARK;
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * Mutable preferences snapshot for the Settings UI.
     */
    public static final class Preferences {
        private String theme = ThemeManager.THEME_DARK;
        private Long defaultRepositoryId;
        private boolean autoRefresh;
        private boolean confirmBeforeDelete = true;
        private String defaultBranch = "main";
        private int fontSize = 13;
        private boolean notificationsEnabled = true;
        private boolean notifySuccess = true;
        private boolean notifyWarning = true;
        private boolean notifyError = true;
        private String author = "GitForge Team";
        private String appVersion = "1.0.0-SNAPSHOT";

        public String getTheme() {
            return theme;
        }

        public void setTheme(String theme) {
            this.theme = theme;
        }

        public Long getDefaultRepositoryId() {
            return defaultRepositoryId;
        }

        public void setDefaultRepositoryId(Long defaultRepositoryId) {
            this.defaultRepositoryId = defaultRepositoryId;
        }

        public boolean isAutoRefresh() {
            return autoRefresh;
        }

        public void setAutoRefresh(boolean autoRefresh) {
            this.autoRefresh = autoRefresh;
        }

        public boolean isConfirmBeforeDelete() {
            return confirmBeforeDelete;
        }

        public void setConfirmBeforeDelete(boolean confirmBeforeDelete) {
            this.confirmBeforeDelete = confirmBeforeDelete;
        }

        public String getDefaultBranch() {
            return defaultBranch;
        }

        public void setDefaultBranch(String defaultBranch) {
            this.defaultBranch = defaultBranch;
        }

        public int getFontSize() {
            return fontSize;
        }

        public void setFontSize(int fontSize) {
            this.fontSize = fontSize;
        }

        public boolean isNotificationsEnabled() {
            return notificationsEnabled;
        }

        public void setNotificationsEnabled(boolean notificationsEnabled) {
            this.notificationsEnabled = notificationsEnabled;
        }

        public boolean isNotifySuccess() {
            return notifySuccess;
        }

        public void setNotifySuccess(boolean notifySuccess) {
            this.notifySuccess = notifySuccess;
        }

        public boolean isNotifyWarning() {
            return notifyWarning;
        }

        public void setNotifyWarning(boolean notifyWarning) {
            this.notifyWarning = notifyWarning;
        }

        public boolean isNotifyError() {
            return notifyError;
        }

        public void setNotifyError(boolean notifyError) {
            this.notifyError = notifyError;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getAppVersion() {
            return appVersion;
        }

        public void setAppVersion(String appVersion) {
            this.appVersion = appVersion;
        }
    }

    public record DatabaseInfo(String status, String location, int repositoryCount, long sizeBytes) {
        public String formattedSize() {
            if (sizeBytes < 1024) {
                return sizeBytes + " B";
            }
            if (sizeBytes < 1024 * 1024) {
                return String.format("%.1f KB", sizeBytes / 1024.0);
            }
            return String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0));
        }
    }
}
