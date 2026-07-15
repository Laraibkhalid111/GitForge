package com.gitforge.util;

import com.gitforge.service.SettingsService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Lightweight toast notifications overlaid on the main shell.
 */
public final class UiNotifications {

    private static final SettingsService SETTINGS = new SettingsService();
    private static StackPane host;

    private UiNotifications() {
    }

    public static void bind(StackPane notificationHost) {
        host = notificationHost;
    }

    public static void success(String message) {
        if (!SETTINGS.shouldShowSuccessNotifications()) {
            return;
        }
        show(message, "mdi2c-check-circle-outline", "toast-success");
    }

    public static void warning(String message) {
        if (!SETTINGS.shouldShowWarningNotifications()) {
            return;
        }
        show(message, "mdi2a-alert-outline", "toast-info");
    }

    public static void info(String message) {
        if (!SETTINGS.shouldShowWarningNotifications() && !SETTINGS.shouldShowSuccessNotifications()) {
            return;
        }
        show(message, "mdi2i-information-outline", "toast-info");
    }

    private static void show(String message, String iconLiteral, String styleClass) {
        if (host == null || message == null || message.isBlank()) {
            return;
        }
        host.getChildren().removeIf(node -> node.getStyleClass().contains("toast-banner"));

        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(16);
        icon.getStyleClass().add("toast-icon");

        Label label = new Label(message);
        label.getStyleClass().add("toast-message");
        label.setWrapText(true);
        label.setMaxWidth(420);

        HBox banner = new HBox(10, icon, label);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.getStyleClass().addAll("toast-banner", styleClass);
        StackPane.setAlignment(banner, Pos.TOP_RIGHT);
        banner.setTranslateY(18);
        banner.setTranslateX(-18);
        banner.setOpacity(0);

        host.getChildren().add(banner);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), banner);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        PauseTransition pause = new PauseTransition(Duration.millis(2400));
        pause.setOnFinished(event -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(220), banner);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> host.getChildren().remove(banner));
            fadeOut.play();
        });
        pause.play();
    }
}
