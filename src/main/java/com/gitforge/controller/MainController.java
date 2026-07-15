package com.gitforge.controller;

import com.gitforge.util.AppInfo;
import com.gitforge.util.ThemeManager;
import com.gitforge.util.UiNotifications;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/**
 * Controller for the GitForge application shell and page navigation.
 */
public class MainController {

    private enum Page {
        DASHBOARD,
        ANALYTICS,
        REPOSITORY,
        COMMITS,
        BRANCHES,
        MERGE,
        COMMIT_GRAPH,
        SETTINGS,
        PLACEHOLDER
    }

    private static final String NAV_SELECTED = "nav-item-selected";

    @FXML
    private BorderPane rootPane;
    @FXML
    private StackPane notificationHost;
    @FXML
    private StackPane loadingOverlay;
    @FXML
    private MFXProgressSpinner loadingSpinner;
    @FXML
    private Label toolbarTitleLabel;
    @FXML
    private Label toolbarSubtitleLabel;
    @FXML
    private Label statusMessageLabel;
    @FXML
    private Label statusPageLabel;
    @FXML
    private Label statusVersionLabel;

    @FXML
    private ScrollPane dashboardPage;
    @FXML
    private DashboardController dashboardPageController;
    @FXML
    private ScrollPane analyticsPage;
    @FXML
    private AnalyticsController analyticsPageController;
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
    private ScrollPane settingsPage;
    @FXML
    private SettingsController settingsPageController;
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

        UiNotifications.bind(notificationHost);
        statusVersionLabel.setText(AppInfo.displayVersion());
        setLoadingVisible(false);

        wireStatusReporters();
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                installKeyboardShortcuts(newScene);
            }
        });

        showDashboard();
    }

    private void wireStatusReporters() {
        if (dashboardPageController != null) {
            dashboardPageController.setStatusReporter(this::reportStatus);
            dashboardPageController.setQuickActions(
                    this::onNewRepositoryClicked,
                    this::showRepositoryPage,
                    this::showCommitPage,
                    this::showBranchPage,
                    this::showCommitGraphPage,
                    this::showAnalyticsPage
            );
        }
        if (analyticsPageController != null) {
            analyticsPageController.setStatusReporter(this::reportStatus);
        }
        if (repositoryPageController != null) {
            repositoryPageController.setStatusReporter(this::reportStatus);
        }
        if (commitPageController != null) {
            commitPageController.setStatusReporter(this::reportStatus);
        }
        if (branchPageController != null) {
            branchPageController.setStatusReporter(this::reportStatus);
            branchPageController.setMergeNavigator(this::showMergePage);
        }
        if (mergePageController != null) {
            mergePageController.setStatusReporter(this::reportStatus);
        }
        if (commitGraphPageController != null) {
            commitGraphPageController.setStatusReporter(this::reportStatus);
        }
        if (settingsPageController != null) {
            settingsPageController.setStatusReporter(this::reportStatus);
            settingsPageController.setThemeApplier(this::applyPreferences);
            settingsPageController.setAboutOpener(this::openAboutDialog);
        }
    }

    private void installKeyboardShortcuts(javafx.scene.Scene scene) {
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN),
                this::showDashboard
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN),
                this::showRepositoryPage
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.CONTROL_DOWN),
                this::showCommitPage
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.CONTROL_DOWN),
                this::showBranchPage
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT5, KeyCombination.CONTROL_DOWN),
                this::showMergePage
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT6, KeyCombination.CONTROL_DOWN),
                this::showCommitGraphPage
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT7, KeyCombination.CONTROL_DOWN),
                this::showAnalyticsPage
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN),
                this::onRefreshClicked
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F5),
                this::onRefreshClicked
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN),
                this::onNewRepositoryClicked
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN),
                this::onSettingsSelected
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F1),
                this::onAboutSelected
        );
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
        showAnalyticsPage();
    }

    @FXML
    private void onSettingsSelected() {
        showSettingsPage();
    }

    @FXML
    private void onAboutSelected() {
        selectNav(navAbout);
        openAboutDialog();
    }

    @FXML
    private void onRefreshClicked() {
        setLoadingVisible(true);
        try {
            if (currentPage == Page.DASHBOARD && dashboardPageController != null) {
                dashboardPageController.onRefreshClicked();
                return;
            }
            if (currentPage == Page.ANALYTICS && analyticsPageController != null) {
                analyticsPageController.onRefreshClicked();
                return;
            }
            if (currentPage == Page.REPOSITORY && repositoryPageController != null) {
                repositoryPageController.onPageShown();
                reportStatus("Repository list refreshed");
                return;
            }
            if (currentPage == Page.COMMITS && commitPageController != null) {
                commitPageController.onPageShown();
                reportStatus("Commit history refreshed");
                return;
            }
            if (currentPage == Page.BRANCHES && branchPageController != null) {
                branchPageController.onPageShown();
                reportStatus("Branch list refreshed");
                return;
            }
            if (currentPage == Page.MERGE && mergePageController != null) {
                mergePageController.onPageShown();
                reportStatus("Merge workspace refreshed");
                return;
            }
            if (currentPage == Page.COMMIT_GRAPH && commitGraphPageController != null) {
                commitGraphPageController.onPageShown();
                reportStatus("Commit graph refreshed");
                return;
            }
            if (currentPage == Page.SETTINGS && settingsPageController != null) {
                settingsPageController.onPageShown();
                reportStatus("Settings refreshed");
                return;
            }
            reportStatus("View refreshed");
        } finally {
            setLoadingVisible(false);
        }
    }

    @FXML
    private void onNewRepositoryClicked() {
        showRepositoryPage();
        if (repositoryPageController != null) {
            repositoryPageController.openCreateDialog();
        }
    }

    @FXML
    private void onMinimizeClicked() {
        if (rootPane.getScene() != null && rootPane.getScene().getWindow() instanceof javafx.stage.Stage stage) {
            stage.setIconified(true);
        }
    }

    @FXML
    private void onCloseClicked() {
        if (rootPane.getScene() != null && rootPane.getScene().getWindow() instanceof javafx.stage.Stage stage) {
            stage.close();
        }
    }

    private void showDashboard() {
        selectNav(navDashboard);
        updateChrome("Dashboard", "Operational home and quick actions");
        showPage(Page.DASHBOARD);
        if (dashboardPageController != null) {
            dashboardPageController.onPageShown();
        }
        reportStatus("Ready");
    }

    private void showAnalyticsPage() {
        selectNav(navAnalytics);
        updateChrome("Analytics", "Repository statistics and visualizations");
        showPage(Page.ANALYTICS);
        if (analyticsPageController != null) {
            analyticsPageController.onPageShown();
        }
        reportStatus("Ready");
    }

    private void showRepositoryPage() {
        selectNav(navRepository);
        updateChrome("Repository", "Manage simulated repositories");
        showPage(Page.REPOSITORY);
        if (repositoryPageController != null) {
            repositoryPageController.onPageShown();
        }
        reportStatus("Ready");
    }

    private void showCommitPage() {
        selectNav(navCommits);
        updateChrome("Commits", "Browse simulated commit history");
        showPage(Page.COMMITS);
        if (commitPageController != null) {
            commitPageController.onPageShown();
        }
        reportStatus("Ready");
    }

    private void showBranchPage() {
        selectNav(navBranches);
        updateChrome("Branches", "Manage simulated branches");
        showPage(Page.BRANCHES);
        if (branchPageController != null) {
            branchPageController.onPageShown();
        }
        reportStatus("Ready");
    }

    private void showMergePage() {
        selectNav(navMerge);
        updateChrome("Merge", "Simulate branch merges");
        showPage(Page.MERGE);
        if (mergePageController != null) {
            mergePageController.onPageShown();
        }
        reportStatus("Ready");
    }

    private void showCommitGraphPage() {
        selectNav(navCommitGraph);
        updateChrome("Commit Graph", "Interactive commit DAG");
        showPage(Page.COMMIT_GRAPH);
        if (commitGraphPageController != null) {
            commitGraphPageController.onPageShown();
        }
        reportStatus("Ready");
    }

    private void showSettingsPage() {
        selectNav(navSettings);
        updateChrome("Settings", "Application preferences");
        showPage(Page.SETTINGS);
        if (settingsPageController != null) {
            settingsPageController.onPageShown();
        }
        reportStatus("Ready");
    }

    private void openAboutDialog() {
        if (rootPane.getScene() != null) {
            AboutDialogController.show(rootPane.getScene().getWindow());
        }
    }

    private void applyPreferences(com.gitforge.service.SettingsService.Preferences preferences) {
        if (rootPane.getScene() == null || preferences == null) {
            return;
        }
        ThemeManager.apply(rootPane.getScene(), preferences.getTheme(), preferences.getFontSize());
        statusVersionLabel.setText(AppInfo.displayVersion());
    }

    private void showModule(VBox navItem, String title, String iconLiteral, String body) {
        selectNav(navItem);
        updateChrome(title, "Application information");
        placeholderTitleLabel.setText(title);
        placeholderBodyLabel.setText(body);
        placeholderIcon.setIconLiteral(iconLiteral);
        showPage(Page.PLACEHOLDER);
        reportStatus("Ready");
    }

    private void updateChrome(String title, String subtitle) {
        toolbarTitleLabel.setText(title);
        toolbarSubtitleLabel.setText(subtitle);
        statusPageLabel.setText(title);
    }

    private void showPage(Page page) {
        currentPage = page;
        Node visible = resolvePageNode(page);
        setVisible(dashboardPage, page == Page.DASHBOARD);
        setVisible(analyticsPage, page == Page.ANALYTICS);
        setVisible(repositoryPage, page == Page.REPOSITORY);
        setVisible(commitPage, page == Page.COMMITS);
        setVisible(branchPage, page == Page.BRANCHES);
        setVisible(mergePage, page == Page.MERGE);
        setVisible(commitGraphPage, page == Page.COMMIT_GRAPH);
        setVisible(settingsPage, page == Page.SETTINGS);
        setVisible(modulePlaceholderPage, page == Page.PLACEHOLDER);
        if (visible != null) {
            fadeInPage(visible);
        }
    }

    private Node resolvePageNode(Page page) {
        return switch (page) {
            case DASHBOARD -> dashboardPage;
            case ANALYTICS -> analyticsPage;
            case REPOSITORY -> repositoryPage;
            case COMMITS -> commitPage;
            case BRANCHES -> branchPage;
            case MERGE -> mergePage;
            case COMMIT_GRAPH -> commitGraphPage;
            case SETTINGS -> settingsPage;
            case PLACEHOLDER -> modulePlaceholderPage;
        };
    }

    private static void fadeInPage(Node node) {
        node.setOpacity(0.35);
        FadeTransition fade = new FadeTransition(Duration.millis(180), node);
        fade.setFromValue(0.35);
        fade.setToValue(1.0);
        fade.play();
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

    private void reportStatus(String message) {
        statusMessageLabel.setText(message);
        if (message != null && looksLikeSuccess(message)) {
            UiNotifications.success(message);
        }
    }

    private static boolean looksLikeSuccess(String message) {
        String lower = message.toLowerCase();
        return lower.contains("created")
                || lower.contains("updated")
                || lower.contains("deleted")
                || lower.contains("merged")
                || lower.contains("exported")
                || lower.contains("renamed")
                || lower.contains("switched");
    }

    private void setLoadingVisible(boolean visible) {
        if (loadingOverlay == null) {
            return;
        }
        loadingOverlay.setVisible(visible);
        loadingOverlay.setManaged(visible);
        if (loadingSpinner != null) {
            loadingSpinner.setVisible(visible);
        }
    }
}
