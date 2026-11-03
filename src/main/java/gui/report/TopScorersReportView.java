package gui.report;

import gui.StatisticsReportView;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import reports.ReportSection;

// Configura el reporte de goleadores del campeonato a partir de los goles de cada jugador.

public class TopScorersReportView extends RankingReportView {
    public TopScorersReportView(StackPane reportContentHost, Label reportMetricChip, Label selectedReportSubtitle,
            Label selectedReportTitle, Node reportsPane, StatisticsReportView statisticsReportView) {
        super(reportContentHost, reportMetricChip, selectedReportSubtitle, selectedReportTitle, reportsPane,
                statisticsReportView);
    }

    @Override
    protected ReportSection section() {
        return ReportSection.TOP_SCORERS;
    }

    @Override
    protected String title() {
        return "Top Scorers";
    }

    @Override
    protected String subtitle() {
        return "Goals and penalties scored in this championship";
    }
}
