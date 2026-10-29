package gui.report;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.participant.Team;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import gui.CoachReportEntry;
import static gui.TableCells.createCenterStringCell;
import static gui.TableCells.createBoldCell;
import static gui.TableCells.createHighlightStatCell;

public class CoachesReportView extends ReportView {

    public CoachesReportView(StackPane reportContentHost, Label reportMetricChip, Label selectedReportSubtitle,
            Label selectedReportTitle, Node reportsPane) {
        super(reportContentHost, reportMetricChip, selectedReportSubtitle, selectedReportTitle, reportsPane);
    }

    @Override
    protected void render() {
        selectedReportTitle.setText("Head Coaches Directory");
        selectedReportSubtitle.setText("Technical directors, nationalities, titles won and club assignments");

        VBox layout = new VBox(12);
        layout.setFillWidth(true);
        VBox.setVgrow(layout, Priority.ALWAYS);

        TableView<CoachReportEntry> table = new TableView<>();
        table.getStyleClass().add("dark-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<CoachReportEntry, String> nameCol = new TableColumn<>("NAME");
        nameCol.setPrefWidth(180);
        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        nameCol.setCellFactory(col -> createBoldCell());

        TableColumn<CoachReportEntry, String> teamCol = new TableColumn<>("CLUB");
        teamCol.setPrefWidth(160);
        teamCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTeamName()));

        TableColumn<CoachReportEntry, String> natCol = new TableColumn<>("COUNTRY");
        natCol.setPrefWidth(120);
        natCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNationality()));

        TableColumn<CoachReportEntry, Integer> titlesCol = new TableColumn<>("TITLES WON");
        titlesCol.setPrefWidth(100);
        titlesCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getTitlesWon()));
        titlesCol.setCellFactory(col -> createHighlightStatCell());

        table.getColumns().addAll(nameCol, teamCol, natCol, titlesCol);

        List<CoachReportEntry> list = new ArrayList<>();
        for (Team team : tournament.getTeams()) {
            if (team.getCoach() != null) {
                var coach = team.getCoach();
                String nat = coach.getNationality() != null ? coach.getNationality().getName() : "-";
                list.add(new CoachReportEntry(
                        coach.getName(),
                        team.getName(),
                        nat,
                        coach.getTitlesObtained(),
                        coach.getBirthDate()
                ));
            }
        }

        list.sort(Comparator.comparingInt(CoachReportEntry::getTitlesWon).reversed());
        table.getItems().setAll(list);

        layout.getChildren().add(table);
        reportMetricChip.setText(list.size() + " COACHES");
        reportContentHost.getChildren().add(layout);
    }
}
