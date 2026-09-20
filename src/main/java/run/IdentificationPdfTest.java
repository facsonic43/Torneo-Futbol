package run;

import main.loader.TournamentData;
import main.loader.TournamentDataLoader;
import report.PdfIdentificationReportService;

/*
 * Permite probar de forma independiente la generación del Reporte I.
 * Carga todos los participantes del JSON y genera sus identificaciones
 * en un único archivo PDF.
 */
public class IdentificationPdfTest {

    public static void main(String[] args) {

        try {
            TournamentDataLoader loader =
                    new TournamentDataLoader();

            TournamentData data =
                    loader.load(
                            "torneo.json"
                    );

            PdfIdentificationReportService reportService =
                    new PdfIdentificationReportService();

            String outputFile =
                    "reports/identifications.pdf";

            int credentialsCreated =
                    reportService.generateIdentifications(
                            data.getTeams(),
                            data.getReferees(),
                            outputFile
                    );

            System.out.println(
                    "===================================="
            );

            System.out.println(
                    "REPORT I - IDENTIFICATIONS"
            );

            System.out.println(
                    "===================================="
            );

            System.out.println(
                    "Credentials created: "
                            + credentialsCreated
            );

            System.out.println(
                    "PDF created successfully."
            );

            System.out.println(
                    "File: "
                            + outputFile
            );

        } catch (Exception exception) {

            System.out.println(
                    "Error generating identifications PDF: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }
}