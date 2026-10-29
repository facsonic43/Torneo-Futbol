package gui.report;

import gui.StatisticsReportView;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import reports.ReportSection;

public class FairPlayReportView extends RankingReportView {

    public FairPlayReportView(StackPane reportContentHost, Label reportMetricChip, Label selectedReportSubtitle,
                              Label selectedReportTitle, Node reportsPane, StatisticsReportView statisticsReportView) {
        super(reportContentHost, reportMetricChip, selectedReportSubtitle, selectedReportTitle, reportsPane,
                statisticsReportView);
    }

    @Override
    protected ReportSection section() {
        return ReportSection.FAIR_PLAY;
    }

    @Override
    protected String title() {
        return "Fair Play Ranking";
    }

    @Override
    protected String subtitle() {
        return "Teams ranked by disciplinary points in this championship";
    }
}
