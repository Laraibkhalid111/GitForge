package com.gitforge.controller;

import com.gitforge.model.BranchSummary;
import com.gitforge.model.Repository;
import com.gitforge.service.BranchService;
import com.gitforge.util.DateDisplays;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Controller for the Branch Management page.
 * Hierarchy is loaded from SQLite into a custom tree and shown via {@link BranchService#displayBranchHierarchy()}.
 */
public class BranchController {

    @FXML
    private ComboBox<Repository> repositoryComboBox;
    @FXML
    private TextField searchField;
    @FXML
    private Button renameButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button switchButton;
    @FXML
    private Button mergeButton;
    @FXML
    private TableView<BranchSummary> branchTable;
    @FXML
    private TableColumn<BranchSummary, String> nameColumn;
    @FXML
    private TableColumn<BranchSummary, String> repositoryColumn;
    @FXML
    private TableColumn<BranchSummary, String> parentColumn;
    @FXML
    private TableColumn<BranchSummary, String> createdColumn;
    @FXML
    private TableColumn<BranchSummary, String> latestCommitColumn;
    @FXML
    private TableColumn<BranchSummary, String> statusColumn;
    @FXML
    private TableColumn<BranchSummary, Number> commitsColumn;
    @FXML
    private Label emptyLabel;
    @FXML
    private TextArea hierarchyArea;

    @FXML
    private Label detailsPlaceholderLabel;
    @FXML
    private GridPane detailsGrid;
    @FXML
    private Label detailNameLabel;
    @FXML
    private Label detailRepositoryLabel;
    @FXML
    private Label detailParentLabel;
    @FXML
    private Label detailCreatedLabel;
    @FXML
    private Label detailLatestCommitLabel;
    @FXML
    private Label detailCommitCountLabel;
    @FXML
    private Label detailStatusLabel;
    @FXML
    private Label detailDescriptionLabel;

    private final BranchService branchService = new BranchService();
    private final ObservableList<BranchSummary> tableItems = FXCollections.observableArrayList();

    private Consumer<String> statusReporter = message -> {
    };

    @FXML
    private void initialize() {
        configureTable();
        repositoryComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Repository repository) {
                return repository == null ? "" : repository.getName();
            }

            @Override
            public Repository fromString(String string) {
                return null;
            }
        });
        repositoryComboBox.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> refreshTable());
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshTable());
        clearDetails();
        loadRepositories();
        refreshTable();
    }

    public void setStatusReporter(Consumer<String> statusReporter) {
        this.statusReporter = statusReporter == null ? message -> {
        } : statusReporter;
    }

    public void onPageShown() {
        loadRepositories();
        refreshTable();
    }

    @FXML
    private void onCreateClicked() {
        openCreateDialog().ifPresent(summary -> {
            refreshTable();
            selectById(summary.getId());
            report("Branch \"" + summary.getName() + "\" created");
        });
    }

    @FXML
    private void onRenameClicked() {
        BranchSummary selected = branchTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            report("Select a branch to rename");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Rename Branch");
        dialog.setHeaderText("Rename branch \"" + selected.getName() + "\"");
        dialog.setContentText("New name:");
        Window owner = branchTable.getScene() == null ? null : branchTable.getScene().getWindow();
        if (owner != null) {
            dialog.initOwner(owner);
            if (owner.getScene() != null) {
                dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
            }
        }
        dialog.getDialogPane().setPadding(new Insets(12));

        dialog.showAndWait().ifPresent(newName -> {
            try {
                BranchSummary renamed = branchService.renameBranch(selected.getId(), newName);
                refreshTable();
                selectById(renamed.getId());
                report("Branch renamed to \"" + renamed.getName() + "\"");
            } catch (IllegalArgumentException ex) {
                showError("Unable to rename branch", ex.getMessage());
            } catch (SQLException ex) {
                showError("Unable to rename branch", ex.getMessage());
            }
        });
    }

    @FXML
    private void onDeleteClicked() {
        BranchSummary selected = branchTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            report("Select a branch to delete");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Branch");
        alert.setHeaderText("Delete branch \"" + selected.getName() + "\"?");
        alert.setContentText("Only branch metadata is removed. Commit history is preserved.");
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);

        alert.showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> {
            try {
                if (branchService.deleteBranch(selected.getId())) {
                    refreshTable();
                    clearDetails();
                    report("Branch deleted");
                } else {
                    report("Branch could not be deleted");
                }
            } catch (IllegalArgumentException | SQLException ex) {
                showError("Unable to delete branch", ex.getMessage());
            }
        });
    }

    @FXML
    private void onSwitchClicked() {
        BranchSummary selected = branchTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            report("Select a branch to switch to");
            return;
        }
        if (selected.isActive()) {
            report("Branch \"" + selected.getName() + "\" is already active");
            return;
        }
        try {
            BranchSummary switched = branchService.switchBranch(selected.getRepositoryId(), selected.getId());
            refreshTable();
            selectById(switched.getId());
            report("Switched to branch \"" + switched.getName() + "\"");
        } catch (IllegalArgumentException | SQLException ex) {
            showError("Unable to switch branch", ex.getMessage());
        }
    }

    @FXML
    private void onMergeClicked() {
        BranchSummary selected = branchTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            report("Select a branch first");
            return;
        }
        report("Use the Merge module to simulate merging \"" + selected.getName() + "\"");
    }

    @FXML
    private void onRefreshClicked() {
        refreshTable();
        report("Branch list refreshed");
    }

    private void loadRepositories() {
        try {
            List<Repository> repositories = branchService.listRepositories();
            Repository previous = repositoryComboBox.getSelectionModel().getSelectedItem();
            repositoryComboBox.getItems().setAll(repositories);
            if (previous != null) {
                repositories.stream()
                        .filter(repo -> Objects.equals(repo.getId(), previous.getId()))
                        .findFirst()
                        .ifPresentOrElse(
                                repo -> repositoryComboBox.getSelectionModel().select(repo),
                                () -> repositoryComboBox.getSelectionModel().clearSelection()
                        );
            }
        } catch (SQLException ex) {
            showError("Unable to load repositories", ex.getMessage());
        }
    }

    private void refreshTable() {
        try {
            Repository selected = repositoryComboBox.getSelectionModel().getSelectedItem();
            Long repositoryId = selected == null ? null : selected.getId();
            tableItems.setAll(branchService.searchSummaries(repositoryId, searchField.getText()));
            boolean empty = tableItems.isEmpty();
            emptyLabel.setVisible(empty);
            emptyLabel.setManaged(empty);
            updateHierarchyDisplay(repositoryId);
        } catch (SQLException ex) {
            showError("Unable to load branches", ex.getMessage());
        }
    }

    private void updateHierarchyDisplay(Long repositoryId) {
        if (repositoryId == null) {
            hierarchyArea.setText("Select a repository to view the branch tree.");
            return;
        }
        List<String> lines = branchService.displayBranchHierarchy();
        if (lines.isEmpty()) {
            hierarchyArea.setText("No branch hierarchy for this repository.");
        } else {
            hierarchyArea.setText(String.join("\n", lines));
        }
    }

    private void configureTable() {
        branchTable.setItems(tableItems);
        branchTable.setPlaceholder(new Label(""));

        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        repositoryColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getRepositoryName()));
        parentColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getParentBranchName()));
        createdColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(DateDisplays.formatDateTime(data.getValue().getCreatedAt())));
        latestCommitColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getShortLatestCommit()));
        statusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getStatus()));
        commitsColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getCommitCount()));

        branchTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> {
                    showDetails(newValue);
                    boolean hasSelection = newValue != null;
                    renameButton.setDisable(!hasSelection);
                    deleteButton.setDisable(!hasSelection);
                    switchButton.setDisable(!hasSelection);
                    mergeButton.setDisable(!hasSelection);
                });

        branchTable.setRowFactory(table -> {
            TableRow<BranchSummary> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    showDetails(row.getItem());
                }
            });
            return row;
        });
    }

    private void showDetails(BranchSummary summary) {
        if (summary == null) {
            clearDetails();
            return;
        }
        detailsPlaceholderLabel.setVisible(false);
        detailsPlaceholderLabel.setManaged(false);
        detailsGrid.setVisible(true);
        detailsGrid.setManaged(true);

        detailNameLabel.setText(summary.getName());
        detailRepositoryLabel.setText(summary.getRepositoryName());
        detailParentLabel.setText(summary.getParentBranchName());
        detailCreatedLabel.setText(DateDisplays.formatDateTime(summary.getCreatedAt()));
        detailLatestCommitLabel.setText(
                summary.getLatestCommitHash() == null || summary.getLatestCommitHash().isBlank()
                        ? "—"
                        : summary.getLatestCommitHash());
        detailCommitCountLabel.setText(Integer.toString(summary.getCommitCount()));
        detailStatusLabel.setText(summary.getStatus());
        detailDescriptionLabel.setText(
                summary.getDescription() == null || summary.getDescription().isBlank()
                        ? "—"
                        : summary.getDescription());
    }

    private void clearDetails() {
        detailsPlaceholderLabel.setVisible(true);
        detailsPlaceholderLabel.setManaged(true);
        detailsGrid.setVisible(false);
        detailsGrid.setManaged(false);
        renameButton.setDisable(true);
        deleteButton.setDisable(true);
        switchButton.setDisable(true);
        mergeButton.setDisable(true);
    }

    private void selectById(Long id) {
        if (id == null) {
            return;
        }
        tableItems.stream()
                .filter(summary -> id.equals(summary.getId()))
                .findFirst()
                .ifPresent(summary -> branchTable.getSelectionModel().select(summary));
    }

    private Optional<BranchSummary> openCreateDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/branch-dialog.fxml"));
            Parent root = loader.load();
            BranchDialogController controller = loader.getController();
            Window owner = branchTable.getScene() == null ? null : branchTable.getScene().getWindow();
            Repository preferred = repositoryComboBox.getSelectionModel().getSelectedItem();
            return controller.showAndWait(owner, root, branchService, preferred);
        } catch (IOException ex) {
            showError("Unable to open dialog", ex.getMessage());
            return Optional.empty();
        }
    }

    private void report(String message) {
        statusReporter.accept(message);
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("GitForge");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
