package com.gitforge;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * GitForge application entry point.
 * Loads the main window shell only — no repository or database logic yet.
 */
public class GitForgeApp extends Application {

    private static final double DEFAULT_WIDTH = 1280;
    private static final double DEFAULT_HEIGHT = 800;
    private static final double MIN_WIDTH = 960;
    private static final double MIN_HEIGHT = 600;

    @Override
    public void start(Stage stage) throws IOException {
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

        stage.setTitle("GitForge");
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
