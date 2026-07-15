package com.gitforge.controller;

import com.gitforge.model.Branch;
import com.gitforge.model.Merge;
import com.gitforge.model.MergePreview;
import com.gitforge.model.MergeSummary;
import com.gitforge.model.Repository;
import com.gitforge.service.MergeService;
import com.gitforge.util.DateDisplays;
import javafx.animation.FadeTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Controller for the Merge Management page.
 */
public class MergeController {

    @FXML
    private ComboBox<Repository> repositoryComboBox;
    @FXML
    private ComboBox<Branch> sourceBranchComboBox;
    @FXML
    private ComboBox<Branch> targetBranchComboBox;
    @FXML
    private RadioButton fastForwardRadio;
    @FXML
    private RadioButton threeWayRadio;
    @FXML
    private RadioButton simulationOnlyRadio;
    @FXML
    private Button mergeButton;

    @FXML
    private Label previewPlaceholderLabel;
    @FXML
    private GridPane previewGrid;
    @FXML
    private Label previewSourceLabel;
    @FXML
    private Label previewTargetLabel;
    @FXML
    private Label previewLatestLabel;
    @FXML
    private Label previewCommitsLabel;
    @FXML
    private Label previewExpectedLabel;
    @FXML
    private Label previewConflictLabel;
    @FXML
    private Label previewStrategyLabel;

    @FXML
    private TableView<MergeSummary> historyTable;
    @FXML
    private TableColumn<MergeSummary, Number> idColumn;
    @FXML
    private TableColumn<MergeSummary, String> sourceColumn;
    @FXML
    private TableColumn<MergeSummary, String> targetColumn;
    @FXML
    private TableColumn<MergeSummary, String> strategyColumn;
    @FXML
    private TableColumn<MergeSummary, String> statusColumn;
    @FXML
    private TableColumn<MergeSummary, String> timestampColumn;
    @FXML
    private TableColumn<MergeSummary, String> mergeCommitColumn;
    @FXML
    private Label historyEmptyLabel;

    private final MergeService mergeService = new MergeService();
    private final ObservableList<MergeSummary> historyItems = FXCollections.observableArrayList();

    private MergePreview currentPreview;
    private Consumer<String> statusReporter = message -> {
    };

    @FXML
    private void initialize() {
        configureConverters();
        configureHistoryTable();
        repositoryComboBox.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> onRepositoryChanged(newValue));
        clearPreview();
        loadRepositories();
        refreshHistory();
    }

    public void setStatusReporter(Consumer<String> statusReporter) {
        this.statusReporter = statusReporter == null ? message -> {
        } : statusReporter;
    }

    public void onPageShown() {
        loadRepositories();
        refreshHistory();
    }

    @FXML
    private void onPreviewClicked() {
        try {
            MergeSelection selection = requireSelection();
            currentPreview = mergeService.previewMerge(
                    selection.repository().getId(),
                    selection.source().getId(),
                    selection.target().getId(),
                    selectedStrategy()
            );
            showPreview(currentPreview);
            fadeIn(previewGrid);
            report("Merge preview ready");
        } catch (IllegalArgumentException ex) {
            showError("Unable to preview merge", ex.getMessage());
        } catch (SQLException ex) {
            showError("Unable to preview merge", ex.getMessage());
        }
    }

    @FXML
    private void onMergeClicked() {
        try {
            MergeSelection selection = requireSelection();
            if (currentPreview == null
                    || !Objects.equals(currentPreview.getSourceBranchName(), selection.source().getName())
                    || !Objects.equals(currentPreview.getTargetBranchName(), selection.target().getName())
                    || !Objects.equals(currentPreview.getStrategy(), selectedStrategy())) {
                currentPreview = mergeService.previewMerge(
                        selection.repository().getId(),
                        selection.source().getId(),
                        selection.target().getId(),
                        selectedStrategy()
                );
                showPreview(currentPreview);
            }

            if (!confirmConflictContinuation(currentPreview.getConflictStatus())) {
                report("Merge cancelled");
                return;
            }

            MergeSummary result = mergeService.executeMerge(
                    selection.repository().getId(),
                    selection.source().getId(),
                    selection.target().getId(),
                    selectedStrategy(),
                    currentPreview.getConflictStatus(),
                    true
            );
            refreshHistory();
            selectHistoryRow(result.getId());
            report("Merged " + result.getSourceBranchName() + " into " + result.getTargetBranchName());
        } catch (IllegalArgumentException ex) {
            showError("Unable to merge", ex.getMessage());
        } catch (SQLException ex) {
            showError("Unable to merge", ex.getMessage());
        }
    }

    @FXML
    private void onHistoryClicked() {
        refreshHistory();
        report("Merge history refreshed");
        if (!historyItems.isEmpty()) {
            historyTable.scrollTo(0);
            historyTable.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onRefreshClicked() {
        loadRepositories();
        refreshHistory();
        report("Merge workspace refreshed");
    }

    private void loadRepositories() {
        try {
            List<Repository> repositories = mergeService.listRepositories();
            Repository previous = repositoryComboBox.getSelectionModel().getSelectedItem();
            repositoryComboBox.getItems().setAll(repositories);
            if (previous != null) {
                repositories.stream()
                        .filter(repo -> Objects.equals(repo.getId(), previous.getId()))
                        .findFirst()
                        .ifPresentOrElse(
                                repo -> repositoryComboBox.getSelectionModel().select(repo),
                                this::selectFirstRepository
                        );
            } else {
                selectFirstRepository();
            }
        } catch (SQLException ex) {
            showError("Unable to load repositories", ex.getMessage());
        }
    }

    private void selectFirstRepository() {
        if (!repositoryComboBox.getItems().isEmpty()) {
            repositoryComboBox.getSelectionModel().selectFirst();
        } else {
            sourceBranchComboBox.getItems().clear();
            targetBranchComboBox.getItems().clear();
        }
    }

    private void onRepositoryChanged(Repository repository) {
        sourceBranchComboBox.getItems().clear();
        targetBranchComboBox.getItems().clear();
        clearPreview();
        if (repository == null) {
            refreshHistory();
            return;
        }
        try {
            List<Branch> branches = mergeService.listBranchesForRepository(repository.getId());
            sourceBranchComboBox.getItems().setAll(branches);
            targetBranchComboBox.getItems().setAll(branches);
            if (branches.size() >= 2) {
                sourceBranchComboBox.getSelectionModel().select(0);
                targetBranchComboBox.getSelectionModel().select(1);
            } else if (branches.size() == 1) {
                sourceBranchComboBox.getSelectionModel().selectFirst();
                targetBranchComboBox.getSelectionModel().selectFirst();
            }
            refreshHistory();
        } catch (SQLException ex) {
            showError("Unable to load branches", ex.getMessage());
        }
    }

    private void refreshHistory() {
        try {
            Repository selected = repositoryComboBox.getSelectionModel().getSelectedItem();
            Long repositoryId = selected == null ? null : selected.getId();
            historyItems.setAll(mergeService.listHistory(repositoryId));
            boolean empty = historyItems.isEmpty();
            historyEmptyLabel.setVisible(empty);
            historyEmptyLabel.setManaged(empty);
        } catch (SQLException ex) {
            showError("Unable to load merge history", ex.getMessage());
        }
    }

    private void configureConverters() {
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
        StringConverter<Branch> branchConverter = new StringConverter<>() {
            @Override
            public String toString(Branch branch) {
                return branch == null ? "" : branch.getName();
            }

            @Override
            public Branch fromString(String string) {
                return null;
            }
        };
        sourceBranchComboBox.setConverter(branchConverter);
        targetBranchComboBox.setConverter(branchConverter);
    }

    private void configureHistoryTable() {
        historyTable.setItems(historyItems);
        historyTable.setPlaceholder(new Label(""));
        idColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        sourceColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getSourceBranchName()));
        targetColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getTargetBranchName()));
        strategyColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getStrategy()));
        statusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getStatus()));
        timestampColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(DateDisplays.formatDateTime(data.getValue().getMergedAt())));
        mergeCommitColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getShortMergeCommit()));
    }

    private void showPreview(MergePreview preview) {
        previewPlaceholderLabel.setVisible(false);
        previewPlaceholderLabel.setManaged(false);
        previewGrid.setVisible(true);
        previewGrid.setManaged(true);

        previewSourceLabel.setText(preview.getSourceBranchName());
        previewTargetLabel.setText(preview.getTargetBranchName());
        previewLatestLabel.setText(preview.getLatestCommitDisplay());
        previewCommitsLabel.setText(Integer.toString(preview.getCommitsToMerge()));
        previewExpectedLabel.setText(preview.getExpectedMergeCommit());
        previewConflictLabel.setText(preview.getConflictStatus());
        previewStrategyLabel.setText(preview.getStrategy());
    }

    private void clearPreview() {
        currentPreview = null;
        previewPlaceholderLabel.setVisible(true);
        previewPlaceholderLabel.setManaged(true);
        previewGrid.setVisible(false);
        previewGrid.setManaged(false);
    }

    private boolean confirmConflictContinuation(String conflictStatus) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Merge Simulation");
        alert.setHeaderText(conflictTitle(conflictStatus));
        alert.setContentText(conflictBody(conflictStatus)
                + "\n\nContinue with the simulated merge?");
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static String conflictTitle(String conflictStatus) {
        if (Merge.CONFLICT_MAJOR.equals(conflictStatus)) {
            return "Major Conflict Detected";
        }
        if (Merge.CONFLICT_MINOR.equals(conflictStatus)) {
            return "Minor Conflict Detected";
        }
        return "No Conflicts Detected";
    }

    private static String conflictBody(String conflictStatus) {
        if (Merge.CONFLICT_MAJOR.equals(conflictStatus)) {
            return "Simulation found overlapping changes on both branches. "
                    + "GitForge will still create a simulated merge commit.";
        }
        if (Merge.CONFLICT_MINOR.equals(conflictStatus)) {
            return "Simulation found a minor conflict that can be auto-resolved.";
        }
        return "Branches can be merged cleanly in this simulation.";
    }

    private MergeSelection requireSelection() {
        Repository repository = repositoryComboBox.getSelectionModel().getSelectedItem();
        Branch source = sourceBranchComboBox.getSelectionModel().getSelectedItem();
        Branch target = targetBranchComboBox.getSelectionModel().getSelectedItem();
        if (repository == null) {
            throw new IllegalArgumentException("Select a repository");
        }
        if (source == null) {
            throw new IllegalArgumentException("Select a source branch");
        }
        if (target == null) {
            throw new IllegalArgumentException("Select a target branch");
        }
        return new MergeSelection(repository, source, target);
    }

    private String selectedStrategy() {
        if (threeWayRadio.isSelected()) {
            return Merge.STRATEGY_THREE_WAY;
        }
        if (simulationOnlyRadio.isSelected()) {
            return Merge.STRATEGY_SIMULATION_ONLY;
        }
        return Merge.STRATEGY_FAST_FORWARD;
    }

    private void selectHistoryRow(Long id) {
        if (id == null) {
            return;
        }
        historyItems.stream()
                .filter(summary -> id.equals(summary.getId()))
                .findFirst()
                .ifPresent(summary -> historyTable.getSelectionModel().select(summary));
    }

    private static void fadeIn(GridPane node) {
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(220), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
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

    private record MergeSelection(Repository repository, Branch source, Branch target) {
    }
}
