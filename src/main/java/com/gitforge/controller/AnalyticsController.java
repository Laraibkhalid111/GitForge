package com.gitforge.controller;

import com.gitforge.model.AnalyticsSnapshot;
import com.gitforge.model.AnalyticsSnapshot.ActivityItem;
import com.gitforge.model.AnalyticsSnapshot.Filter;
import com.gitforge.model.AnalyticsSnapshot.RepositoryAnalytics;
import com.gitforge.model.Branch;
import com.gitforge.model.Repository;
import com.gitforge.service.AnalyticsService;
import com.gitforge.util.DateDisplays;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Repository Analytics Dashboard controller.
 * Reads {@link AnalyticsService} snapshots through the custom analytics cache.
 */
public class AnalyticsController {

    private static final double STATS_TWO_COLUMN_BREAKPOINT = 980;
    private static final double STATS_ONE_COLUMN_BREAKPOINT = 640;
    private static final double CHARTS_STACK_BREAKPOINT = 900;

    @FXML
    private ComboBox<Repository> repositoryFilter;
    @FXML
    private ComboBox<Branch> branchFilter;
    @FXML
    private ComboBox<String> authorFilter;
    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private Label cacheStatusLabel;

    @FXML
    private GridPane statsGrid;
    @FXML
    private VBox statCardRepository;
    @FXML
    private VBox statCardCommits;
    @FXML
    private VBox statCardBranches;
    @FXML
    private VBox statCardMerges;
    @FXML
    private VBox statCardActiveBranch;
    @FXML
    private VBox statCardLatestCommit;
    @FXML
    private VBox statCardHealth;

    @FXML
    private Label cardRepositoryValue;
    @FXML
    private Label cardCommitsValue;
    @FXML
    private Label cardBranchesValue;
    @FXML
    private Label cardMergesValue;
    @FXML
    private Label cardActiveBranchValue;
    @FXML
    private Label cardLatestCommitValue;
    @FXML
    private Label cardHealthValue;

    @FXML
    private GridPane chartsGrid;
    @FXML
    private VBox chartCardRepoCommits;
    @FXML
    private VBox chartCardBranchPie;
    @FXML
    private VBox chartCardTimeline;
    @FXML
    private VBox chartCardGrowth;
    @FXML
    private VBox chartCardContributors;

    @FXML
    private BarChart<String, Number> commitsPerRepoChart;
    @FXML
    private PieChart branchDistributionChart;
    @FXML
    private LineChart<String, Number> commitTimelineChart;
    @FXML
    private AreaChart<String, Number> repositoryGrowthChart;
    @FXML
    private BarChart<Number, String> topContributorsChart;

    @FXML
    private TableView<RepositoryAnalytics> repositoryStatsTable;
    @FXML
    private TableColumn<RepositoryAnalytics, String> repoNameColumn;
    @FXML
    private TableColumn<RepositoryAnalytics, String> repoBranchColumn;
    @FXML
    private TableColumn<RepositoryAnalytics, Number> repoBranchesColumn;
    @FXML
    private TableColumn<RepositoryAnalytics, Number> repoCommitsColumn;
    @FXML
    private TableColumn<RepositoryAnalytics, Number> repoMergesColumn;
    @FXML
    private TableColumn<RepositoryAnalytics, String> repoLatestColumn;
    @FXML
    private TableColumn<RepositoryAnalytics, String> repoCreatedColumn;
    @FXML
    private TableColumn<RepositoryAnalytics, String> repoSizeColumn;
    @FXML
    private TableColumn<RepositoryAnalytics, String> repoStatusColumn;

    @FXML
    private ListView<ActivityItem> activityList;

    private final AnalyticsService analyticsService = new AnalyticsService();
    private final ObservableList<RepositoryAnalytics> statsItems = FXCollections.observableArrayList();
    private final ObservableList<ActivityItem> activityItems = FXCollections.observableArrayList();

    private List<Node> statCards;
    private List<Node> chartCards;
    private int currentStatsColumns = -1;
    private int currentChartColumns = -1;
    private boolean suppressFilterEvents;
    private Consumer<String> statusReporter = message -> {
    };

    @FXML
    private void initialize() {
        statCards = List.of(
                statCardRepository, statCardCommits, statCardBranches, statCardMerges,
                statCardActiveBranch, statCardLatestCommit, statCardHealth
        );
        chartCards = List.of(
                chartCardRepoCommits, chartCardBranchPie, chartCardTimeline,
                chartCardGrowth, chartCardContributors
        );

        configureFilters();
        configureStatsTable();
        configureActivityList();
        wireCardHoverAnimations(statCards);
        bindResponsiveLayout();
        loadFilterOptions();
        refreshDashboard(false);
    }

    public void setStatusReporter(Consumer<String> statusReporter) {
        this.statusReporter = statusReporter == null ? message -> {
        } : statusReporter;
    }

    public void onPageShown() {
        loadFilterOptions();
        refreshDashboard(false);
    }

    @FXML
    private void onApplyFilters() {
        refreshDashboard(false);
        report("Analytics filters applied");
    }

    @FXML
    private void onClearFilters() {
        suppressFilterEvents = true;
        repositoryFilter.getSelectionModel().clearSelection();
        branchFilter.getSelectionModel().clearSelection();
        authorFilter.getSelectionModel().clearSelection();
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        suppressFilterEvents = false;
        refreshDashboard(false);
        report("Analytics filters cleared");
    }

    @FXML
    public void onRefreshClicked() {
        analyticsService.invalidateCache();
        refreshDashboard(true);
        report("Analytics refreshed from SQLite");
    }

    private void configureFilters() {
        repositoryFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(Repository repository) {
                return repository == null ? "" : repository.getName();
            }

            @Override
            public Repository fromString(String string) {
                return null;
            }
        });
        branchFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(Branch branch) {
                return branch == null ? "" : branch.getName();
            }

            @Override
            public Branch fromString(String string) {
                return null;
            }
        });
        repositoryFilter.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (!suppressFilterEvents) {
                reloadBranchesForFilter(n);
            }
        });
    }

    private void configureStatsTable() {
        repositoryStatsTable.setItems(statsItems);
        repoNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        repoBranchColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCurrentBranch()));
        repoBranchesColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getBranchCount()));
        repoCommitsColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getCommitCount()));
        repoMergesColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getMergeCount()));
        repoLatestColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getLatestCommit()));
        repoCreatedColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(DateDisplays.formatDateTime(data.getValue().getCreatedAt())));
        repoSizeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getSimulatedSize()));
        repoStatusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStatus()));
    }

    private void configureActivityList() {
        activityList.setItems(activityItems);
        activityList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ActivityItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                FontIcon icon = new FontIcon(item.getIconLiteral());
                icon.setIconSize(16);
                icon.getStyleClass().add("activity-icon");
                Label title = new Label(item.getTitle());
                title.getStyleClass().add("activity-title");
                title.setWrapText(true);
                Label meta = new Label(item.getType() + " · " + DateDisplays.formatDateTime(item.getTimestamp())
                        + " · " + item.getDetail());
                meta.getStyleClass().add("activity-meta");
                meta.setWrapText(true);
                VBox text = new VBox(2, title, meta);
                HBox row = new HBox(10, icon, text);
                row.setAlignment(Pos.TOP_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
    }

    private void loadFilterOptions() {
        try {
            suppressFilterEvents = true;
            Repository previousRepo = repositoryFilter.getSelectionModel().getSelectedItem();
            String previousAuthor = authorFilter.getSelectionModel().getSelectedItem();

            List<Repository> repositories = analyticsService.listRepositories();
            repositoryFilter.getItems().setAll(repositories);
            if (previousRepo != null) {
                repositories.stream()
                        .filter(repo -> Objects.equals(repo.getId(), previousRepo.getId()))
                        .findFirst()
                        .ifPresent(repo -> repositoryFilter.getSelectionModel().select(repo));
            }

            authorFilter.getItems().setAll(analyticsService.listAuthors());
            if (previousAuthor != null && authorFilter.getItems().contains(previousAuthor)) {
                authorFilter.getSelectionModel().select(previousAuthor);
            }

            reloadBranchesForFilter(repositoryFilter.getSelectionModel().getSelectedItem());
        } catch (SQLException ex) {
            report("Unable to load analytics filters: " + ex.getMessage());
        } finally {
            suppressFilterEvents = false;
        }
    }

    private void reloadBranchesForFilter(Repository repository) {
        try {
            Branch previous = branchFilter.getSelectionModel().getSelectedItem();
            List<Branch> branches = analyticsService.listBranches(repository == null ? null : repository.getId());
            branchFilter.getItems().setAll(branches);
            if (previous != null) {
                branches.stream()
                        .filter(branch -> Objects.equals(branch.getId(), previous.getId()))
                        .findFirst()
                        .ifPresentOrElse(
                                branch -> branchFilter.getSelectionModel().select(branch),
                                () -> branchFilter.getSelectionModel().clearSelection()
                        );
            }
        } catch (SQLException ex) {
            report("Unable to load branches: " + ex.getMessage());
        }
    }

    private void refreshDashboard(boolean forceRefresh) {
        try {
            AnalyticsSnapshot snapshot = analyticsService.loadDashboard(currentFilter(), forceRefresh);
            applySnapshot(snapshot);
            cacheStatusLabel.setText(snapshot.isFromCache()
                    ? "Loaded from analytics cache"
                    : "Loaded from SQLite · cached");
            fadeIn(statsGrid);
        } catch (SQLException ex) {
            report("Unable to load analytics: " + ex.getMessage());
        }
    }

    private Filter currentFilter() {
        Filter filter = new Filter();
        Repository repository = repositoryFilter.getSelectionModel().getSelectedItem();
        Branch branch = branchFilter.getSelectionModel().getSelectedItem();
        filter.setRepositoryId(repository == null ? null : repository.getId());
        filter.setBranchId(branch == null ? null : branch.getId());
        filter.setAuthor(authorFilter.getSelectionModel().getSelectedItem());
        filter.setFromDate(fromDatePicker.getValue());
        filter.setToDate(toDatePicker.getValue());
        return filter;
    }

    private void applySnapshot(AnalyticsSnapshot snapshot) {
        cardRepositoryValue.setText(Integer.toString(snapshot.getTotalRepositories()));
        cardCommitsValue.setText(Integer.toString(snapshot.getTotalCommits()));
        cardBranchesValue.setText(Integer.toString(snapshot.getTotalBranches()));
        cardMergesValue.setText(Integer.toString(snapshot.getTotalMerges()));
        cardActiveBranchValue.setText(snapshot.getActiveBranch());
        cardLatestCommitValue.setText(snapshot.getLatestCommit());
        cardHealthValue.setText(snapshot.getHealthScore() + "%");

        populateNamedNumberChart(commitsPerRepoChart, "Commits", snapshot.getCommitsPerRepository());
        populatePie(branchDistributionChart, snapshot.getBranchDistribution());
        populateNamedNumberChart(commitTimelineChart, "Commits", snapshot.getCommitActivityTimeline());
        populateNamedNumberChart(repositoryGrowthChart, "Repositories", snapshot.getRepositoryGrowth());
        populateHorizontalBar(topContributorsChart, snapshot.getTopContributors());

        statsItems.setAll(snapshot.getRepositoryStats());
        activityItems.setAll(snapshot.getRecentActivity());
    }

    @SuppressWarnings("unchecked")
    private void populateNamedNumberChart(XYChart<String, Number> chart, String seriesName, Map<String, Integer> values) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(seriesName);
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        chart.setData(FXCollections.observableArrayList(series));
    }

    private void populatePie(PieChart chart, Map<String, Integer> values) {
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            data.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }
        if (data.isEmpty()) {
            data.add(new PieChart.Data("No branches", 1));
        }
        chart.setData(data);
    }

    private void populateHorizontalBar(BarChart<Number, String> chart, Map<String, Integer> values) {
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Commits");
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(values.entrySet());
        for (int i = entries.size() - 1; i >= 0; i--) {
            Map.Entry<String, Integer> entry = entries.get(i);
            series.getData().add(new XYChart.Data<>(entry.getValue(), entry.getKey()));
        }
        chart.setData(FXCollections.observableArrayList(series));
    }

    private void wireCardHoverAnimations(List<Node> cards) {
        for (Node card : cards) {
            card.setOnMouseEntered(event -> {
                ScaleTransition up = new ScaleTransition(Duration.millis(140), card);
                up.setToX(1.02);
                up.setToY(1.02);
                up.play();
            });
            card.setOnMouseExited(event -> {
                ScaleTransition down = new ScaleTransition(Duration.millis(140), card);
                down.setToX(1.0);
                down.setToY(1.0);
                down.play();
            });
        }
    }

    private void bindResponsiveLayout() {
        statsGrid.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }
            Node parent = statsGrid.getParent();
            while (parent != null && !(parent instanceof ScrollPane)) {
                parent = parent.getParent();
            }
            if (parent instanceof ScrollPane scrollPane) {
                scrollPane.viewportBoundsProperty().addListener((o, a, b) -> {
                    if (b != null && b.getWidth() > 0) {
                        applyResponsiveLayout(b.getWidth());
                    }
                });
                if (scrollPane.getViewportBounds() != null && scrollPane.getViewportBounds().getWidth() > 0) {
                    applyResponsiveLayout(scrollPane.getViewportBounds().getWidth());
                }
            }
        });
        applyResponsiveLayout(STATS_TWO_COLUMN_BREAKPOINT + 1);
    }

    private void applyResponsiveLayout(double width) {
        int statsColumns = width < STATS_ONE_COLUMN_BREAKPOINT ? 1
                : width < STATS_TWO_COLUMN_BREAKPOINT ? 2
                : 4;
        if (statsColumns != currentStatsColumns) {
            currentStatsColumns = statsColumns;
            layoutGrid(statsGrid, statCards, statsColumns);
        }

        int chartColumns = width < CHARTS_STACK_BREAKPOINT ? 1 : 2;
        if (chartColumns != currentChartColumns) {
            currentChartColumns = chartColumns;
            layoutGrid(chartsGrid, chartCards, chartColumns);
        }
    }

    private void layoutGrid(GridPane grid, List<Node> nodes, int columns) {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        double percent = 100.0 / columns;
        for (int i = 0; i < columns; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setHgrow(Priority.ALWAYS);
            column.setPercentWidth(percent);
            column.setMinWidth(columns == 1 ? 200 : 140);
            grid.getColumnConstraints().add(column);
        }
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            int colSpan = (node == chartCardContributors && columns >= 2) ? 2 : 1;
            int column = i % columns;
            if (colSpan == 2) {
                column = 0;
            }
            GridPane.setColumnIndex(node, column);
            GridPane.setRowIndex(node, i / columns);
            GridPane.setColumnSpan(node, colSpan);
            GridPane.setHgrow(node, Priority.ALWAYS);
            GridPane.setVgrow(node, Priority.ALWAYS);
            grid.getChildren().add(node);
        }
    }

    private static void fadeIn(Node node) {
        FadeTransition fade = new FadeTransition(Duration.millis(220), node);
        fade.setFromValue(0.35);
        fade.setToValue(1.0);
        fade.play();
    }

    private void report(String message) {
        statusReporter.accept(message);
    }
}
