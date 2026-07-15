package com.gitforge.controller;

import com.gitforge.model.CommitSummary;
import com.gitforge.model.Repository;
import com.gitforge.service.CommitService;
import com.gitforge.util.DateDisplays;
import com.gitforge.util.UiDialogs;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Controller for the Commit Management page.
 * Commit history is loaded from SQLite into a linked list and displayed via {@link CommitService#displayCommitHistory()}.
 */
public class CommitController {

    @FXML
    private ComboBox<Repository> repositoryComboBox;
    @FXML
    private TextField searchField;
    @FXML
    private Button deleteButton;
    @FXML
    private TableView<CommitSummary> commitTable;
    @FXML
    private TableColumn<CommitSummary, String> hashColumn;
    @FXML
    private TableColumn<CommitSummary, String> messageColumn;
    @FXML
    private TableColumn<CommitSummary, String> authorColumn;
    @FXML
    private TableColumn<CommitSummary, String> typeColumn;
    @FXML
    private TableColumn<CommitSummary, String> branchColumn;
    @FXML
    private TableColumn<CommitSummary, String> repositoryColumn;
    @FXML
    private TableColumn<CommitSummary, String> dateColumn;
    @FXML
    private TableColumn<CommitSummary, Number> filesColumn;
    @FXML
    private Label emptyLabel;

    @FXML
    private Label detailsPlaceholderLabel;
    @FXML
    private GridPane detailsGrid;
    @FXML
    private Label detailHashLabel;
    @FXML
    private Label detailMessageLabel;
    @FXML
    private Label detailAuthorLabel;
    @FXML
    private Label detailTypeLabel;
    @FXML
    private Label detailBranchLabel;
    @FXML
    private Label detailRepositoryLabel;
    @FXML
    private Label detailDateLabel;
    @FXML
    private Label detailParentLabel;
    @FXML
    private Label detailFilesLabel;

    private final CommitService commitService = new CommitService();
    private final ObservableList<CommitSummary> tableItems = FXCollections.observableArrayList();

    private Consumer<String> statusReporter = message -> {
    };

    @FXML
    private void initialize() {
        configureTable();
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
    private void onNewClicked() {
        openDialog().ifPresent(summary -> {
            refreshTable();
            selectById(summary.getId());
            report("Commit \"" + summary.getShortHash() + "\" created");
        });
    }

    @FXML
    private void onDeleteClicked() {
        CommitSummary selected = commitTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            report("Select a commit to delete");
            return;
        }

        Window owner = commitTable.getScene() == null ? null : commitTable.getScene().getWindow();
        if (!UiDialogs.confirmDelete(
                owner,
                "Delete Commit",
                "Delete commit " + selected.getShortHash() + "?",
                "This removes the simulated commit from SQLite and the linked history."
        )) {
            return;
        }
        try {
            if (commitService.deleteCommit(selected.getId())) {
                refreshTable();
                clearDetails();
                report("Commit deleted");
            } else {
                report("Commit could not be deleted");
            }
        } catch (SQLException ex) {
            showError("Unable to delete commit", ex.getMessage());
        }
    }

    @FXML
    private void onRefreshClicked() {
        refreshTable();
        report("Commit history refreshed");
    }

    private void loadRepositories() {
        try {
            List<Repository> repositories = commitService.listRepositories();
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
            tableItems.setAll(commitService.searchSummaries(repositoryId, searchField.getText()));
            boolean empty = commitService.getCommitHistory().isEmpty();
            emptyLabel.setVisible(empty);
            emptyLabel.setManaged(empty);
        } catch (SQLException ex) {
            showError("Unable to load commits", ex.getMessage());
        }
    }

    private void configureTable() {
        commitTable.setItems(tableItems);
        commitTable.setPlaceholder(new Label(""));

        hashColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getShortHash()));
        messageColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getMessage()));
        authorColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getAuthor()));
        typeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getCommitType()));
        branchColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getBranchName()));
        repositoryColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getRepositoryName()));
        dateColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(DateDisplays.formatDateTime(data.getValue().getCommittedAt())));
        filesColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getFilesChanged()));

        commitTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> showDetails(newValue));

        commitTable.setRowFactory(table -> {
            TableRow<CommitSummary> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    showDetails(row.getItem());
                }
            });
            return row;
        });

        commitTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            boolean hasSelection = newValue != null;
            deleteButton.setDisable(!hasSelection);
        });
    }

    private void showDetails(CommitSummary summary) {
        if (summary == null) {
            clearDetails();
            return;
        }
        detailsPlaceholderLabel.setVisible(false);
        detailsPlaceholderLabel.setManaged(false);
        detailsGrid.setVisible(true);
        detailsGrid.setManaged(true);

        detailHashLabel.setText(summary.getHash());
        detailMessageLabel.setText(summary.getMessage());
        detailAuthorLabel.setText(summary.getAuthor());
        detailTypeLabel.setText(summary.getCommitType());
        detailBranchLabel.setText(summary.getBranchName());
        detailRepositoryLabel.setText(summary.getRepositoryName());
        detailDateLabel.setText(DateDisplays.formatDateTime(summary.getCommittedAt()));
        detailParentLabel.setText(summary.getParentHash() == null ? "—" : summary.getParentHash());
        detailFilesLabel.setText(Integer.toString(summary.getFilesChanged()));
    }

    private void clearDetails() {
        detailsPlaceholderLabel.setVisible(true);
        detailsPlaceholderLabel.setManaged(true);
        detailsGrid.setVisible(false);
        detailsGrid.setManaged(false);
        deleteButton.setDisable(true);
    }

    private void selectById(Long id) {
        if (id == null) {
            return;
        }
        tableItems.stream()
                .filter(summary -> id.equals(summary.getId()))
                .findFirst()
                .ifPresent(summary -> commitTable.getSelectionModel().select(summary));
    }

    private Optional<CommitSummary> openDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/commit-dialog.fxml"));
            Parent root = loader.load();
            CommitDialogController controller = loader.getController();
            Window owner = commitTable.getScene() == null ? null : commitTable.getScene().getWindow();
            return controller.showAndWait(owner, root, commitService);
        } catch (IOException ex) {
            showError("Unable to open dialog", ex.getMessage());
            return Optional.empty();
        }
    }

    private void report(String message) {
        statusReporter.accept(message);
    }

    private void showError(String header, String message) {
        Window owner = commitTable.getScene() == null ? null : commitTable.getScene().getWindow();
        UiDialogs.error(owner, header, message);
    }
}
