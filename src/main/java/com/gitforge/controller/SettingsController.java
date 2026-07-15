package com.gitforge.controller;

import com.gitforge.model.Repository;
import com.gitforge.service.SettingsService;
import com.gitforge.service.SettingsService.DatabaseInfo;
import com.gitforge.service.SettingsService.Preferences;
import com.gitforge.util.AppInfo;
import com.gitforge.util.ThemeManager;
import com.gitforge.util.UiDialogs;
import com.gitforge.util.UiNotifications;
import io.github.palexdev.materialfx.controls.MFXToggleButton;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Settings &amp; Preferences page controller.
 */
public class SettingsController {

    @FXML
    private TabPane settingsTabs;
    @FXML
    private ComboBox<Repository> defaultRepositoryCombo;
    @FXML
    private TextField defaultBranchField;
    @FXML
    private MFXToggleButton autoRefreshToggle;
    @FXML
    private MFXToggleButton confirmDeleteToggle;
    @FXML
    private ToggleGroup themeToggleGroup;
    @FXML
    private RadioButton themeDarkRadio;
    @FXML
    private RadioButton themeLightRadio;
    @FXML
    private RadioButton themeSystemRadio;
    @FXML
    private Slider fontSizeSlider;
    @FXML
    private Label fontSizeValueLabel;
    @FXML
    private MFXToggleButton notificationsEnabledToggle;
    @FXML
    private MFXToggleButton notifySuccessToggle;
    @FXML
    private MFXToggleButton notifyWarningToggle;
    @FXML
    private MFXToggleButton notifyErrorToggle;
    @FXML
    private Label dbStatusLabel;
    @FXML
    private Label dbLocationLabel;
    @FXML
    private Label dbRepoCountLabel;
    @FXML
    private Label dbSizeLabel;
    @FXML
    private Label appNameLabel;
    @FXML
    private Label appVersionLabel;
    @FXML
    private Label appAuthorLabel;
    @FXML
    private Label javaVersionLabel;
    @FXML
    private Label sqliteVersionLabel;

    private final SettingsService settingsService = new SettingsService();
    private boolean suppressEvents;
    private Consumer<String> statusReporter = message -> {
    };
    private Consumer<Preferences> themeApplier = prefs -> {
    };
    private Runnable aboutOpener = () -> {
    };

    @FXML
    private void initialize() {
        defaultRepositoryCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Repository repository) {
                return repository == null ? "None" : repository.getName();
            }

            @Override
            public Repository fromString(String string) {
                return null;
            }
        });

        wireImmediateListeners();
        populateApplicationInfo();
        refreshAll();
        fadeIn(settingsTabs);
    }

    public void setStatusReporter(Consumer<String> statusReporter) {
        this.statusReporter = statusReporter == null ? message -> {
        } : statusReporter;
    }

    public void setThemeApplier(Consumer<Preferences> themeApplier) {
        this.themeApplier = themeApplier == null ? prefs -> {
        } : themeApplier;
    }

    public void setAboutOpener(Runnable aboutOpener) {
        this.aboutOpener = aboutOpener == null ? () -> {
        } : aboutOpener;
    }

    public void onPageShown() {
        refreshAll();
    }

    @FXML
    private void onRefreshDatabaseInfo() {
        refreshDatabaseInfo();
        report("Database information refreshed");
    }

    @FXML
    private void onBackupDatabase() {
        Window owner = window();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Backup GitForge Database");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database", "*.db"));
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        chooser.setInitialFileName("gitforge-backup-" + stamp + ".db");
        var file = chooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }
        try {
            Path saved = settingsService.backupDatabase(file.toPath());
            UiNotifications.success("Database backed up");
            report("Backup saved to " + saved.getFileName());
            refreshDatabaseInfo();
        } catch (Exception ex) {
            UiDialogs.error(owner, "Backup failed", ex.getMessage());
        }
    }

    @FXML
    private void onRestoreDatabase() {
        Window owner = window();
        if (!UiDialogs.confirm(
                owner,
                "Restore Database",
                "Restore from a backup file?",
                "This replaces the current SQLite database file. Restart GitForge after restore."
        )) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Restore GitForge Database");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database", "*.db"));
        var file = chooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }
        try {
            Path restored = settingsService.restoreDatabase(file.toPath());
            UiNotifications.success("Database restored");
            report("Restored database at " + restored.getFileName() + " — data reloaded");
            refreshDatabaseInfo();
        } catch (Exception ex) {
            UiDialogs.error(owner, "Restore failed", ex.getMessage());
        }
    }

    @FXML
    private void onOpenAbout() {
        aboutOpener.run();
    }

    @FXML
    private void onSaveClicked() {
        persistCurrentPreferences(true);
    }

    private void wireImmediateListeners() {
        defaultRepositoryCombo.valueProperty().addListener((obs, o, n) -> persistIfReady());
        defaultBranchField.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused) {
                persistIfReady();
            }
        });
        autoRefreshToggle.selectedProperty().addListener((obs, o, n) -> persistIfReady());
        confirmDeleteToggle.selectedProperty().addListener((obs, o, n) -> persistIfReady());

        themeDarkRadio.setUserData(ThemeManager.THEME_DARK);
        themeLightRadio.setUserData(ThemeManager.THEME_LIGHT);
        themeSystemRadio.setUserData(ThemeManager.THEME_SYSTEM);
        themeToggleGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (!suppressEvents && n != null) {
                persistIfReady();
                applyThemeNow();
            }
        });

        fontSizeSlider.valueProperty().addListener((obs, o, n) -> {
            int size = n.intValue();
            fontSizeValueLabel.setText(size + " px");
            if (!suppressEvents) {
                persistIfReady();
                applyThemeNow();
            }
        });

        notificationsEnabledToggle.selectedProperty().addListener((obs, o, n) -> {
            boolean enabled = Boolean.TRUE.equals(n);
            notifySuccessToggle.setDisable(!enabled);
            notifyWarningToggle.setDisable(!enabled);
            notifyErrorToggle.setDisable(!enabled);
            persistIfReady();
        });
        notifySuccessToggle.selectedProperty().addListener((obs, o, n) -> persistIfReady());
        notifyWarningToggle.selectedProperty().addListener((obs, o, n) -> persistIfReady());
        notifyErrorToggle.selectedProperty().addListener((obs, o, n) -> persistIfReady());
    }

    private void refreshAll() {
        try {
            suppressEvents = true;
            Preferences preferences = settingsService.loadPreferences();
            List<Repository> repositories = settingsService.listRepositories();
            defaultRepositoryCombo.getItems().setAll(repositories);
            defaultRepositoryCombo.getItems().add(0, null);
            if (preferences.getDefaultRepositoryId() != null) {
                repositories.stream()
                        .filter(repo -> Objects.equals(repo.getId(), preferences.getDefaultRepositoryId()))
                        .findFirst()
                        .ifPresentOrElse(
                                repo -> defaultRepositoryCombo.getSelectionModel().select(repo),
                                () -> defaultRepositoryCombo.getSelectionModel().selectFirst()
                        );
            } else {
                defaultRepositoryCombo.getSelectionModel().selectFirst();
            }

            defaultBranchField.setText(preferences.getDefaultBranch());
            autoRefreshToggle.setSelected(preferences.isAutoRefresh());
            confirmDeleteToggle.setSelected(preferences.isConfirmBeforeDelete());

            switch (preferences.getTheme()) {
                case ThemeManager.THEME_LIGHT -> themeLightRadio.setSelected(true);
                case ThemeManager.THEME_SYSTEM -> themeSystemRadio.setSelected(true);
                default -> themeDarkRadio.setSelected(true);
            }

            fontSizeSlider.setValue(preferences.getFontSize());
            fontSizeValueLabel.setText(preferences.getFontSize() + " px");

            notificationsEnabledToggle.setSelected(preferences.isNotificationsEnabled());
            notifySuccessToggle.setSelected(preferences.isNotifySuccess());
            notifyWarningToggle.setSelected(preferences.isNotifyWarning());
            notifyErrorToggle.setSelected(preferences.isNotifyError());
            notifySuccessToggle.setDisable(!preferences.isNotificationsEnabled());
            notifyWarningToggle.setDisable(!preferences.isNotificationsEnabled());
            notifyErrorToggle.setDisable(!preferences.isNotificationsEnabled());

            appVersionLabel.setText(preferences.getAppVersion());
            appAuthorLabel.setText(preferences.getAuthor());
            refreshDatabaseInfo();
        } catch (SQLException ex) {
            UiDialogs.error(window(), "Unable to load settings", ex.getMessage());
        } finally {
            suppressEvents = false;
        }
    }

    private void refreshDatabaseInfo() {
        try {
            DatabaseInfo info = settingsService.loadDatabaseInfo();
            dbStatusLabel.setText(info.status());
            dbLocationLabel.setText(info.location());
            dbRepoCountLabel.setText(Integer.toString(info.repositoryCount()));
            dbSizeLabel.setText(info.formattedSize());
        } catch (Exception ex) {
            dbStatusLabel.setText("Unavailable");
            dbLocationLabel.setText("—");
            dbRepoCountLabel.setText("—");
            dbSizeLabel.setText("—");
            report("Unable to read database info: " + ex.getMessage());
        }
    }

    private void populateApplicationInfo() {
        appNameLabel.setText(AppInfo.APP_NAME);
        javaVersionLabel.setText(System.getProperty("java.version", "Unknown"));
        sqliteVersionLabel.setText("sqlite-jdbc 3.49.1.0");
    }

    private void persistIfReady() {
        if (!suppressEvents) {
            persistCurrentPreferences(false);
        }
    }

    private void persistCurrentPreferences(boolean announce) {
        try {
            Preferences preferences = currentPreferences();
            settingsService.savePreferences(preferences);
            themeApplier.accept(preferences);
            if (announce) {
                UiNotifications.success("Preferences saved");
                report("Preferences saved");
            }
        } catch (SQLException ex) {
            UiDialogs.error(window(), "Unable to save settings", ex.getMessage());
        }
    }

    private Preferences currentPreferences() {
        Preferences preferences = new Preferences();
        Repository repository = defaultRepositoryCombo.getSelectionModel().getSelectedItem();
        preferences.setDefaultRepositoryId(repository == null ? null : repository.getId());
        preferences.setDefaultBranch(defaultBranchField.getText());
        preferences.setAutoRefresh(autoRefreshToggle.isSelected());
        preferences.setConfirmBeforeDelete(confirmDeleteToggle.isSelected());
        Object theme = themeToggleGroup.getSelectedToggle() == null
                ? ThemeManager.THEME_DARK
                : themeToggleGroup.getSelectedToggle().getUserData();
        preferences.setTheme(theme == null ? ThemeManager.THEME_DARK : theme.toString());
        preferences.setFontSize((int) Math.round(fontSizeSlider.getValue()));
        preferences.setNotificationsEnabled(notificationsEnabledToggle.isSelected());
        preferences.setNotifySuccess(notifySuccessToggle.isSelected());
        preferences.setNotifyWarning(notifyWarningToggle.isSelected());
        preferences.setNotifyError(notifyErrorToggle.isSelected());
        preferences.setAuthor(appAuthorLabel.getText());
        preferences.setAppVersion(appVersionLabel.getText());
        return preferences;
    }

    private void applyThemeNow() {
        themeApplier.accept(currentPreferences());
    }

    private Window window() {
        return settingsTabs.getScene() == null ? null : settingsTabs.getScene().getWindow();
    }

    private void report(String message) {
        statusReporter.accept(message);
    }

    private static void fadeIn(TabPane node) {
        if (node == null) {
            return;
        }
        node.setOpacity(0.4);
        FadeTransition fade = new FadeTransition(Duration.millis(180), node);
        fade.setFromValue(0.4);
        fade.setToValue(1.0);
        fade.play();
    }
}
