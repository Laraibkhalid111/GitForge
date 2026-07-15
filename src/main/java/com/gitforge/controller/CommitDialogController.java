package com.gitforge.controller;

import com.gitforge.model.Branch;
import com.gitforge.model.CommitSummary;
import com.gitforge.model.Repository;
import com.gitforge.service.CommitService;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Create dialog for simulated commits.
 */
public class CommitDialogController {

    @FXML
    private Label validationLabel;
    @FXML
    private ComboBox<Repository> repositoryComboBox;
    @FXML
    private ComboBox<Branch> branchComboBox;
    @FXML
    private ComboBox<String> commitTypeComboBox;
    @FXML
    private TextField authorField;
    @FXML
    private TextArea messageArea;
    @FXML
    private Button saveButton;

    private Stage dialogStage;
    private CommitService commitService;
    private CommitSummary result;

    @FXML
    private void initialize() {
        commitTypeComboBox.getItems().setAll(CommitService.COMMIT_TYPES);
        commitTypeComboBox.getSelectionModel().selectFirst();
        authorField.setText("GitForge User");
        clearValidation();

        repositoryComboBox.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> loadBranches(newValue));
    }

    public Optional<CommitSummary> showAndWait(Window owner, Parent dialogRoot, CommitService service) {
        this.commitService = service;
        this.result = null;

        try {
            repositoryComboBox.getItems().setAll(service.listRepositories());
            if (!repositoryComboBox.getItems().isEmpty()) {
                repositoryComboBox.getSelectionModel().selectFirst();
            }
        } catch (SQLException ex) {
            showValidation("Unable to load repositories: " + ex.getMessage());
        }

        dialogStage = new Stage();
        dialogStage.setTitle("New Commit");
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

    private void loadBranches(Repository repository) {
        branchComboBox.getItems().clear();
        if (repository == null) {
            return;
        }
        try {
            List<Branch> branches = commitService.listBranchesForRepository(repository.getId());
            branchComboBox.getItems().setAll(branches);
            if (!branches.isEmpty()) {
                branchComboBox.getSelectionModel().selectFirst();
            }
        } catch (SQLException ex) {
            showValidation("Unable to load branches: " + ex.getMessage());
        }
    }

    @FXML
    private void onSave() {
        clearValidation();

        Repository repository = repositoryComboBox.getSelectionModel().getSelectedItem();
        Branch branch = branchComboBox.getSelectionModel().getSelectedItem();
        String message = messageArea.getText();

        if (repository == null) {
            showValidation("Select a repository.");
            return;
        }
        if (branch == null) {
            showValidation("Select a branch.");
            return;
        }
        if (message == null || message.isBlank()) {
            showValidation("Commit message is required.");
            messageArea.requestFocus();
            return;
        }

        try {
            result = commitService.createCommit(
                    repository.getId(),
                    branch.getId(),
                    message,
                    authorField.getText(),
                    commitTypeComboBox.getSelectionModel().getSelectedItem()
            );
            dialogStage.close();
        } catch (IllegalArgumentException ex) {
            showValidation(ex.getMessage());
        } catch (SQLException ex) {
            showValidation("Unable to create commit: " + ex.getMessage());
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
