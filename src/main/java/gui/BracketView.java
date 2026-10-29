package gui;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import java.util.List;

public class BracketView extends StackPane {

    public static final String CHAMPION_PATH = "championPath";

    private static final int BODY_ROWS = 8;
    private static final double ROW_HEIGHT = 68;
    private static final String[] HEADER_TITLES = {"QUARTER FINALS", "SEMIFINALS", "FINAL"};

    private final GridPane grid = new GridPane();
    private final Pane linesLayer = new Pane();
    private final Label[] headers = new Label[HEADER_TITLES.length];

    private List<Region> quarters = List.of();
    private List<Region> semis = List.of();
    private Region finalMatch;
    private int activeRound = -1;
    private boolean redrawPending;

    public BracketView() {
        grid.setHgap(56); // espacio donde se dibujan las líneas
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER);

        for (int i = 0; i < 3; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 3);
            col.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(col);
        }

        // Fila 0: títulos de ronda. Filas 1..8: cuerpo del bracket (todas iguales).
        RowConstraints header = new RowConstraints();
        header.setMinHeight(28);
        header.setPrefHeight(28);
        header.setMaxHeight(28);
        grid.getRowConstraints().add(header);

        for (int i = 0; i < BODY_ROWS; i++) {
            RowConstraints row = new RowConstraints();
            row.setMinHeight(ROW_HEIGHT);
            row.setPrefHeight(ROW_HEIGHT);
            row.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(row);
        }

        linesLayer.setMouseTransparent(true);
        linesLayer.setPickOnBounds(false);
        getChildren().addAll(grid, linesLayer);

        // Redibujar los conectores cuando cambia el tamaño disponible.
        widthProperty().addListener((obs, oldValue, newValue) -> requestRedraw());
        heightProperty().addListener((obs, oldValue, newValue) -> requestRedraw());
    }

    /* Reemplaza el contenido del bracket y lo redibuja. */
    public void setMatches(List<? extends Region> quarters, List<? extends Region> semis, Region finalMatch) {
        this.quarters = List.copyOf(quarters);
        this.semis = List.copyOf(semis);
        this.finalMatch = finalMatch;
        rebuild();
    }

    /* Resalta el título de la ronda en juego: 0 cuartos, 1 semis, 2 final, -1 ninguna. */
    public void setActiveRound(int round) {
        this.activeRound = round;
        applyActiveRound();
    }

    private void applyActiveRound() {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i] == null) {
                continue;
            }
            headers[i].getStyleClass().remove("bracket-stage-header-active");
            if (i == activeRound) {
                headers[i].getStyleClass().add("bracket-stage-header-active");
            }
        }
    }

    private void rebuild() {
        grid.getChildren().clear();

        for (int i = 0; i < HEADER_TITLES.length; i++) {
            addHeader(i);
        }
        applyActiveRound();

        place(quarters, 0, 2);
        place(semis, 1, 4);
        if (finalMatch != null) {
            place(List.of(finalMatch), 2, 8);
        }

        requestRedraw();
    }

    private void addHeader(int column) {
        Label label = new Label(HEADER_TITLES[column]);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add("bracket-stage-header");
        headers[column] = label;
        grid.add(label, column, 0);
    }

    /* Coloca los nodos en una columna; cada uno ocupa 'span' filas, centrado. */
    private void place(List<? extends Region> cards, int column, int span) {
        for (int i = 0; i < cards.size(); i++) {
            Region card = cards.get(i);
            card.setMaxWidth(Double.MAX_VALUE);

            // Fijamos un alto máximo idéntico para todas las tarjetas (el tamaño estándar de cuartos)
            card.setMaxHeight(125);
            card.setPrefHeight(125);

            grid.add(card, column, 1 + i * span, 1, span);
            GridPane.setValignment(card, VPos.CENTER);
        }
    }

    /* Agrupa varios pedidos de redibujo en uno solo, después del layout. */
    private void requestRedraw() {
        if (redrawPending) {
            return;
        }
        redrawPending = true;
        Platform.runLater(() -> {
            redrawPending = false;
            applyCss();
            layout();
            drawLines();
        });
    }

    private void drawLines() {
        linesLayer.getChildren().clear();

        if (quarters.size() != 4 || semis.size() != 2 || finalMatch == null) {
            return;
        }

        connect(quarters.get(0), quarters.get(1), semis.get(0));
        connect(quarters.get(2), quarters.get(3), semis.get(1));
        connect(semis.get(0), semis.get(1), finalMatch);
    }

    /*
     * Conector en codo: [a] y [b] salen hacia una línea vertical que llega a [target].
     * Si alguno de los dos es parte del camino del campeón, ese tramo se repinta en dorado.
     */
    private void connect(Node a, Node b, Node target) {
        Bounds ba = toLayer(a);
        Bounds bb = toLayer(b);
        Bounds bt = toLayer(target);

        double midX = (ba.getMaxX() + bt.getMinX()) / 2;
        double targetY = bt.getCenterY();

        Path base = new Path(
                new MoveTo(ba.getMaxX(), ba.getCenterY()),
                new LineTo(midX, ba.getCenterY()),
                new LineTo(midX, bb.getCenterY()),
                new LineTo(bb.getMaxX(), bb.getCenterY()),
                new MoveTo(midX, targetY),
                new LineTo(bt.getMinX(), targetY));

        base.setFill(Color.TRANSPARENT);
        base.getStyleClass().add("bracket-line");
        linesLayer.getChildren().add(base);

        for (Node feeder : new Node[]{a, b}) {
            if (!isChampionPath(feeder)) {
                continue;
            }

            Bounds bf = toLayer(feeder);

            Path gold = new Path(
                    new MoveTo(bf.getMaxX(), bf.getCenterY()),
                    new LineTo(midX, bf.getCenterY()),
                    new LineTo(midX, targetY),
                    new LineTo(bt.getMinX(), targetY));

            gold.setFill(Color.TRANSPARENT);
            gold.getStyleClass().add("bracket-line-champion");
            linesLayer.getChildren().add(gold);
        }
    }

    private boolean isChampionPath(Node node) {
        return Boolean.TRUE.equals(node.getProperties().get(CHAMPION_PATH));
    }

    private Bounds toLayer(Node node) {
        return linesLayer.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
    }
}
