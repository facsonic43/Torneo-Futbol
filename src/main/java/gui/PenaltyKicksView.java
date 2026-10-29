package gui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.match.PenaltyTaken;
import java.util.List;

/** Draws the kicks of a penalty shootout: the two columns and the kick-by-kick detail. */
public class PenaltyKicksView {

    private final HBox penaltyColumnHeaderBox;
    private final HBox penaltyHomeKicksBox;
    private final HBox penaltyAwayKicksBox;
    private final VBox penaltyKickDetailsBox;

    public PenaltyKicksView(HBox penaltyColumnHeaderBox, HBox penaltyHomeKicksBox,
            HBox penaltyAwayKicksBox, VBox penaltyKickDetailsBox) {
        this.penaltyColumnHeaderBox = penaltyColumnHeaderBox;
        this.penaltyHomeKicksBox = penaltyHomeKicksBox;
        this.penaltyAwayKicksBox = penaltyAwayKicksBox;
        this.penaltyKickDetailsBox = penaltyKickDetailsBox;
    }

    /*
     * Genera las columnas 1 a 5 y las necesarias para muerte súbita.
     */
    public void renderColumns(
            List<PenaltyTaken> homePenalties,
            List<PenaltyTaken> awayPenalties,
            boolean finished) {

        penaltyColumnHeaderBox
                .getChildren()
                .clear();

        penaltyHomeKicksBox
                .getChildren()
                .clear();

        penaltyAwayKicksBox
                .getChildren()
                .clear();

        int columns =
                Math.max(
                        5,
                        Math.max(
                                homePenalties.size(),
                                awayPenalties.size()
                        )
                );

        for (int i = 0;
             i < columns;
             i++) {

            String header;

            if (i < 5) {

                header =
                        String.valueOf(
                                i + 1
                        );

            } else {

                header =
                        "SD"
                                + (i - 4);
            }

            penaltyColumnHeaderBox
                    .getChildren()
                    .add(
                            createPenaltyHeader(
                                    header
                            )
                    );

            penaltyHomeKicksBox
                    .getChildren()
                    .add(
                            createPenaltyCell(
                                    i < homePenalties.size()
                                            ? homePenalties.get(i)
                                            : null,
                                    finished
                            )
                    );

            penaltyAwayKicksBox
                    .getChildren()
                    .add(
                            createPenaltyCell(
                                    i < awayPenalties.size()
                                            ? awayPenalties.get(i)
                                            : null,
                                    finished
                            )
                    );
        }
    }

    /*
     * Muestra de manera visible quién convirtió o falló cada penal.
     */
    public void renderKickDetails(
            List<PenaltyTaken> homePenalties,
            List<PenaltyTaken> awayPenalties) {

        penaltyKickDetailsBox
                .getChildren()
                .clear();

        int rounds =
                Math.max(
                        homePenalties.size(),
                        awayPenalties.size()
                );

        /*
         * Antes de ejecutar la tanda no mostramos detalles vacíos.
         */
        if (rounds == 0) {

            penaltyKickDetailsBox.setVisible(
                    false
            );

            penaltyKickDetailsBox.setManaged(
                    false
            );

            return;
        }

        penaltyKickDetailsBox.setVisible(
                true
        );

        penaltyKickDetailsBox.setManaged(
                true
        );

        for (int i = 0;
             i < rounds;
             i++) {

            String roundName =
                    i < 5
                            ? "ROUND " + (i + 1)
                            : "SUDDEN DEATH " + (i - 4);

            Label roundLabel =
                    new Label(
                            roundName
                    );

            roundLabel.getStyleClass()
                    .add(
                            "small-label"
                    );

            VBox roundBox =
                    new VBox(
                            5
                    );

            roundBox.getStyleClass()
                    .add(
                            "event-row"
                    );

            roundBox
                    .getChildren()
                    .add(
                            roundLabel
                    );

            if (i < homePenalties.size()) {

                roundBox
                        .getChildren()
                        .add(
                                createPenaltyDetailLabel(
                                        homePenalties.get(i)
                                )
                        );
            }

            if (i < awayPenalties.size()) {

                roundBox
                        .getChildren()
                        .add(
                                createPenaltyDetailLabel(
                                        awayPenalties.get(i)
                                )
                        );
            }

            penaltyKickDetailsBox
                    .getChildren()
                    .add(
                            roundBox
                    );
        }
    }

    private Label createPenaltyHeader(
            String text) {

        Label label =
                new Label(
                        text
                );

        label.setAlignment(
                Pos.CENTER
        );

        label.setMinSize(
                42,
                24
        );

        label.setPrefSize(
                42,
                24
        );

        label.setMaxSize(
                42,
                24
        );

        label.getStyleClass()
                .add(
                        "small-label"
                );

        return label;
    }

    /*
     * Genera el indicador visual verde, rojo o pendiente.
     */
    private Label createPenaltyCell(
            PenaltyTaken penalty,
            boolean shootoutFinished) {

        Label cell =
                new Label();

        cell.setAlignment(
                Pos.CENTER
        );

        cell.setMinSize(
                42,
                38
        );

        cell.setPrefSize(
                42,
                38
        );

        cell.setMaxSize(
                42,
                38
        );

        if (penalty == null) {

            cell.setText(
                    shootoutFinished
                            ? "—"
                            : "•"
            );

            cell.getStyleClass()
                    .add(
                            "purple-chip"
                    );

            return cell;
        }

        if (penalty.isScored()) {

            cell.setText(
                    "✓"
            );

            cell.getStyleClass()
                    .add(
                            "ready-chip"
                    );

            cell.setTooltip(
                    new Tooltip(
                            penalty
                                    .getPlayer()
                                    .getName()
                                    + " - SCORED"
                    )
            );

        } else {

            cell.setText(
                    "✕"
            );

            cell.getStyleClass()
                    .add(
                            "blocked-chip"
                    );

            cell.setTooltip(
                    new Tooltip(
                            penalty
                                    .getPlayer()
                                    .getName()
                                    + " - MISSED"
                    )
            );
        }

        return cell;
    }

    /*
     * Genera una línea como:
     * LONABARCE · Juan Perez     ✓ SCORED
     */
    private Label createPenaltyDetailLabel(
            PenaltyTaken penalty) {

        String result =
                penalty.isScored()
                        ? "✓ SCORED"
                        : "✕ MISSED";

        String text =
                penalty.getTeam()
                        .getName()
                        + " · "
                        + penalty
                        .getPlayer()
                        .getName()
                        + "     "
                        + result;

        Label label =
                new Label(
                        text
                );

        label.setWrapText(
                true
        );

        if (penalty.isScored()) {

            label.getStyleClass()
                    .add(
                            "ready-chip"
                    );

        } else {

            label.getStyleClass()
                    .add(
                            "blocked-chip"
                    );
        }

        return label;
    }
}
