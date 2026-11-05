package gui.report;

import gui.StatisticsReportView;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import model.match.Match;
import reports.ReportSection;

// Comparte la presentación de los reportes que clasifican jugadores o equipos.
// Cada subclase define el contenido y sus títulos; la vista estadística construye la tabla.

public abstract class RankingReportView extends ReportView {
    private final StatisticsReportView statisticsReportView;

    protected RankingReportView(StackPane reportContentHost, Label reportMetricChip, Label selectedReportSubtitle,
            Label selectedReportTitle, Node reportsPane, StatisticsReportView statisticsReportView) {
        super(reportContentHost, reportMetricChip, selectedReportSubtitle, selectedReportTitle, reportsPane);
        this.statisticsReportView = statisticsReportView;
    }

    public StatisticsReportView getStatisticsReportView() {
        return statisticsReportView;
    }

    protected abstract ReportSection section();

    protected abstract String title();

    protected abstract String subtitle();

    @Override
    protected void render() throws Exception {
        getSelectedReportTitle().setText(title());
        getSelectedReportSubtitle().setText(subtitle());
        long played = getTournament().getAllMatches().stream().filter(Match::isPlayed).count();
        getReportMetricChip().setText(played + " MATCHES PLAYED");
        if (!getReportContentHost().getChildren().contains(statisticsReportView)) {
            getReportContentHost().getChildren().setAll(statisticsReportView);
        }
        statisticsReportView.showReport(getTournament(), section());
    }
}
