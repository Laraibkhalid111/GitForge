package com.gitforge.controller;

import com.gitforge.model.RepositorySummary;
import com.gitforge.service.RepositoryService;
import com.gitforge.util.DateDisplays;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Controller for the Repository Management page.
 */
public class RepositoryController {

    @FXML
    private TextField searchField;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private TableView<RepositorySummary> repositoryTable;
    @FXML
    private TableColumn<RepositorySummary, String> nameColumn;
    @FXML
    private TableColumn<RepositorySummary, String> descriptionColumn;
    @FXML
    private TableColumn<RepositorySummary, String> branchColumn;
    @FXML
    private TableColumn<RepositorySummary, Number> commitsColumn;
    @FXML
    private TableColumn<RepositorySummary, String> createdColumn;
    @FXML
    private TableColumn<RepositorySummary, String> statusColumn;
    @FXML
    private Label emptyLabel;

    @FXML
    private Label detailsPlaceholderLabel;
    @FXML
    private GridPane detailsGrid;
    @FXML
    private Label detailNameLabel;
    @FXML
    private Label detailDescriptionLabel;
    @FXML
    private Label detailPathLabel;
    @FXML
    private Label detailCreatedLabel;
    @FXML
    private Label detailBranchLabel;
    @FXML
    private Label detailBranchCountLabel;
    @FXML
    private Label detailCommitCountLabel;
    @FXML
    private Label detailStatusLabel;

    private final RepositoryService repositoryService = new RepositoryService();
    private final ObservableList<RepositorySummary> tableItems = FXCollections.observableArrayList();

    private Consumer<String> statusReporter = message -> {
    };

    @FXML
    private void initialize() {
        configureTable();
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshTable());
        clearDetails();
        refreshTable();
    }

    public void setStatusReporter(Consumer<String> statusReporter) {
        this.statusReporter = statusReporter == null ? message -> {
        } : statusReporter;
    }

    public void onPageShown() {
        refreshTable();
    }

    @FXML
    private void onNewClicked() {
        openDialog(null).ifPresent(summary -> {
            refreshTable();
            selectById(summary.getId());
            report("Repository \"" + summary.getName() + "\" created");
        });
    }

    @FXML
    private void onEditClicked() {
        RepositorySummary selected = repositoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            report("Select a repository to edit");
            return;
        }
        openDialog(selected).ifPresent(summary -> {
            refreshTable();
            selectById(summary.getId());
            report("Repository \"" + summary.getName() + "\" updated");
        });
    }

    @FXML
    private void onDeleteClicked() {
        RepositorySummary selected = repositoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            report("Select a repository to delete");
            return;
        }
        if (!confirmDelete(selected)) {
            return;
        }
        try {
            boolean deleted = repositoryService.deleteRepository(selected.getId());
            refreshTable();
            clearDetails();
            report(deleted
                    ? "Repository \"" + selected.getName() + "\" deleted"
                    : "Repository could not be deleted");
        } catch (SQLException ex) {
            showError("Delete failed", ex.getMessage());
        }
    }

    @FXML
    private void onRefreshClicked() {
        refreshTable();
        report("Repository list refreshed");
    }

    public void openCreateDialog() {
        onNewClicked();
    }

    private void configureTable() {
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(nullToDash(data.getValue().getName())));
        descriptionColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToDash(data.getValue().getDescription())));
        branchColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToDash(data.getValue().getCurrentBranch())));
        commitsColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getTotalCommits()));
        createdColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(DateDisplays.formatDate(data.getValue().getCreatedAt())));
        statusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToDash(data.getValue().getStatus())));

        repositoryTable.setItems(tableItems);
        repositoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        repositoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            boolean hasSelection = newItem != null;
            editButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);
            if (hasSelection) {
                showDetails(newItem);
            } else {
                clearDetails();
            }
        });

        repositoryTable.setRowFactory(table -> {
            TableRow<RepositorySummary> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()
                        && event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2) {
                    repositoryTable.getSelectionModel().select(row.getItem());
                    showDetails(row.getItem());
                }
            });
            return row;
        });
    }

    private void refreshTable() {
        try {
            String query = searchField.getText();
            tableItems.setAll(repositoryService.searchSummaries(query));
            boolean empty = tableItems.isEmpty();
            emptyLabel.setVisible(empty);
            emptyLabel.setManaged(empty);
        } catch (SQLException ex) {
            showError("Unable to load repositories", ex.getMessage());
        }
    }

    private void showDetails(RepositorySummary summary) {
        detailsPlaceholderLabel.setVisible(false);
        detailsPlaceholderLabel.setManaged(false);
        detailsGrid.setVisible(true);
        detailsGrid.setManaged(true);

        detailNameLabel.setText(nullToDash(summary.getName()));
        detailDescriptionLabel.setText(nullToDash(summary.getDescription()));
        detailPathLabel.setText(nullToDash(summary.getPath()));
        detailCreatedLabel.setText(DateDisplays.formatDateTime(summary.getCreatedAt()));
        detailBranchLabel.setText(nullToDash(summary.getCurrentBranch()));
        detailBranchCountLabel.setText(Integer.toString(summary.getTotalBranches()));
        detailCommitCountLabel.setText(Integer.toString(summary.getTotalCommits()));
        detailStatusLabel.setText(nullToDash(summary.getStatus()));
    }

    private void clearDetails() {
        detailsPlaceholderLabel.setVisible(true);
        detailsPlaceholderLabel.setManaged(true);
        detailsGrid.setVisible(false);
        detailsGrid.setManaged(false);
    }

    private Optional<RepositorySummary> openDialog(RepositorySummary existing) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/fxml/repository-dialog.fxml"),
                    "repository-dialog.fxml not found"
            ));
            Parent root = loader.load();
            RepositoryDialogController controller = loader.getController();
            Window owner = repositoryTable.getScene() != null ? repositoryTable.getScene().getWindow() : null;
            return controller.showAndWait(owner, root, existing, repositoryService);
        } catch (IOException ex) {
            showError("Unable to open dialog", ex.getMessage());
            return Optional.empty();
        }
    }

    private boolean confirmDelete(RepositorySummary summary) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Repository");
        alert.setHeaderText("Delete \"" + summary.getName() + "\"?");
        alert.setContentText(
                "This removes the simulated repository and related records from SQLite. This cannot be undone.");
        if (repositoryTable.getScene() != null) {
            alert.initOwner(repositoryTable.getScene().getWindow());
        }
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void selectById(Long id) {
        if (id == null) {
            return;
        }
        for (RepositorySummary item : tableItems) {
            if (id.equals(item.getId())) {
                repositoryTable.getSelectionModel().select(item);
                repositoryTable.scrollTo(item);
                showDetails(item);
                return;
            }
        }
    }

    private void report(String message) {
        statusReporter.accept(message);
    }

    private void showError(String title, String detail) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(detail == null ? "An unexpected error occurred." : detail);
        if (repositoryTable.getScene() != null) {
            alert.initOwner(repositoryTable.getScene().getWindow());
        }
        alert.showAndWait();
        report(title);
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
