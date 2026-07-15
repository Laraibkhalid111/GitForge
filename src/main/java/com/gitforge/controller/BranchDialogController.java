package com.gitforge.controller;

import com.gitforge.model.Branch;
import com.gitforge.model.BranchSummary;
import com.gitforge.model.Repository;
import com.gitforge.service.BranchService;
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
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Create dialog for simulated branches.
 */
public class BranchDialogController {

    @FXML
    private Label validationLabel;
    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<Repository> repositoryComboBox;
    @FXML
    private ComboBox<Branch> parentBranchComboBox;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Button saveButton;

    private Stage dialogStage;
    private BranchService branchService;
    private BranchSummary result;

    @FXML
    private void initialize() {
        clearValidation();
        nameField.textProperty().addListener((obs, o, n) -> clearValidation());

        repositoryComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Repository repository) {
                return repository == null ? "" : repository.getName();
            }

            @Override
            public Repository fromString(String string) {
                return null;
            }
        });
        parentBranchComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Branch branch) {
                return branch == null ? "" : branch.getName();
            }

            @Override
            public Branch fromString(String string) {
                return null;
            }
        });

        repositoryComboBox.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> loadParentBranches(newValue));
    }

    public Optional<BranchSummary> showAndWait(Window owner,
                                               Parent dialogRoot,
                                               BranchService service,
                                               Repository preferredRepository) {
        this.branchService = service;
        this.result = null;

        try {
            List<Repository> repositories = service.listRepositories();
            repositoryComboBox.getItems().setAll(repositories);
            if (preferredRepository != null) {
                repositories.stream()
                        .filter(repo -> Objects.equals(repo.getId(), preferredRepository.getId()))
                        .findFirst()
                        .ifPresentOrElse(
                                repo -> repositoryComboBox.getSelectionModel().select(repo),
                                () -> selectFirstRepository()
                        );
            } else {
                selectFirstRepository();
            }
        } catch (SQLException ex) {
            showValidation("Unable to load repositories: " + ex.getMessage());
        }

        dialogStage = new Stage();
        dialogStage.setTitle("Create Branch");
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

    private void selectFirstRepository() {
        if (!repositoryComboBox.getItems().isEmpty()) {
            repositoryComboBox.getSelectionModel().selectFirst();
        }
    }

    private void loadParentBranches(Repository repository) {
        parentBranchComboBox.getItems().clear();
        if (repository == null || branchService == null) {
            return;
        }
        try {
            List<Branch> branches = branchService.listBranchesForRepository(repository.getId());
            parentBranchComboBox.getItems().setAll(branches);
            branches.stream()
                    .filter(branch -> BranchService.DEFAULT_BRANCH.equalsIgnoreCase(branch.getName()))
                    .findFirst()
                    .ifPresentOrElse(
                            branch -> parentBranchComboBox.getSelectionModel().select(branch),
                            () -> {
                                if (!branches.isEmpty()) {
                                    parentBranchComboBox.getSelectionModel().selectFirst();
                                }
                            }
                    );
        } catch (SQLException ex) {
            showValidation("Unable to load parent branches: " + ex.getMessage());
        }
    }

    @FXML
    private void onSave() {
        clearValidation();

        Repository repository = repositoryComboBox.getSelectionModel().getSelectedItem();
        Branch parent = parentBranchComboBox.getSelectionModel().getSelectedItem();
        String name = nameField.getText();

        if (repository == null) {
            showValidation("Select a repository.");
            return;
        }
        if (name == null || name.isBlank()) {
            showValidation("Branch name is required.");
            nameField.requestFocus();
            return;
        }

        try {
            result = branchService.createBranch(
                    repository.getId(),
                    name,
                    parent == null ? null : parent.getId(),
                    descriptionArea.getText()
            );
            dialogStage.close();
        } catch (IllegalArgumentException ex) {
            showValidation(ex.getMessage());
        } catch (SQLException ex) {
            showValidation("Unable to create branch: " + ex.getMessage());
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
