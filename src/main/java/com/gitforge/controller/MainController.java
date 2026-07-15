package com.gitforge.controller;

import com.gitforge.service.RepositoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller for the GitForge application shell, Dashboard, and page navigation.
 */
public class MainController {

    private enum Page {
        DASHBOARD,
        REPOSITORY,
        COMMITS,
        BRANCHES,
        MERGE,
        COMMIT_GRAPH,
        PLACEHOLDER
    }

    private static final String NAV_SELECTED = "nav-item-selected";
    private static final double STATS_TWO_COLUMN_BREAKPOINT = 980;
    private static final double STATS_ONE_COLUMN_BREAKPOINT = 640;
    private static final double CHARTS_STACK_BREAKPOINT = 900;

    @FXML
    private Label toolbarTitleLabel;
    @FXML
    private Label toolbarSubtitleLabel;
    @FXML
    private Label statusMessageLabel;
    @FXML
    private Label statusPageLabel;

    @FXML
    private ScrollPane dashboardPage;
    @FXML
    private BorderPane repositoryPage;
    @FXML
    private RepositoryController repositoryPageController;
    @FXML
    private BorderPane commitPage;
    @FXML
    private CommitController commitPageController;
    @FXML
    private BorderPane branchPage;
    @FXML
    private BranchController branchPageController;
    @FXML
    private BorderPane mergePage;
    @FXML
    private MergeController mergePageController;
    @FXML
    private BorderPane commitGraphPage;
    @FXML
    private CommitGraphController commitGraphPageController;
    @FXML
    private VBox modulePlaceholderPage;
    @FXML
    private Label placeholderTitleLabel;
    @FXML
    private Label placeholderBodyLabel;
    @FXML
    private FontIcon placeholderIcon;

    @FXML
    private Label cardRepositoryValue;
    @FXML
    private Label cardCommitsValue;
    @FXML
    private Label cardBranchValue;
    @FXML
    private Label cardMergeValue;

    @FXML
    private GridPane statsGrid;
    @FXML
    private GridPane chartsGrid;
    @FXML
    private VBox statCardRepository;
    @FXML
    private VBox statCardCommits;
    @FXML
    private VBox statCardBranch;
    @FXML
    private VBox statCardMerge;
    @FXML
    private VBox chartCardCommits;
    @FXML
    private VBox chartCardBranches;

    @FXML
    private LineChart<String, Number> commitActivityChart;
    @FXML
    private BarChart<String, Number> branchActivityChart;

    @FXML
    private VBox navDashboard;
    @FXML
    private VBox navRepository;
    @FXML
    private VBox navCommits;
    @FXML
    private VBox navBranches;
    @FXML
    private VBox navMerge;
    @FXML
    private VBox navCommitGraph;
    @FXML
    private VBox navAnalytics;
    @FXML
    private VBox navSettings;
    @FXML
    private VBox navAbout;

    private final RepositoryService repositoryService = new RepositoryService();

    private List<VBox> navItems;
    private List<Node> statCards;
    private List<Node> chartCards;
    private int currentStatsColumns = -1;
    private int currentChartColumns = -1;
    private Page currentPage = Page.DASHBOARD;

    @FXML
    private void initialize() {
        navItems = List.of(
                navDashboard, navRepository, navCommits, navBranches, navMerge,
                navCommitGraph, navAnalytics, navSettings, navAbout
        );
        statCards = List.of(statCardRepository, statCardCommits, statCardBranch, statCardMerge);
        chartCards = List.of(chartCardCommits, chartCardBranches);

        if (repositoryPageController != null) {
            repositoryPageController.setStatusReporter(message -> statusMessageLabel.setText(message));
        }
        if (commitPageController != null) {
            commitPageController.setStatusReporter(message -> statusMessageLabel.setText(message));
        }
        if (branchPageController != null) {
            branchPageController.setStatusReporter(message -> statusMessageLabel.setText(message));
        }
        if (mergePageController != null) {
            mergePageController.setStatusReporter(message -> statusMessageLabel.setText(message));
        }
        if (commitGraphPageController != null) {
            commitGraphPageController.setStatusReporter(message -> statusMessageLabel.setText(message));
        }

        populatePlaceholderCharts();
        refreshDashboardStats();
        bindResponsiveLayout();
        showDashboard();
    }

    @FXML
    private void onDashboardSelected() {
        showDashboard();
    }

    @FXML
    private void onRepositorySelected() {
        showRepositoryPage();
    }

    @FXML
    private void onCommitsSelected() {
        showCommitPage();
    }

    @FXML
    private void onBranchesSelected() {
        showBranchPage();
    }

    @FXML
    private void onMergeSelected() {
        showMergePage();
    }

    @FXML
    private void onCommitGraphSelected() {
        showCommitGraphPage();
    }

    @FXML
    private void onAnalyticsSelected() {
        showModule(navAnalytics, "Analytics", "mdi2c-chart-bar",
                "Explore repository metrics and activity trends.");
    }

    @FXML
    private void onSettingsSelected() {
        showModule(navSettings, "Settings", "mdi2c-cog-outline",
                "Configure GitForge preferences and appearance.");
    }

    @FXML
    private void onAboutSelected() {
        showModule(navAbout, "About", "mdi2i-information-outline",
                "GitForge — a desktop Git visualizer with simulated operations.");
    }

    @FXML
    private void onRefreshClicked() {
        if (currentPage == Page.REPOSITORY && repositoryPageController != null) {
            repositoryPageController.onPageShown();
            statusMessageLabel.setText("Repository list refreshed");
            return;
        }
        if (currentPage == Page.COMMITS && commitPageController != null) {
            commitPageController.onPageShown();
            statusMessageLabel.setText("Commit history refreshed");
            return;
        }
        if (currentPage == Page.BRANCHES && branchPageController != null) {
            branchPageController.onPageShown();
            statusMessageLabel.setText("Branch list refreshed");
            return;
        }
        if (currentPage == Page.MERGE && mergePageController != null) {
            mergePageController.onPageShown();
            statusMessageLabel.setText("Merge workspace refreshed");
            return;
        }
        if (currentPage == Page.COMMIT_GRAPH && commitGraphPageController != null) {
            commitGraphPageController.onPageShown();
            statusMessageLabel.setText("Commit graph refreshed");
            return;
        }
        if (currentPage == Page.DASHBOARD) {
            refreshDashboardStats();
            statusMessageLabel.setText("Dashboard refreshed");
            return;
        }
        statusMessageLabel.setText("View refreshed");
    }

    @FXML
    private void onNewRepositoryClicked() {
        showRepositoryPage();
        if (repositoryPageController != null) {
            repositoryPageController.openCreateDialog();
        }
    }

    private void showDashboard() {
        selectNav(navDashboard);
        updateChrome("Dashboard", "Simulated repository overview");
        showPage(Page.DASHBOARD);
        refreshDashboardStats();
        statusMessageLabel.setText("Ready");
    }

    private void showRepositoryPage() {
        selectNav(navRepository);
        updateChrome("Repository", "Manage simulated repositories");
        showPage(Page.REPOSITORY);
        if (repositoryPageController != null) {
            repositoryPageController.onPageShown();
        }
        statusMessageLabel.setText("Ready");
    }

    private void showCommitPage() {
        selectNav(navCommits);
        updateChrome("Commits", "Browse simulated commit history");
        showPage(Page.COMMITS);
        if (commitPageController != null) {
            commitPageController.onPageShown();
        }
        statusMessageLabel.setText("Ready");
    }

    private void showBranchPage() {
        selectNav(navBranches);
        updateChrome("Branches", "Manage simulated branches");
        showPage(Page.BRANCHES);
        if (branchPageController != null) {
            branchPageController.onPageShown();
        }
        statusMessageLabel.setText("Ready");
    }

    private void showMergePage() {
        selectNav(navMerge);
        updateChrome("Merge", "Simulate branch merges");
        showPage(Page.MERGE);
        if (mergePageController != null) {
            mergePageController.onPageShown();
        }
        statusMessageLabel.setText("Ready");
    }

    private void showCommitGraphPage() {
        selectNav(navCommitGraph);
        updateChrome("Commit Graph", "Interactive commit DAG");
        showPage(Page.COMMIT_GRAPH);
        if (commitGraphPageController != null) {
            commitGraphPageController.onPageShown();
        }
        statusMessageLabel.setText("Ready");
    }

    private void showModule(VBox navItem, String title, String iconLiteral, String body) {
        selectNav(navItem);
        updateChrome(title, "Module placeholder");
        placeholderTitleLabel.setText(title);
        placeholderBodyLabel.setText(body);
        placeholderIcon.setIconLiteral(iconLiteral);
        showPage(Page.PLACEHOLDER);
        statusMessageLabel.setText("Ready");
    }

    private void updateChrome(String title, String subtitle) {
        toolbarTitleLabel.setText(title);
        toolbarSubtitleLabel.setText(subtitle);
        statusPageLabel.setText(title);
    }

    private void showPage(Page page) {
        currentPage = page;
        setVisible(dashboardPage, page == Page.DASHBOARD);
        setVisible(repositoryPage, page == Page.REPOSITORY);
        setVisible(commitPage, page == Page.COMMITS);
        setVisible(branchPage, page == Page.BRANCHES);
        setVisible(mergePage, page == Page.MERGE);
        setVisible(commitGraphPage, page == Page.COMMIT_GRAPH);
        setVisible(modulePlaceholderPage, page == Page.PLACEHOLDER);
    }

    private static void setVisible(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void selectNav(VBox selected) {
        for (VBox item : navItems) {
            item.getStyleClass().remove(NAV_SELECTED);
        }
        if (selected != null && !selected.getStyleClass().contains(NAV_SELECTED)) {
            selected.getStyleClass().add(NAV_SELECTED);
        }
    }

    private void bindResponsiveLayout() {
        dashboardPage.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            if (newBounds != null && newBounds.getWidth() > 0) {
                applyResponsiveLayout(newBounds.getWidth());
            }
        });

        if (dashboardPage.getViewportBounds() != null && dashboardPage.getViewportBounds().getWidth() > 0) {
            applyResponsiveLayout(dashboardPage.getViewportBounds().getWidth());
        } else {
            applyResponsiveLayout(STATS_TWO_COLUMN_BREAKPOINT + 1);
        }
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
            GridPane.setColumnIndex(node, i % columns);
            GridPane.setRowIndex(node, i / columns);
            GridPane.setHgrow(node, Priority.ALWAYS);
            GridPane.setVgrow(node, Priority.ALWAYS);
            grid.getChildren().add(node);
        }
    }

    private void populatePlaceholderCharts() {
        ObservableList<XYChart.Data<String, Number>> commitPoints = FXCollections.observableArrayList();
        commitPoints.add(new XYChart.Data<>("Mon", 2));
        commitPoints.add(new XYChart.Data<>("Tue", 4));
        commitPoints.add(new XYChart.Data<>("Wed", 3));
        commitPoints.add(new XYChart.Data<>("Thu", 6));
        commitPoints.add(new XYChart.Data<>("Fri", 5));
        commitPoints.add(new XYChart.Data<>("Sat", 1));
        commitPoints.add(new XYChart.Data<>("Sun", 2));

        XYChart.Series<String, Number> commitSeries = new XYChart.Series<>("Commits", commitPoints);
        ObservableList<XYChart.Series<String, Number>> commitChartData = FXCollections.observableArrayList();
        commitChartData.add(commitSeries);
        commitActivityChart.setData(commitChartData);

        ObservableList<XYChart.Data<String, Number>> branchPoints = FXCollections.observableArrayList();
        branchPoints.add(new XYChart.Data<>("main", 8));
        branchPoints.add(new XYChart.Data<>("develop", 5));
        branchPoints.add(new XYChart.Data<>("feature", 3));

        XYChart.Series<String, Number> branchSeries = new XYChart.Series<>("Branches", branchPoints);
        ObservableList<XYChart.Series<String, Number>> branchChartData = FXCollections.observableArrayList();
        branchChartData.add(branchSeries);
        branchActivityChart.setData(branchChartData);
    }

    private void refreshDashboardStats() {
        try {
            int repositoryCount = repositoryService.countRepositories();
            int commitCount = repositoryService.countAllCommits();
            int mergeCount = repositoryService.countAllMerges();
            cardRepositoryValue.setText(Integer.toString(repositoryCount));
            cardCommitsValue.setText(Integer.toString(commitCount));
            cardBranchValue.setText(repositoryCount > 0 ? "main" : "—");
            cardMergeValue.setText(Integer.toString(mergeCount));
        } catch (SQLException ex) {
            cardRepositoryValue.setText("—");
            cardCommitsValue.setText("0");
            cardBranchValue.setText("—");
            cardMergeValue.setText("0");
        }
    }
}
