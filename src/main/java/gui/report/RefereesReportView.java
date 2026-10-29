package gui.report;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.match.Match;
import model.participant.Referee;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import gui.RefereeReportEntry;
import static gui.TableCells.createIntegerCenterCell;
import static gui.TableCells.createCenterStringCell;
import static gui.TableCells.createRankBadgeCell;
import static gui.TableCells.createBoldCell;
import static gui.TableCells.createHighlightStatCell;

public class RefereesReportView extends ReportView {

    public RefereesReportView(StackPane reportContentHost, Label reportMetricChip, Label selectedReportSubtitle,
            Label selectedReportTitle, Node reportsPane) {
        super(reportContentHost, reportMetricChip, selectedReportSubtitle, selectedReportTitle, reportsPane);
    }

    // --- REPORTE VI: REFEREES ---
    @Override
    protected void render() {
        selectedReportTitle.setText("Match Officials Registry");
        selectedReportSubtitle.setText("Registered referees, experience and average officiating career duration");

        VBox layout = new VBox(12);
        layout.setFillWidth(true);
        VBox.setVgrow(layout, Priority.ALWAYS);

        TableView<RefereeReportEntry> table = new TableView<>();
        table.getStyleClass().add("dark-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<RefereeReportEntry, Number> rankCol = new TableColumn<>("#");
        rankCol.setPrefWidth(45);
        rankCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(table.getItems().indexOf(cell.getValue()) + 1));
        rankCol.setCellFactory(col -> createRankBadgeCell());

        TableColumn<RefereeReportEntry, String> nameCol = new TableColumn<>("NAME");
        nameCol.setPrefWidth(180);
        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        nameCol.setCellFactory(col -> createBoldCell());

        TableColumn<RefereeReportEntry, String> natCol = new TableColumn<>("COUNTRY");
        natCol.setPrefWidth(110);
        natCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNationality()));

        TableColumn<RefereeReportEntry, Integer> expCol = new TableColumn<>("EXPERIENCE (YEARS)");
        expCol.setPrefWidth(90);
        expCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getYearsOfficiated()));
        expCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<RefereeReportEntry, Integer> matchesCol = new TableColumn<>("MATCHES OFFICIATED");
        matchesCol.setPrefWidth(85);
        matchesCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getMatchesOfficiated()));
        matchesCol.setCellFactory(col -> createHighlightStatCell());

        table.getColumns().addAll(rankCol, nameCol, natCol, expCol, matchesCol);

        List<RefereeReportEntry> list = new ArrayList<>();
        int totalYears = 0;
        if (tournament.getReferees() != null) {
            for (Referee ref : tournament.getReferees()) {
                String nat = ref.getNationality() != null ? ref.getNationality().getName() : "-";
                totalYears += ref.getYearsOfficiated();
                list.add(new RefereeReportEntry(
                        ref.getName(),
                        nat,
                        ref.getBirthDate(),
                        ref.getIdType() != null ? ref.getIdType().toString() : "-",
                        ref.getIdNumber(),
                        ref.getYearsOfficiated(),
                        ref.getMatchesOfficiated()
                ));
            }
        }

        list.sort(Comparator.comparingInt(RefereeReportEntry::getMatchesOfficiated).reversed());
        table.getItems().setAll(list);

        double avgExp = !list.isEmpty() ? (double) totalYears / list.size() : 0.0;

        HBox summaryBar = new HBox(12);
        summaryBar.getStyleClass().add("info-card");
        summaryBar.setAlignment(Pos.CENTER_RIGHT);

        Label avgLabel = new Label(String.format("Average Referee Experience: %.1f years", avgExp));
        avgLabel.getStyleClass().add("accent-text");
        summaryBar.getChildren().add(avgLabel);

        layout.getChildren().addAll(table, summaryBar);
        reportMetricChip.setText(list.size() + " REFEREES");
        reportContentHost.getChildren().add(layout);
    }
}
