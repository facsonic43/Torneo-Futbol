package gui.report;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.participant.Player;
import model.participant.Team;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import gui.TeamSummaryEntry;
import static gui.TableCells.createIntegerCenterCell;
import static gui.TableCells.createHighlightStatStringCell;
import static gui.TableCells.createBoldCell;

public class TeamsReportView extends ReportView {

    public TeamsReportView(StackPane reportContentHost, Label reportMetricChip, Label selectedReportSubtitle,
            Label selectedReportTitle, Node reportsPane) {
        super(reportContentHost, reportMetricChip, selectedReportSubtitle, selectedReportTitle, reportsPane);
    }

    // --- REPORTE V: TEAMS ---
    // --- REPORTE TEAMS ---
    @Override
    protected void render() {
        selectedReportTitle.setText("Participating Teams Analysis");
        selectedReportSubtitle.setText("Comprehensive technical, staff and performance metrics for all clubs");

        VBox layout = new VBox(12);
        layout.setFillWidth(true);
        VBox.setVgrow(layout, Priority.ALWAYS);

        TableView<TeamSummaryEntry> table = new TableView<>();
        table.getStyleClass().add("dark-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<TeamSummaryEntry, String> teamCol = new TableColumn<>("TEAM");
        teamCol.setPrefWidth(160);
        teamCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTeamName()));
        teamCol.setCellFactory(col -> createBoldCell());

        TableColumn<TeamSummaryEntry, String> countryCol = new TableColumn<>("COUNTRY");
        countryCol.setPrefWidth(110);
        countryCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCountryName()));

        TableColumn<TeamSummaryEntry, Integer> rankCol = new TableColumn<>("RANK");
        rankCol.setPrefWidth(60);
        rankCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getRanking()));
        rankCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<TeamSummaryEntry, String> avgAgeCol = new TableColumn<>("AVERAGE AGE");
        avgAgeCol.setPrefWidth(110);
        avgAgeCol.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format(Locale.ENGLISH, "%.1f", cell.getValue().getSquadAverageAge())));

        TableColumn<TeamSummaryEntry, String> coachCol = new TableColumn<>("HEAD COACH");
        coachCol.setPrefWidth(150);
        coachCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCoachName()));

        TableColumn<TeamSummaryEntry, String> coachAgeCol = new TableColumn<>("COACH AGE");
        coachAgeCol.setPrefWidth(80);
        coachAgeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCoachAge()));

        TableColumn<TeamSummaryEntry, String> coachNatCol = new TableColumn<>("COACH COUNTRY");
        coachNatCol.setPrefWidth(110);
        coachNatCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCoachNationality()));

        TableColumn<TeamSummaryEntry, Integer> gfCol = new TableColumn<>("SCORED");
        gfCol.setPrefWidth(50);
        gfCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getTotalGoalsScored()));
        gfCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<TeamSummaryEntry, Integer> gaCol = new TableColumn<>("CONCEDED");
        gaCol.setPrefWidth(50);
        gaCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getTotalGoalsConceded()));
        gaCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<TeamSummaryEntry, String> effCol = new TableColumn<>("EFFECTIVENESS");
        effCol.setPrefWidth(110);
        effCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.1f%%", cell.getValue().getEffectiveness())));
        effCol.setCellFactory(col -> createHighlightStatStringCell());

        TableColumn<TeamSummaryEntry, Integer> ovrCol = new TableColumn<>("OVERALL");
        ovrCol.setPrefWidth(60);
        ovrCol.setCellValueFactory(cell -> new SimpleObjectProperty<>((int) Math.round(cell.getValue().getOverall())));
        ovrCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(String.valueOf(val));
                    badge.getStyleClass().add("ovr-badge");
                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        table.getColumns().addAll(teamCol, countryCol, rankCol, avgAgeCol, coachCol, coachAgeCol, coachNatCol, gfCol, gaCol, effCol, ovrCol);

        List<TeamSummaryEntry> list = new ArrayList<>();
        for (Team t : tournament.getTeams()) {
            int squadSize = t.getSquad().size();
            double totalAge = 0;
            int gf = 0;
            int ga = 0;

            for (Player p : t.getSquad()) {
                totalAge += p.getAge();
                gf += p.getGoals();
                ga += p.getGoalsConceded();
            }
            double avgAge = squadSize > 0 ? (totalAge / squadSize) : 0;
            list.add(new TeamSummaryEntry(t, avgAge, gf, ga, tournament.getGroups()));
        }

        list.sort(Comparator.comparing(TeamSummaryEntry::getTeamName, String.CASE_INSENSITIVE_ORDER));
        table.getItems().setAll(list);

        layout.getChildren().add(table);
        reportMetricChip.setText(list.size() + " TEAMS");
        reportContentHost.getChildren().add(layout);
    }
}
