package com.gitforge.controller;

import com.gitforge.model.RepositorySummary;
import com.gitforge.service.RepositoryService;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Create / edit dialog for simulated repositories.
 */
public class RepositoryDialogController {

    @FXML
    private Label dialogTitleLabel;
    @FXML
    private FontIcon dialogIcon;
    @FXML
    private Label validationLabel;
    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField pathField;
    @FXML
    private Label defaultBranchLabel;
    @FXML
    private TextField defaultBranchField;
    @FXML
    private Button saveButton;

    private Stage dialogStage;
    private RepositoryService repositoryService;
    private RepositorySummary editing;
    private RepositorySummary result;

    @FXML
    private void initialize() {
        clearValidation();
        nameField.textProperty().addListener((obs, o, n) -> clearValidation());
    }

    public Optional<RepositorySummary> showAndWait(Window owner,
                                                   Parent dialogRoot,
                                                   RepositorySummary existing,
                                                   RepositoryService service) {
        this.repositoryService = service;
        this.editing = existing;
        this.result = null;

        configureMode(existing);

        dialogStage = new Stage();
        dialogStage.setTitle(existing == null ? "New Repository" : "Edit Repository");
        dialogStage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            dialogStage.initOwner(owner);
        }

        Scene scene = new Scene(dialogRoot);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        dialogStage.setScene(scene);
        dialogStage.setResizable(false);
        dialogStage.showAndWait();
        return Optional.ofNullable(result);
    }

    private void configureMode(RepositorySummary existing) {
        boolean createMode = existing == null;
        dialogTitleLabel.setText(createMode ? "New Repository" : "Edit Repository");
        saveButton.setText(createMode ? "Create" : "Save");
        defaultBranchLabel.setVisible(createMode);
        defaultBranchLabel.setManaged(createMode);
        defaultBranchField.setVisible(createMode);
        defaultBranchField.setManaged(createMode);

        if (createMode) {
            nameField.clear();
            descriptionArea.clear();
            pathField.clear();
            defaultBranchField.setText(RepositoryService.DEFAULT_BRANCH);
            dialogIcon.setIconLiteral("mdi2p-plus");
        } else {
            nameField.setText(existing.getName());
            descriptionArea.setText(existing.getDescription() == null ? "" : existing.getDescription());
            pathField.setText(existing.getPath() == null ? "" : existing.getPath());
            dialogIcon.setIconLiteral("mdi2p-pencil-outline");
        }
    }

    @FXML
    private void onSave() {
        clearValidation();
        String name = nameField.getText();
        if (name == null || name.isBlank()) {
            showValidation("Repository name is required.");
            nameField.requestFocus();
            return;
        }

        try {
            if (editing == null) {
                result = repositoryService.createRepository(
                        name,
                        descriptionArea.getText(),
                        pathField.getText(),
                        defaultBranchField.getText()
                );
            } else {
                result = repositoryService.updateRepository(
                        editing.getId(),
                        name,
                        descriptionArea.getText(),
                        pathField.getText()
                );
            }
            dialogStage.close();
        } catch (IllegalArgumentException ex) {
            showValidation(ex.getMessage());
        } catch (SQLException ex) {
            showValidation("Unable to save repository: " + ex.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        result = null;
        dialogStage.close();
    }

    private void showValidation(String message) {
        validationLabel.setText(message);
        validationLabel.setVisible(true);
        validationLabel.setManaged(true);
    }

    private void clearValidation() {
        validationLabel.setText("");
        validationLabel.setVisible(false);
        validationLabel.setManaged(false);
    }
}
