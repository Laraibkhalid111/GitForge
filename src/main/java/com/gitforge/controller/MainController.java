package com.gitforge.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Controller for the main application shell.
 * Handles sidebar navigation presentation only.
 */
public class MainController {

    @FXML
    private Label contentTitleLabel;

    @FXML
    private Label contentSubtitleLabel;

    @FXML
    private VBox navDashboard;

    @FXML
    private VBox navRepositories;

    @FXML
    private VBox navCommits;

    @FXML
    private VBox navBranches;

    @FXML
    private VBox navSettings;

    @FXML
    private void initialize() {
        selectNav(navDashboard, "Dashboard",
                "Overview of your simulated repositories. Content will be wired in a later module.");
    }

    @FXML
    private void onDashboardSelected() {
        selectNav(navDashboard, "Dashboard",
                "Overview of your simulated repositories. Content will be wired in a later module.");
    }

    @FXML
    private void onRepositoriesSelected() {
        selectNav(navRepositories, "Repositories",
                "Repository management will be implemented in a later module.");
    }

    @FXML
    private void onCommitsSelected() {
        selectNav(navCommits, "Commits",
                "Commit history visualization will be implemented in a later module.");
    }

    @FXML
    private void onBranchesSelected() {
        selectNav(navBranches, "Branches",
                "Branch graph and switching will be implemented in a later module.");
    }

    @FXML
    private void onSettingsSelected() {
        selectNav(navSettings, "Settings",
                "Application settings will be implemented in a later module.");
    }

    private void selectNav(VBox selected, String title, String subtitle) {
        clearNavSelection();
        if (selected != null) {
            selected.getStyleClass().add("nav-item-selected");
        }
        if (contentTitleLabel != null) {
            contentTitleLabel.setText(title);
        }
        if (contentSubtitleLabel != null) {
            contentSubtitleLabel.setText(subtitle);
        }
    }

    private void clearNavSelection() {
        removeSelected(navDashboard);
        removeSelected(navRepositories);
        removeSelected(navCommits);
        removeSelected(navBranches);
        removeSelected(navSettings);
    }

    private void removeSelected(VBox item) {
        if (item != null) {
            item.getStyleClass().remove("nav-item-selected");
        }
    }
}
