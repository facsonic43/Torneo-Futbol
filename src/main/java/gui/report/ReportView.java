package gui.report;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import model.tournament.Tournament;

/**
 * A report of the Reports screen. Each subclass draws one report into the shared panel;
 * the screen only chooses which view to show.
 */
public abstract class ReportView {

    protected final StackPane reportContentHost;
    protected final Label reportMetricChip;
    protected final Label selectedReportSubtitle;
    protected final Label selectedReportTitle;
    protected final Node reportsPane;
    protected Tournament tournament;

    protected ReportView(StackPane reportContentHost, Label reportMetricChip, Label selectedReportSubtitle,
                         Label selectedReportTitle, Node reportsPane) {
        this.reportContentHost = reportContentHost;
        this.reportMetricChip = reportMetricChip;
        this.selectedReportSubtitle = selectedReportSubtitle;
        this.selectedReportTitle = selectedReportTitle;
        this.reportsPane = reportsPane;
    }

    /** Shows this report for the given tournament. */
    public final void show(Tournament tournament) throws Exception {
        this.tournament = tournament;
        render();
    }

    /** Fills title, subtitle, metric chip and content of the report panel. */
    protected abstract void render() throws Exception;
}
