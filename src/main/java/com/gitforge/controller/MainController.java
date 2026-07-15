package com.gitforge.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/**
 * Controller for the GitForge application shell and page navigation.
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
    private AnalyticsController dashboardPageController;
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

    private List<VBox> navItems;
    private Page currentPage = Page.DASHBOARD;

    @FXML
    private void initialize() {
        navItems = List.of(
                navDashboard, navRepository, navCommits, navBranches, navMerge,
                navCommitGraph, navAnalytics, navSettings, navAbout
        );

        if (dashboardPageController != null) {
            dashboardPageController.setStatusReporter(message -> statusMessageLabel.setText(message));
        }
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
        showDashboard();
        selectNav(navAnalytics);
        updateChrome("Analytics", "Repository analytics dashboard");
        statusPageLabel.setText("Analytics");
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
        if (currentPage == Page.DASHBOARD && dashboardPageController != null) {
            dashboardPageController.onRefreshClicked();
            return;
        }
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
        updateChrome("Dashboard", "Repository analytics overview");
        showPage(Page.DASHBOARD);
        if (dashboardPageController != null) {
            dashboardPageController.onPageShown();
        }
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
}
