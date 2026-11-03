package gui.report;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import model.tournament.Tournament;

// Define la base de los reportes que se muestran en el panel compartido del campeonato.
// Gestiona el torneo actual, los títulos y el contenido que dibuja cada subclase.

public abstract class ReportView {
    private final StackPane reportContentHost;
    private final Label reportMetricChip;
    private final Label selectedReportSubtitle;
    private final Label selectedReportTitle;
    private final Node reportsPane;
    private Tournament tournament;

    protected ReportView(StackPane reportContentHost, Label reportMetricChip, Label selectedReportSubtitle,
            Label selectedReportTitle, Node reportsPane) {
        this.reportContentHost = reportContentHost;
        this.reportMetricChip = reportMetricChip;
        this.selectedReportSubtitle = selectedReportSubtitle;
        this.selectedReportTitle = selectedReportTitle;
        this.reportsPane = reportsPane;
    }

    public final void show(Tournament tournament) throws Exception {
        this.tournament = tournament;
        render();
    }

    protected StackPane getReportContentHost() {
        return reportContentHost;
    }

    protected Label getReportMetricChip() {
        return reportMetricChip;
    }

    protected Label getSelectedReportSubtitle() {
        return selectedReportSubtitle;
    }

    protected Label getSelectedReportTitle() {
        return selectedReportTitle;
    }

    protected Node getReportsPane() {
        return reportsPane;
    }

    protected Tournament getTournament() {
        return tournament;
    }

    protected abstract void render() throws Exception;
}
