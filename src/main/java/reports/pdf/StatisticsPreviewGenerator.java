package reports.pdf;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEvent;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEventHandler;
import com.itextpdf.kernel.pdf.event.PdfDocumentEvent;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import reports.ChampionshipStatistics;
import reports.ChampionshipStatistics.PlayerStats;
import reports.ChampionshipStatistics.TeamStats;
import reports.ReportData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/** Builds the original report pages in memory for display in the Reports tab. */
public final class StatisticsPreviewGenerator {
    public enum Section { TOP_SCORERS, FAIR_PLAY, MOST_MINUTES, BRACKET }

    private StatisticsPreviewGenerator() { }

    public static byte[] generate(ReportData data, Section section) throws IOException {
        Objects.requireNonNull(data, "Tournament data is required");
        Objects.requireNonNull(section, "Report section is required");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(output);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4)) {
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            document.setFont(font).setFontSize(10);
            document.setMargins(36, 36, 42, 36);
            pdf.getDocumentInfo().setTitle("Continental Cup - Reports, statistics and rankings")
                    .setAuthor("Championship");
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new PageFooter(font));
            if (section == Section.BRACKET) {
                BracketPdfSection.addTo(document, data);
            } else {
                ChampionshipStatistics statistics = new ChampionshipStatistics(data);
                switch (section) {
                    case TOP_SCORERS -> addScorers(document, statistics);
                    case FAIR_PLAY -> addFairPlay(document, statistics);
                    case MOST_MINUTES -> addMinutes(document, statistics);
                    default -> throw new IllegalStateException("Unsupported report section: " + section);
                }
            }
        }
        return output.toByteArray();
    }

    private static void addScorers(Document document, ChampionshipStatistics statistics) {
        PdfReportStyle.heading(document, "II. Top scorers");
        PdfReportStyle.note(document, "Goals scored during matches, including match penalties. Own goals and penalty shootouts are excluded.");
        Table table = PdfReportStyle.table(new float[]{0.5f, 2.5f, 2.5f, 0.8f, 1.1f}, "Rank", "Player", "Team", "Goals", "Of which penalties");
        List<PlayerStats> players = statistics.getTopScorers();
        int previous = -1, rank = 0;
        for (int i = 0; i < players.size(); i++) {
            PlayerStats row = players.get(i);
            if (row.goals() != previous) rank = i + 1;
            previous = row.goals();
            PdfReportStyle.row(table, rank, row.player().getName(), row.team().getName(), row.goals(),
                    statistics.hasCompleteGoalInformation() ? row.penaltyGoals() : "Not available");
        }
        if (players.isEmpty()) PdfReportStyle.empty(table, 5, "No goals have been scored in this championship.");
        document.add(table);
    }

    private static void addFairPlay(Document document, ChampionshipStatistics statistics) {
        PdfReportStyle.heading(document, "III. Fair Play ranking");
        PdfReportStyle.note(document, "Penalty points: 1 per yellow card + 3 per red card. Fewer points ranks higher. "
                + "A sending-off for two yellows counts as two yellow cards and one red card. Equal scores share a rank.");
        Table table = PdfReportStyle.table(new float[]{0.6f, 3.4f, 1, 1, 1, 1}, "Rank", "Team", "Played", "Yellows", "Reds", "Penalty points");
        List<TeamStats> teams = statistics.getFairPlayRanking();
        int previous = -1, rank = 0;
        for (int i = 0; i < teams.size(); i++) {
            TeamStats row = teams.get(i);
            if (row.fairPlayScore() != previous) rank = i + 1;
            previous = row.fairPlayScore();
            PdfReportStyle.row(table, rank, row.team().getName(), row.played(), row.yellowCards(), row.redCards(), row.fairPlayScore());
        }
        if (teams.isEmpty()) PdfReportStyle.empty(table, 6, "No teams registered.");
        document.add(table);
    }

    private static void addMinutes(Document document, ChampionshipStatistics statistics) {
        PdfReportStyle.heading(document, "IV. Most minutes played");
        PdfReportStyle.note(document, "Participation includes starters and substitutes. Minutes end at substitution, dismissal or the final whistle.");
        Table table = PdfReportStyle.table(new float[]{0.5f, 2.7f, 2.7f, 1, 1}, "Rank", "Player", "Team", "Matches", "Minutes");
        if (!statistics.hasCompleteStartingLineups()) {
            PdfReportStyle.empty(table, 5,
                    "Not available: starting lineups were not supplied for one or more completed matches.");
            document.add(table);
            return;
        }
        List<PlayerStats> players = statistics.getMinutesRanking();
        int previous = -1, rank = 0;
        for (int i = 0; i < players.size(); i++) {
            PlayerStats row = players.get(i);
            if (row.minutes() != previous) rank = i + 1;
            previous = row.minutes();
            PdfReportStyle.row(table, rank, row.player().getName(), row.team().getName(), row.matches(), row.minutes());
        }
        if (players.isEmpty()) PdfReportStyle.empty(table, 5, "No players registered.");
        document.add(table);
    }

    private static final class PageFooter extends AbstractPdfDocumentEventHandler {
        private final PdfFont font;

        private PageFooter(PdfFont font) { this.font = font; }

        @Override
        protected void onAcceptedEvent(AbstractPdfDocumentEvent event) {
            PdfDocumentEvent pageEvent = (PdfDocumentEvent) event;
            PdfPage page = pageEvent.getPage();
            PdfDocument pdf = pageEvent.getDocument();
            PdfCanvas canvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdf);
            canvas.beginText().setFontAndSize(font, 8).moveText(36, 22)
                    .showText("Continental Cup | Reports | Page " + pdf.getPageNumber(page)).endText();
            canvas.release();
        }
    }
}
