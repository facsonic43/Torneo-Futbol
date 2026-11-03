package gui.report;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import model.tournament.Tournament;

/**
 * Define la base de los reportes que se muestran en el panel compartido del campeonato.
 * Gestiona el torneo actual, los títulos y el contenido que dibuja cada subclase.
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

    public final void show(Tournament tournament) throws Exception {
        this.tournament = tournament;
        render();
    }

    protected abstract void render() throws Exception;
}
