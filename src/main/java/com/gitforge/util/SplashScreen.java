package com.gitforge.util;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;

/**
 * Branded splash stage shown briefly while the main shell prepares.
 */
public final class SplashScreen {

    private SplashScreen() {
    }

    public static Stage show(Stage ownerHint) {
        Stage splash = new Stage(StageStyle.UNDECORATED);
        if (ownerHint != null) {
            splash.initOwner(ownerHint);
        }

        FontIcon icon = new FontIcon("mdi2g-git");
        icon.setIconSize(56);
        icon.getStyleClass().add("splash-icon");

        Label title = new Label(AppInfo.APP_NAME);
        title.getStyleClass().add("splash-title");

        Label subtitle = new Label(AppInfo.APP_TAGLINE);
        subtitle.getStyleClass().add("splash-subtitle");

        Label version = new Label(AppInfo.displayVersion());
        version.getStyleClass().add("splash-version");

        ProgressBar progress = new ProgressBar(-1);
        progress.setPrefWidth(220);
        progress.getStyleClass().add("splash-progress");

        VBox content = new VBox(12, icon, title, subtitle, progress, version);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(36));
        content.getStyleClass().add("splash-content");

        StackPane root = new StackPane(content);
        root.getStyleClass().add("splash-root");

        ImageView badge = loadIconView();
        if (badge != null) {
            root.getChildren().add(0, badge);
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            badge.setTranslateX(-18);
            badge.setTranslateY(18);
            badge.setOpacity(0.85);
        }

        Scene scene = new Scene(root, 420, 280);
        scene.getStylesheets().add(ThemeManager.currentDialogStylesheet());

        splash.setScene(scene);
        splash.setAlwaysOnTop(true);
        splash.centerOnScreen();
        splash.show();

        FadeTransition fade = new FadeTransition(Duration.millis(280), root);
        fade.setFromValue(0.2);
        fade.setToValue(1.0);
        fade.play();
        return splash;
    }

    private static ImageView loadIconView() {
        try {
            Image image = new Image(Objects.requireNonNull(
                    SplashScreen.class.getResourceAsStream("/icons/gitforge-icon.png")
            ), 42, 42, true, true);
            ImageView view = new ImageView(image);
            view.setPreserveRatio(true);
            return view;
        } catch (Exception ex) {
            return null;
        }
    }
}
