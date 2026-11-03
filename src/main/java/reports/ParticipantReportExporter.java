package reports;

import main.loader.TournamentData;
import model.tournament.Tournament;
import reports.pdf.PDFGenerator;

import java.io.IOException;
import java.nio.file.Path;

// Exporta las credenciales de identificación de todos los participantes del campeonato
// a un archivo PDF utilizando las opciones de fotografías indicadas.

public final class ParticipantReportExporter {
    private ParticipantReportExporter() {
    }

    public static void export(Tournament tournament, Path destination) throws IOException {
        TournamentData participants = new TournamentData(tournament.getTeams(), tournament.getReferees());
        ReportData data = new ReportData(participants);
        PDFGenerator.generate(data, destination, ReportOptions.defaults());
    }
}
