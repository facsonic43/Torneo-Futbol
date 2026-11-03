package gui.report;

import gui.BracketSection;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import model.participant.Team;

// Muestra el cuadro eliminatorio del campeonato dentro de la pantalla de reportes.

public class BracketReportView extends ReportView {
    private final BracketSection bracketSection;

    public BracketReportView(StackPane reportContentHost, Label reportMetricChip, Label selectedReportSubtitle,
            Label selectedReportTitle, Node reportsPane, BracketSection bracketSection) {
        super(reportContentHost, reportMetricChip, selectedReportSubtitle, selectedReportTitle, reportsPane);
        this.bracketSection = bracketSection;
    }

    @Override
    protected void render() {
        getSelectedReportTitle().setText("Championship Bracket");
        getSelectedReportSubtitle().setText("Knockout stage roadmap and match results");
        Team champion = bracketSection.load(getTournament());
        ScrollPane scrollPane = new ScrollPane(bracketSection.getView());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        getReportMetricChip().setText(champion != null ? "CHAMPION: " + champion.getName().toUpperCase() : "IN PROGRESS");
        getReportContentHost().getChildren().add(scrollPane);
    }
}
