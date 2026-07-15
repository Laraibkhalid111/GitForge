package com.gitforge.controller;

import com.gitforge.model.GraphCommitInfo;
import com.gitforge.model.Repository;
import com.gitforge.service.CommitGraphService;
import com.gitforge.util.CommitGraph;
import com.gitforge.util.DateDisplays;
import com.gitforge.util.GraphEdge;
import com.gitforge.util.GraphNode;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Interactive commit DAG visualization page.
 */
public class CommitGraphController {

    private static final double NODE_RADIUS = 18;
    private static final Color EDGE_COLOR = Color.web("#484f58");
    private static final Color MERGE_EDGE_COLOR = Color.web("#d29922");
    private static final Color HEAD_RING = Color.web("#f0f6fc");
    private static final Color SELECTED_RING = Color.web("#58a6ff");
    private static final Color PATH_EDGE = Color.web("#3fb950");
    private static final Color MONO_NODE = Color.web("#8b949e");

    @FXML
    private ComboBox<Repository> repositoryComboBox;
    @FXML
    private Button colorsButton;
    @FXML
    private HBox legendBox;
    @FXML
    private Label emptyLabel;
    @FXML
    private ScrollPane graphScroll;
    @FXML
    private Pane graphCanvas;

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
    private Label detailTimestampLabel;
    @FXML
    private Label detailBranchLabel;
    @FXML
    private Label detailParentLabel;
    @FXML
    private Label detailMergeLabel;
    @FXML
    private Label detailFilesLabel;

    private final CommitGraphService graphService = new CommitGraphService();
    private final Map<String, StackPane> nodeViews = new HashMap<>();
    private final Map<String, javafx.scene.shape.Shape> edgeViews = new HashMap<>();

    private double scale = 1.0;
    private boolean branchColorsEnabled = true;
    private String selectedHash;
    private Set<String> highlightedPath = Set.of();
    private Consumer<String> statusReporter = message -> {
    };

    private double panStartX;
    private double panStartY;
    private double hScrollStart;
    private double vScrollStart;

    @FXML
    private void initialize() {
        configureRepositoryCombo();
        clearDetails();
        graphScroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                if (event.getDeltaY() > 0) {
                    zoomBy(0.1);
                } else {
                    zoomBy(-0.1);
                }
                event.consume();
            }
        });
        graphCanvas.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY || event.isMiddleButtonDown()) {
                panStartX = event.getSceneX();
                panStartY = event.getSceneY();
                hScrollStart = graphScroll.getHvalue();
                vScrollStart = graphScroll.getVvalue();
            }
        });
        graphCanvas.setOnMouseDragged(event -> {
            if (event.getButton() == MouseButton.SECONDARY || event.isMiddleButtonDown()) {
                double dx = event.getSceneX() - panStartX;
                double dy = event.getSceneY() - panStartY;
                Bounds content = graphCanvas.getBoundsInLocal();
                Bounds viewport = graphScroll.getViewportBounds();
                double hRange = Math.max(1, content.getWidth() - viewport.getWidth());
                double vRange = Math.max(1, content.getHeight() - viewport.getHeight());
                graphScroll.setHvalue(clamp(hScrollStart - dx / hRange, 0, 1));
                graphScroll.setVvalue(clamp(vScrollStart - dy / vRange, 0, 1));
            }
        });
        loadRepositories();
        refreshGraph();
    }

    public void setStatusReporter(Consumer<String> statusReporter) {
        this.statusReporter = statusReporter == null ? message -> {
        } : statusReporter;
    }

    public void onPageShown() {
        loadRepositories();
        refreshGraph();
    }

    @FXML
    private void onRefreshClicked() {
        refreshGraph();
        report("Commit graph refreshed");
    }

    @FXML
    private void onAutoLayoutClicked() {
        graphService.autoLayout();
        redrawCanvas();
        centerGraph();
        report("Auto layout applied");
    }

    @FXML
    private void onZoomInClicked() {
        zoomBy(0.15);
    }

    @FXML
    private void onZoomOutClicked() {
        zoomBy(-0.15);
    }

    @FXML
    private void onCenterClicked() {
        centerGraph();
        report("Graph centered");
    }

    @FXML
    private void onToggleColorsClicked() {
        branchColorsEnabled = !branchColorsEnabled;
        colorsButton.setText(branchColorsEnabled ? "Toggle Colors" : "Colors Off");
        redrawCanvas();
        report(branchColorsEnabled ? "Branch colors enabled" : "Branch colors disabled");
    }

    @FXML
    private void onExportClicked() {
        if (graphService.getGraph().isEmpty()) {
            report("Nothing to export");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Commit Graph");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        chooser.setInitialFileName("gitforge-commit-graph.png");
        File file = chooser.showSaveDialog(graphCanvas.getScene() == null ? null : graphCanvas.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            WritableImage image = graphCanvas.snapshot(null, null);
            PixelReader reader = image.getPixelReader();
            int width = (int) image.getWidth();
            int height = (int) image.getHeight();
            BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    buffered.setRGB(x, y, reader.getArgb(x, y));
                }
            }
            ImageIO.write(buffered, "png", file);
            report("Exported graph to " + file.getName());
        } catch (Exception ex) {
            showError("Unable to export graph", ex.getMessage());
        }
    }

    private void configureRepositoryCombo() {
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
        repositoryComboBox.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> refreshGraph());
    }

    private void loadRepositories() {
        try {
            List<Repository> repositories = graphService.listRepositories();
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
        }
    }

    private void refreshGraph() {
        try {
            Repository selected = repositoryComboBox.getSelectionModel().getSelectedItem();
            Long repositoryId = selected == null ? null : selected.getId();
            graphService.buildGraph(repositoryId);
            selectedHash = null;
            highlightedPath = Set.of();
            clearDetails();
            redrawCanvas();
            boolean empty = graphService.getGraph().isEmpty();
            emptyLabel.setVisible(empty);
            emptyLabel.setManaged(empty);
            if (!empty) {
                Platform.runLater(this::centerGraph);
                animateGraphLoad();
            }
        } catch (SQLException ex) {
            showError("Unable to build commit graph", ex.getMessage());
        }
    }

    private void redrawCanvas() {
        graphCanvas.getChildren().clear();
        nodeViews.clear();
        edgeViews.clear();
        legendBox.getChildren().clear();

        CommitGraph<GraphCommitInfo> graph = graphService.getGraph();
        if (graph.isEmpty()) {
            graphCanvas.setPrefSize(800, 500);
            return;
        }

        for (Map.Entry<String, String> entry : graphService.getBranchColorHex().entrySet()) {
            Circle swatch = new Circle(6, Color.web(entry.getValue()));
            Label name = new Label(entry.getKey());
            name.getStyleClass().add("graph-legend-label");
            HBox item = new HBox(6, swatch, name);
            item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            legendBox.getChildren().add(item);
        }

        for (GraphEdge edge : graph.getEdges()) {
            GraphNode<GraphCommitInfo> from = graph.findNode(edge.getFromId()).orElse(null);
            GraphNode<GraphCommitInfo> to = graph.findNode(edge.getToId()).orElse(null);
            if (from == null || to == null) {
                continue;
            }
            javafx.scene.shape.Shape shape = createEdgeShape(from, to, edge);
            String edgeKey = edge.getFromId() + "->" + edge.getToId() + ":" + edge.getType();
            edgeViews.put(edgeKey, shape);
            graphCanvas.getChildren().add(shape);
        }

        double maxX = 0;
        double maxY = 0;
        for (GraphNode<GraphCommitInfo> node : graph.getNodes()) {
            StackPane view = createNodeView(node);
            nodeViews.put(node.getId(), view);
            graphCanvas.getChildren().add(view);
            maxX = Math.max(maxX, node.getX() + 80);
            maxY = Math.max(maxY, node.getY() + 80);
        }

        graphCanvas.setPrefSize(Math.max(800, maxX * scale + 120), Math.max(500, maxY * scale + 120));
        applyScale();
        refreshHighlightStyles();
    }

    private javafx.scene.shape.Shape createEdgeShape(GraphNode<GraphCommitInfo> from,
                                                      GraphNode<GraphCommitInfo> to,
                                                      GraphEdge edge) {
        double x1 = from.getX();
        double y1 = from.getY() + NODE_RADIUS;
        double x2 = to.getX();
        double y2 = to.getY() - NODE_RADIUS;

        boolean curved = Math.abs(x1 - x2) > 8 || edge.isMergeEdge();
        if (curved) {
            CubicCurve curve = new CubicCurve();
            curve.setStartX(x1);
            curve.setStartY(y1);
            curve.setEndX(x2);
            curve.setEndY(y2);
            double midY = (y1 + y2) / 2;
            curve.setControlX1(x1);
            curve.setControlY1(midY);
            curve.setControlX2(x2);
            curve.setControlY2(midY);
            styleEdge(curve, edge);
            return curve;
        }

        Line line = new Line(x1, y1, x2, y2);
        styleEdge(line, edge);
        return line;
    }

    private void styleEdge(javafx.scene.shape.Shape shape, GraphEdge edge) {
        shape.setStroke(edge.isMergeEdge() ? MERGE_EDGE_COLOR : EDGE_COLOR);
        shape.setStrokeWidth(edge.isMergeEdge() ? 2.4 : 1.8);
        shape.setFill(null);
        shape.getStrokeDashArray().clear();
        if (edge.isMergeEdge()) {
            shape.getStrokeDashArray().addAll(8.0, 6.0);
        }
        if (shape instanceof Line line) {
            line.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        shape.setMouseTransparent(true);
    }

    private StackPane createNodeView(GraphNode<GraphCommitInfo> node) {
        GraphCommitInfo info = node.getData();
        Color fill = resolveNodeColor(info);

        Circle circle = new Circle(NODE_RADIUS, fill);
        circle.setStroke(info.isHead() ? HEAD_RING : Color.web("#21262d"));
        circle.setStrokeWidth(info.isHead() ? 3.2 : 1.5);

        Text hashText = new Text(info.getShortHash());
        hashText.setFill(Color.web("#0d1117"));
        hashText.setFont(Font.font("Consolas", FontWeight.BOLD, 9));

        Label branchLabel = new Label(info.getBranchName());
        branchLabel.setTextFill(Color.web("#c9d1d9"));
        branchLabel.setStyle("-fx-font-size: 10px;");

        VBoxWithPadding content = new VBoxWithPadding(circle, hashText);
        StackPane stack = new StackPane(content);
        stack.setTranslateX(node.getX() - NODE_RADIUS - 4);
        stack.setTranslateY(node.getY() - NODE_RADIUS - 4);
        stack.setCursor(Cursor.HAND);

        String tooltipText = info.getShortHash() + " · " + info.getBranchName()
                + "\n" + nullToDash(info.getMessage())
                + (info.isHead() ? "\nHEAD" : "")
                + (info.isMergeCommit() ? "\nMerge commit" : "");
        Tooltip.install(stack, new Tooltip(tooltipText));

        stack.setOnMouseEntered(event -> {
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(140), stack);
            scaleUp.setToX(1.12);
            scaleUp.setToY(1.12);
            scaleUp.play();
        });
        stack.setOnMouseExited(event -> {
            if (!Objects.equals(selectedHash, info.getHash())) {
                ScaleTransition scaleDown = new ScaleTransition(Duration.millis(140), stack);
                scaleDown.setToX(1.0);
                scaleDown.setToY(1.0);
                scaleDown.play();
            }
        });
        stack.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                selectNode(info.getHash());
            }
        });

        Label underLabel = new Label(info.getBranchName());
        underLabel.setTextFill(Color.web("#8b949e"));
        underLabel.setStyle("-fx-font-size: 10px;");
        underLabel.setTranslateX(node.getX() - 28);
        underLabel.setTranslateY(node.getY() + NODE_RADIUS + 8);
        underLabel.setMouseTransparent(true);
        graphCanvas.getChildren().add(underLabel);

        if (info.isHead()) {
            Label headBadge = new Label("HEAD");
            headBadge.setTextFill(Color.web("#0d1117"));
            headBadge.setStyle("-fx-background-color: #f0f6fc; -fx-background-radius: 4; -fx-padding: 1 4 1 4; -fx-font-size: 9px; -fx-font-weight: bold;");
            headBadge.setTranslateX(node.getX() + NODE_RADIUS);
            headBadge.setTranslateY(node.getY() - NODE_RADIUS - 12);
            headBadge.setMouseTransparent(true);
            graphCanvas.getChildren().add(headBadge);
        }

        return stack;
    }

    private Color resolveNodeColor(GraphCommitInfo info) {
        if (!branchColorsEnabled) {
            return MONO_NODE;
        }
        String hex = graphService.getBranchColorHex().get(info.getBranchName());
        return hex == null ? MONO_NODE : Color.web(hex);
    }

    private void selectNode(String hash) {
        selectedHash = hash;
        GraphCommitInfo info = graphService.findCommit(hash).orElse(null);
        if (info == null) {
            clearDetails();
            return;
        }
        showDetails(info);

        String head = graphService.getHeadHash();
        if (head != null && !head.equals(hash)) {
            highlightedPath = graphService.findPath(hash, head)
                    .map(Set::copyOf)
                    .orElseGet(() -> graphService.findPath(head, hash).map(Set::copyOf).orElse(Set.of(hash)));
        } else {
            highlightedPath = Set.of(hash);
        }
        refreshHighlightStyles();
        report("Selected " + info.getShortHash());
    }

    private void refreshHighlightStyles() {
        for (Map.Entry<String, StackPane> entry : nodeViews.entrySet()) {
            StackPane view = entry.getValue();
            boolean selected = Objects.equals(entry.getKey(), selectedHash);
            boolean onPath = highlightedPath.contains(entry.getKey());
            Circle circle = findCircle(view);
            if (circle != null) {
                GraphCommitInfo info = graphService.findCommit(entry.getKey()).orElse(null);
                if (selected) {
                    circle.setStroke(SELECTED_RING);
                    circle.setStrokeWidth(3.5);
                    view.setScaleX(1.12);
                    view.setScaleY(1.12);
                } else if (info != null && info.isHead()) {
                    circle.setStroke(HEAD_RING);
                    circle.setStrokeWidth(3.2);
                    view.setScaleX(1.0);
                    view.setScaleY(1.0);
                } else if (onPath) {
                    circle.setStroke(PATH_EDGE);
                    circle.setStrokeWidth(2.8);
                    view.setScaleX(1.0);
                    view.setScaleY(1.0);
                } else {
                    circle.setStroke(Color.web("#21262d"));
                    circle.setStrokeWidth(1.5);
                    view.setScaleX(1.0);
                    view.setScaleY(1.0);
                }
            }
        }

        for (Map.Entry<String, javafx.scene.shape.Shape> entry : edgeViews.entrySet()) {
            String key = entry.getKey();
            String from = key.substring(0, key.indexOf("->"));
            String rest = key.substring(key.indexOf("->") + 2);
            String to = rest.substring(0, rest.indexOf(':'));
            boolean onPath = highlightedPath.contains(from) && highlightedPath.contains(to);
            javafx.scene.shape.Shape shape = entry.getValue();
            if (onPath) {
                shape.setStroke(PATH_EDGE);
                shape.setStrokeWidth(3.0);
            } else if (key.endsWith("MERGE")) {
                shape.setStroke(MERGE_EDGE_COLOR);
                shape.setStrokeWidth(2.4);
            } else {
                shape.setStroke(EDGE_COLOR);
                shape.setStrokeWidth(1.8);
            }
        }
    }

    private Circle findCircle(StackPane view) {
        if (view.getChildren().isEmpty()) {
            return null;
        }
        javafx.scene.Node child = view.getChildren().getFirst();
        if (child instanceof VBoxWithPadding box && !box.getChildren().isEmpty()
                && box.getChildren().getFirst() instanceof Circle circle) {
            return circle;
        }
        return null;
    }

    private void showDetails(GraphCommitInfo info) {
        detailsPlaceholderLabel.setVisible(false);
        detailsPlaceholderLabel.setManaged(false);
        detailsGrid.setVisible(true);
        detailsGrid.setManaged(true);

        detailHashLabel.setText(info.getHash());
        detailMessageLabel.setText(nullToDash(info.getMessage()));
        detailAuthorLabel.setText(nullToDash(info.getAuthor()));
        detailTimestampLabel.setText(DateDisplays.formatDateTime(info.getCommittedAt()));
        detailBranchLabel.setText(info.getBranchName() + (info.isHead() ? " (HEAD)" : ""));
        detailParentLabel.setText(info.getPrimaryParentHash() == null ? "—" : info.getPrimaryParentHash());
        detailMergeLabel.setText(info.isMergeCommit()
                ? (info.getSecondaryParentHash() == null ? "Yes" : info.getSecondaryParentHash())
                : "No");
        detailFilesLabel.setText(Integer.toString(info.getFilesChanged()));
    }

    private void clearDetails() {
        detailsPlaceholderLabel.setVisible(true);
        detailsPlaceholderLabel.setManaged(true);
        detailsGrid.setVisible(false);
        detailsGrid.setManaged(false);
    }

    private void zoomBy(double delta) {
        scale = clamp(scale + delta, 0.5, 2.4);
        applyScale();
        report(String.format("Zoom %.0f%%", scale * 100));
    }

    private void applyScale() {
        graphCanvas.setScaleX(scale);
        graphCanvas.setScaleY(scale);
    }

    private void centerGraph() {
        graphScroll.setHvalue(0.5);
        graphScroll.setVvalue(0.0);
        if (selectedHash != null && nodeViews.containsKey(selectedHash)) {
            StackPane view = nodeViews.get(selectedHash);
            Point2D local = new Point2D(view.getTranslateX(), view.getTranslateY());
            Bounds content = graphCanvas.getBoundsInLocal();
            if (content.getWidth() > 0) {
                graphScroll.setHvalue(clamp(local.getX() / content.getWidth(), 0, 1));
            }
            if (content.getHeight() > 0) {
                graphScroll.setVvalue(clamp(local.getY() / content.getHeight(), 0, 1));
            }
        }
    }

    private void animateGraphLoad() {
        FadeTransition fade = new FadeTransition(Duration.millis(320), graphCanvas);
        fade.setFromValue(0.2);
        fade.setToValue(1.0);
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

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    /**
     * Tiny layout helper so node circle + hash stay centered together.
     */
    private static final class VBoxWithPadding extends javafx.scene.layout.VBox {
        VBoxWithPadding(javafx.scene.Node... children) {
            super(2, children);
            setAlignment(javafx.geometry.Pos.CENTER);
            setPadding(new Insets(4));
            setFillWidth(true);
        }
    }
}
