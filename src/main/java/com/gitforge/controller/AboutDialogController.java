package com.gitforge.controller;

import com.gitforge.util.AppInfo;
import com.gitforge.util.ThemeManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Objects;

/**
 * About dialog for GitForge project information.
 */
public class AboutDialogController {

    @FXML
    private Label versionLabel;
    @FXML
    private Label javaLabel;

    public static void show(Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    AboutDialogController.class.getResource("/fxml/about-dialog.fxml"),
                    "about-dialog.fxml not found"
            ));
            Parent root = loader.load();
            AboutDialogController controller = loader.getController();
            controller.bindDetails();

            Stage stage = new Stage();
            stage.setTitle("About " + AppInfo.APP_NAME);
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) {
                stage.initOwner(owner);
            }
            Scene scene = new Scene(root);
            scene.getStylesheets().add(ThemeManager.currentDialogStylesheet());
            stage.setScene(scene);
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to open About dialog", ex);
        }
    }

    @FXML
    private void onCloseClicked() {
        if (versionLabel.getScene() != null && versionLabel.getScene().getWindow() instanceof Stage stage) {
            stage.close();
        }
    }

    private void bindDetails() {
        versionLabel.setText(AppInfo.displayVersion());
        javaLabel.setText("Java " + System.getProperty("java.version", "Unknown"));
    }
}
