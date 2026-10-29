package gui.report;

import gui.BracketSection;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import model.participant.Team;

/** The championship bracket shown inside the Reports screen. */
public class BracketReportView extends ReportView {

    private final BracketSection bracketSection;

    public BracketReportView(StackPane reportContentHost, Label reportMetricChip, Label selectedReportSubtitle,
            Label selectedReportTitle, Node reportsPane, BracketSection bracketSection) {
        super(reportContentHost, reportMetricChip, selectedReportSubtitle, selectedReportTitle, reportsPane);
        this.bracketSection = bracketSection;
    }

    @Override
    protected void render() {
        selectedReportTitle.setText("Championship Bracket");
        selectedReportSubtitle.setText("Knockout stage roadmap and match results");

        Team champion = bracketSection.load(tournament);

        ScrollPane scrollPane = new ScrollPane(bracketSection.getView());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        reportMetricChip.setText(champion != null ? "CHAMPION: " + champion.getName().toUpperCase() : "IN PROGRESS");
        reportContentHost.getChildren().add(scrollPane);
    }
}
