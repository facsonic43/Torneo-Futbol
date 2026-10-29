package gui.report;

import gui.StatisticsReportView;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import reports.ReportSection;

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
