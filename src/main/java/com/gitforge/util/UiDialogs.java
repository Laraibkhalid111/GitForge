package com.gitforge.util;

import com.gitforge.service.SettingsService;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Shared confirmation and error dialogs with theme-aware styling.
 */
public final class UiDialogs {

    private static final SettingsService SETTINGS = new SettingsService();

    private UiDialogs() {
    }

    /**
     * Always shows a confirmation dialog (merge, restore, and other non-delete actions).
     */
    public static boolean confirm(Window owner, String title, String header, String content) {
        Alert alert = create(Alert.AlertType.CONFIRMATION, owner, title, header, content);
        ButtonType accept = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(accept, cancel);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == accept;
    }

    /**
     * Confirmation for destructive deletes. Honors the "Confirm before delete" preference.
     */
    public static boolean confirmDelete(Window owner, String title, String header, String content) {
        if (!SETTINGS.isConfirmBeforeDeleteEnabled()) {
            return true;
        }
        return confirm(owner, title, header, content);
    }

    public static void error(Window owner, String header, String detail) {
        Alert alert = create(
                Alert.AlertType.ERROR,
                owner,
                AppInfo.APP_NAME,
                header,
                detail == null || detail.isBlank() ? "An unexpected error occurred." : detail
        );
        alert.showAndWait();
    }

    public static void info(Window owner, String title, String header, String content) {
        Alert alert = create(Alert.AlertType.INFORMATION, owner, title, header, content);
        alert.showAndWait();
    }

    private static Alert create(Alert.AlertType type, Window owner, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        if (owner != null) {
            alert.initOwner(owner);
        }
        style(alert);
        return alert;
    }

    private static void style(Alert alert) {
        DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().add("gitforge-dialog");
        String stylesheet = ThemeManager.currentDialogStylesheet();
        pane.getStylesheets().setAll(stylesheet);
    }
}
