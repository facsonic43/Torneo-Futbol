package gui.report;

import gui.StatisticsReportView;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import model.match.Match;
import reports.ReportSection;

/**
 * A report that ranks players or teams. The table is drawn by the shared
 * {@link StatisticsReportView}; every subclass only says which report it is.
 */
public abstract class RankingReportView extends ReportView {

    private final StatisticsReportView statisticsReportView;

    protected RankingReportView(StackPane reportContentHost, Label reportMetricChip, Label selectedReportSubtitle,
                                Label selectedReportTitle, Node reportsPane, StatisticsReportView statisticsReportView) {
        super(reportContentHost, reportMetricChip, selectedReportSubtitle, selectedReportTitle, reportsPane);
        this.statisticsReportView = statisticsReportView;
    }

    protected abstract ReportSection section();

    protected abstract String title();

    protected abstract String subtitle();

    @Override
    protected void render() throws Exception {
        selectedReportTitle.setText(title());
        selectedReportSubtitle.setText(subtitle());

        long played = tournament.getAllMatches().stream().filter(Match::isPlayed).count();
        reportMetricChip.setText(played + " MATCHES PLAYED");

        if (!reportContentHost.getChildren().contains(statisticsReportView)) {
            reportContentHost.getChildren().setAll(statisticsReportView);
        }
        statisticsReportView.showReport(tournament, section());
    }
}
