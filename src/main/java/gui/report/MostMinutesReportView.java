package gui.report;

import gui.StatisticsReportView;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import reports.ReportSection;

/**
 * Configura el reporte de jugadores con mayor cantidad de minutos disputados.
 */
public class MostMinutesReportView extends RankingReportView {
    public MostMinutesReportView(StackPane reportContentHost, Label reportMetricChip, Label selectedReportSubtitle,
                                 Label selectedReportTitle, Node reportsPane, StatisticsReportView statisticsReportView) {
        super(reportContentHost, reportMetricChip, selectedReportSubtitle, selectedReportTitle, reportsPane,
                statisticsReportView);
    }

    @Override
    protected ReportSection section() {
        return ReportSection.MOST_MINUTES;
    }

    @Override
    protected String title() {
        return "Most Minutes Played";
    }

    @Override
    protected String subtitle() {
        return "Player participation and minutes in this championship";
    }
}
