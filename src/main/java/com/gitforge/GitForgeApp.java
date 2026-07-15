package com.gitforge;

import com.gitforge.service.DatabaseService;
import com.gitforge.util.AppInfo;
import com.gitforge.util.SplashScreen;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * GitForge application entry point.
 * Initializes the SQLite data layer, then loads the desktop shell.
 */
public class GitForgeApp extends Application {

    private static final double DEFAULT_WIDTH = 1280;
    private static final double DEFAULT_HEIGHT = 800;
    private static final double MIN_WIDTH = 1024;
    private static final double MIN_HEIGHT = 640;

    private final DatabaseService databaseService = new DatabaseService();

    @Override
    public void init() throws Exception {
        databaseService.initializeDatabase();
    }

    @Override
    public void start(Stage stage) {
        Stage splash = SplashScreen.show(null);
        PauseTransition delay = new PauseTransition(Duration.millis(1100));
        delay.setOnFinished(event -> {
            try {
                openMainWindow(stage);
                fadeOutSplash(splash);
            } catch (IOException ex) {
                splash.close();
                throw new IllegalStateException("Unable to launch GitForge UI", ex);
            }
        });
        delay.play();
    }

    private void openMainWindow(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(
                        getClass().getResource("/fxml/main-view.fxml"),
                        "main-view.fxml not found on classpath"
                )
        );

        Parent root = loader.load();
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/css/dark-theme.css"),
                        "dark-theme.css not found on classpath"
                ).toExternalForm()
        );

        stage.setTitle(AppInfo.APP_NAME + " — " + AppInfo.APP_TAGLINE);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        loadApplicationIcons(stage);
        stage.setScene(scene);
        stage.show();
        stage.toFront();
    }

    private void fadeOutSplash(Stage splash) {
        if (splash == null) {
            return;
        }
        PauseTransition closeDelay = new PauseTransition(Duration.millis(180));
        closeDelay.setOnFinished(event -> Platform.runLater(splash::close));
        closeDelay.play();
    }

    private void loadApplicationIcons(Stage stage) {
        try (InputStream stream = getClass().getResourceAsStream("/icons/gitforge-icon.png")) {
            if (stream != null) {
                stage.getIcons().add(new Image(stream));
            }
        } catch (IOException ignored) {
            // Optional branding asset — shell still launches without an icon.
        }
    }

    @Override
    public void stop() throws Exception {
        databaseService.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
