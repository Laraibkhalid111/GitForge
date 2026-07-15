package com.gitforge;

import com.gitforge.service.DatabaseService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
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

    @Override
    public void stop() throws Exception {
        databaseService.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
