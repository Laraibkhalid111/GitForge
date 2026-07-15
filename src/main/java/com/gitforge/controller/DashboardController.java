package com.gitforge.controller;

import com.gitforge.model.AnalyticsSnapshot;
import com.gitforge.model.AnalyticsSnapshot.ActivityItem;
import com.gitforge.model.AnalyticsSnapshot.Filter;
import com.gitforge.model.GraphCommitInfo;
import com.gitforge.model.RepositorySummary;
import com.gitforge.service.AnalyticsService;
import com.gitforge.service.CommitGraphService;
import com.gitforge.service.RepositoryService;
import com.gitforge.util.AppInfo;
import com.gitforge.util.CommitGraph;
import com.gitforge.util.DateDisplays;
import com.gitforge.util.GraphEdge;
import com.gitforge.util.GraphNode;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Operational home dashboard — welcome, shortcuts, status, activity, and graph preview.
 * Analytics charts and summary statistics live on the Analytics page.
 */
public class DashboardController {

    private static final int RECENT_REPO_LIMIT = 6;
    private static final int ACTIVITY_LIMIT = 12;
    private static final double PREVIEW_SCALE = 0.55;

    @FXML
    private Label welcomeTitleLabel;
    @FXML
    private Label welcomeSubtitleLabel;
    @FXML
    private ListView<RepositorySummary> recentRepositoriesList;
    @FXML
    private Label recentReposEmptyLabel;
    @FXML
    private Label statusPlaceholderLabel;
    @FXML
    private GridPane statusGrid;
    @FXML
    private Label statusNameLabel;
    @FXML
    private Label statusBranchLabel;
    @FXML
    private Label statusCommitsLabel;
    @FXML
    private Label statusBranchesLabel;
    @FXML
    private Label statusStateLabel;
    @FXML
    private Label statusUpdatedLabel;
    @FXML
    private ListView<ActivityItem> recentActivityList;
    @FXML
    private Label activityEmptyLabel;
    @FXML
    private Pane miniGraphCanvas;
    @FXML
    private Label graphEmptyLabel;
    @FXML
    private Label graphPreviewHintLabel;

    private final RepositoryService repositoryService = new RepositoryService();
    private final AnalyticsService analyticsService = new AnalyticsService();
    private final CommitGraphService commitGraphService = new CommitGraphService();

    private final ObservableList<RepositorySummary> recentRepos = FXCollections.observableArrayList();
    private final ObservableList<ActivityItem> activityItems = FXCollections.observableArrayList();

    private Consumer<String> statusReporter = message -> {
    };
    private Runnable newRepositoryAction = () -> {
    };
    private Runnable openRepositoriesAction = () -> {
    };
    private Runnable openCommitsAction = () -> {
    };
    private Runnable openBranchesAction = () -> {
    };
    private Runnable openGraphAction = () -> {
    };
    private Runnable openAnalyticsAction = () -> {
    };

    @FXML
    private void initialize() {
        welcomeTitleLabel.setText("Welcome to " + AppInfo.APP_NAME);
        welcomeSubtitleLabel.setText(AppInfo.APP_TAGLINE
                + " — operational control center for your simulated workspaces.");

        recentRepositoriesList.setItems(recentRepos);
        recentRepositoriesList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(RepositorySummary item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                FontIcon icon = new FontIcon("mdi2s-source-repository");
                icon.setIconSize(16);
                icon.getStyleClass().add("activity-icon");
                Label title = new Label(item.getName());
                title.getStyleClass().add("activity-title");
                Label meta = new Label(nullToDash(item.getCurrentBranch())
                        + " · " + item.getTotalCommits() + " commits · "
                        + nullToDash(item.getStatus()));
                meta.getStyleClass().add("activity-meta");
                VBox text = new VBox(2, title, meta);
                HBox row = new HBox(10, icon, text);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
        recentRepositoriesList.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            if (selected != null) {
                showStatus(selected);
                drawMiniGraph(selected.getId());
            }
        });

        recentActivityList.setItems(activityItems);
        recentActivityList.setCellFactory(list -> new ListCell<>() {
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
                Label meta = new Label(item.getType() + " · " + DateDisplays.formatDateTime(item.getTimestamp()));
                meta.getStyleClass().add("activity-meta");
                VBox text = new VBox(2, title, meta);
                HBox row = new HBox(10, icon, text);
                row.setAlignment(Pos.TOP_LEFT);
                setGraphic(row);
                setText(null);
            }
        });

        clearStatus();
        refreshHome();
    }

    public void setStatusReporter(Consumer<String> statusReporter) {
        this.statusReporter = statusReporter == null ? message -> {
        } : statusReporter;
    }

    public void setQuickActions(Runnable newRepository,
                                Runnable openRepositories,
                                Runnable openCommits,
                                Runnable openBranches,
                                Runnable openGraph,
                                Runnable openAnalytics) {
        this.newRepositoryAction = safe(newRepository);
        this.openRepositoriesAction = safe(openRepositories);
        this.openCommitsAction = safe(openCommits);
        this.openBranchesAction = safe(openBranches);
        this.openGraphAction = safe(openGraph);
        this.openAnalyticsAction = safe(openAnalytics);
    }

    public void onPageShown() {
        refreshHome();
    }

    public void onRefreshClicked() {
        refreshHome();
        statusReporter.accept("Dashboard refreshed");
    }

    @FXML
    private void onQuickNewRepository() {
        newRepositoryAction.run();
    }

    @FXML
    private void onQuickOpenRepositories() {
        openRepositoriesAction.run();
    }

    @FXML
    private void onQuickOpenCommits() {
        openCommitsAction.run();
    }

    @FXML
    private void onQuickOpenBranches() {
        openBranchesAction.run();
    }

    @FXML
    private void onQuickOpenGraph() {
        openGraphAction.run();
    }

    @FXML
    private void onQuickOpenAnalytics() {
        openAnalyticsAction.run();
    }

    private void refreshHome() {
        try {
            List<RepositorySummary> summaries = repositoryService.listSummaries().stream()
                    .sorted(Comparator.comparing(RepositorySummary::getUpdatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(RECENT_REPO_LIMIT)
                    .toList();
            recentRepos.setAll(summaries);
            boolean emptyRepos = summaries.isEmpty();
            recentReposEmptyLabel.setVisible(emptyRepos);
            recentReposEmptyLabel.setManaged(emptyRepos);
            recentRepositoriesList.setVisible(!emptyRepos);
            recentRepositoriesList.setManaged(!emptyRepos);

            if (!summaries.isEmpty()) {
                RepositorySummary selected = recentRepositoriesList.getSelectionModel().getSelectedItem();
                if (selected == null || summaries.stream().noneMatch(s -> s.getId().equals(selected.getId()))) {
                    recentRepositoriesList.getSelectionModel().selectFirst();
                } else {
                    showStatus(selected);
                    drawMiniGraph(selected.getId());
                }
            } else {
                clearStatus();
                clearMiniGraph();
            }

            AnalyticsSnapshot snapshot = analyticsService.loadDashboard(new Filter(), false);
            List<ActivityItem> activity = snapshot.getRecentActivity().stream()
                    .limit(ACTIVITY_LIMIT)
                    .toList();
            activityItems.setAll(activity);
            boolean emptyActivity = activity.isEmpty();
            activityEmptyLabel.setVisible(emptyActivity);
            activityEmptyLabel.setManaged(emptyActivity);
            recentActivityList.setVisible(!emptyActivity);
            recentActivityList.setManaged(!emptyActivity);
        } catch (SQLException ex) {
            statusReporter.accept("Unable to load dashboard: " + ex.getMessage());
        }
    }

    private void showStatus(RepositorySummary summary) {
        statusPlaceholderLabel.setVisible(false);
        statusPlaceholderLabel.setManaged(false);
        statusGrid.setVisible(true);
        statusGrid.setManaged(true);
        statusNameLabel.setText(nullToDash(summary.getName()));
        statusBranchLabel.setText(nullToDash(summary.getCurrentBranch()));
        statusCommitsLabel.setText(Integer.toString(summary.getTotalCommits()));
        statusBranchesLabel.setText(Integer.toString(summary.getTotalBranches()));
        statusStateLabel.setText(nullToDash(summary.getStatus()));
        statusUpdatedLabel.setText(DateDisplays.formatDateTime(summary.getUpdatedAt()));
        graphPreviewHintLabel.setText("Preview · " + summary.getName());
    }

    private void clearStatus() {
        statusPlaceholderLabel.setVisible(true);
        statusPlaceholderLabel.setManaged(true);
        statusGrid.setVisible(false);
        statusGrid.setManaged(false);
        graphPreviewHintLabel.setText("Preview of the active repository DAG");
    }

    private void drawMiniGraph(Long repositoryId) {
        miniGraphCanvas.getChildren().clear();
        try {
            CommitGraph<GraphCommitInfo> graph = commitGraphService.buildGraph(repositoryId);
            if (graph.isEmpty()) {
                clearMiniGraph();
                return;
            }
            graphEmptyLabel.setVisible(false);
            graphEmptyLabel.setManaged(false);

            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = 0;
            double maxY = 0;
            for (GraphNode<GraphCommitInfo> node : graph.getNodes()) {
                minX = Math.min(minX, node.getX());
                minY = Math.min(minY, node.getY());
                maxX = Math.max(maxX, node.getX());
                maxY = Math.max(maxY, node.getY());
            }

            for (GraphEdge edge : graph.getEdges()) {
                GraphNode<GraphCommitInfo> from = graph.findNode(edge.getFromId()).orElse(null);
                GraphNode<GraphCommitInfo> to = graph.findNode(edge.getToId()).orElse(null);
                if (from == null || to == null) {
                    continue;
                }
                Line line = new Line(
                        (from.getX() - minX) * PREVIEW_SCALE + 28,
                        (from.getY() - minY) * PREVIEW_SCALE + 24,
                        (to.getX() - minX) * PREVIEW_SCALE + 28,
                        (to.getY() - minY) * PREVIEW_SCALE + 24
                );
                line.setStroke(edge.isMergeEdge() ? Color.web("#d29922") : Color.web("#484f58"));
                line.setStrokeWidth(edge.isMergeEdge() ? 2.0 : 1.5);
                line.setMouseTransparent(true);
                miniGraphCanvas.getChildren().add(line);
            }

            for (GraphNode<GraphCommitInfo> node : graph.getNodes()) {
                double x = (node.getX() - minX) * PREVIEW_SCALE + 28;
                double y = (node.getY() - minY) * PREVIEW_SCALE + 24;
                Circle circle = new Circle(x, y, 9, Color.web("#58a6ff"));
                circle.setStroke(node.getData().isHead() ? Color.web("#f0f6fc") : Color.web("#21262d"));
                circle.setStrokeWidth(node.getData().isHead() ? 2.4 : 1.2);
                String shortHash = node.getData().getShortHash();
                String label = shortHash == null || shortHash.isBlank()
                        ? "?"
                        : shortHash.substring(0, Math.min(4, shortHash.length()));
                Text hash = new Text(x - 10, y + 3, label);
                hash.setFill(Color.web("#0d1117"));
                hash.setFont(Font.font("Consolas", FontWeight.BOLD, 7));
                hash.setMouseTransparent(true);
                miniGraphCanvas.getChildren().addAll(circle, hash);
            }

            miniGraphCanvas.setPrefSize(
                    Math.max(280, (maxX - minX) * PREVIEW_SCALE + 64),
                    Math.max(200, (maxY - minY) * PREVIEW_SCALE + 56)
            );
        } catch (SQLException ex) {
            clearMiniGraph();
            statusReporter.accept("Unable to preview commit graph: " + ex.getMessage());
        }
    }

    private void clearMiniGraph() {
        miniGraphCanvas.getChildren().clear();
        graphEmptyLabel.setVisible(true);
        graphEmptyLabel.setManaged(true);
    }

    private static Runnable safe(Runnable action) {
        return action == null ? () -> {
        } : action;
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
