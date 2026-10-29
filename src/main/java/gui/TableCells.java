package gui;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import model.participant.Player;

/** Cell factories shared by the report and team tables. */
public final class TableCells {

    private TableCells() {
    }

    public static TableCell<Player, String> createCenterCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    getStyleClass().add("table-stat-cell");
                }
            }
        };
    }

    public static <T> TableCell<T, Integer> createIntegerCenterCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                    setAlignment(Pos.CENTER);
                    getStyleClass().add("table-stat-cell");
                }
            }
        };
    }

    // Helper para celdas de texto centradas
    public static <T> TableCell<T, String> createCenterStringCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                } else {
                    setText(val);
                    setAlignment(Pos.CENTER);
                    getStyleClass().add("table-stat-cell");
                }
            }
        };
    }

    public static <T> TableCell<T, String> createHighlightStatStringCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); }
                else {
                    setText(val);
                    setAlignment(Pos.CENTER);
                    getStyleClass().add("metric-value");
                    setStyle("-fx-font-size: 13px;");
                }
            }
        };
    }

    public static <T> TableCell<T, Number> createRankBadgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null); setGraphic(null);
                } else {
                    int rank = val.intValue();
                    Label b = new Label(String.valueOf(rank));
                    b.getStyleClass().add("rank-badge");
                    if (rank == 1) b.getStyleClass().add("rank-gold");
                    else if (rank == 2) b.getStyleClass().add("rank-silver");
                    else if (rank == 3) b.getStyleClass().add("rank-bronze");
                    else b.getStyleClass().add("rank-normal");
                    setGraphic(b); setText(null); setAlignment(Pos.CENTER);
                }
            }
        };
    }

    public static <T> TableCell<T, String> createBoldCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); }
                else { setText(val); getStyleClass().add("player-name-cell"); }
            }
        };
    }

    public static <T> TableCell<T, Integer> createHighlightStatCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Integer val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); }
                else {
                    setText(String.valueOf(val));
                    setAlignment(Pos.CENTER);
                    getStyleClass().add("metric-value");
                    setStyle("-fx-font-size: 14px;");
                }
            }
        };
    }
}
