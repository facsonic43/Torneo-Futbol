package PDF;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEvent;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEventHandler;
import com.itextpdf.kernel.pdf.event.PdfDocumentEvent;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.AreaBreakType;
import main.loader.TournamentData;
import model.participant.*;
import reports.ChampionshipStatistics;
import reports.ChampionshipStatistics.*;
import reports.ReportOptions;
import reports.ReportData;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/** Exports the eight reports required by section E from the current tournament state. */
public class PDFGenerator {
    public PDFGenerator(TournamentData data) {
        try {
            generate(data, Path.of("Report.pdf"), ReportOptions.defaults());
            System.out.println("PDF generated successfully: Report.pdf");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not generate Report.pdf", e);
        }
    }

    public static void generate(TournamentData data, Path output, ReportOptions options) throws IOException {
        generate(ReportData.fromInitialData(data), output, options);
    }

    public static void generate(ReportData data, Path output, ReportOptions options) throws IOException {
        Objects.requireNonNull(data, "Tournament data is required");
        Objects.requireNonNull(output, "Output path is required");
        Objects.requireNonNull(options, "Report options are required");
        ChampionshipStatistics statistics = new ChampionshipStatistics(data);
        if (options.player() != null && statistics.getPlayers(options.position(), options.player()).isEmpty()) {
            throw new IllegalArgumentException("The selected player does not belong to this tournament");
        }
        // Preserve the previous report if generation fails before the new file is closed.
        Path destination = output.toAbsolutePath();
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), ".championship-report-", ".pdf");
        try {
            write(data, statistics, options, temporary);
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void write(ReportData data, ChampionshipStatistics statistics,
                              ReportOptions options, Path output) throws IOException {
        try (PdfWriter writer = new PdfWriter(output.toString());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4)) {
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            document.setFont(font).setFontSize(10);
            document.setMargins(36, 36, 42, 36);
            pdf.getDocumentInfo().setTitle("Continental Cup - Reports, statistics and rankings").setAuthor("Championship");
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new PageFooter(font));
            addCover(document, data, statistics, options);
            nextPage(document);
            new IdentificationPdfSection().addTo(document, data, options.photosDirectory());
            nextPage(document);
            addScorers(document, statistics);
            nextPage(document);
            addFairPlay(document, statistics);
            nextPage(document);
            addMinutes(document, statistics);
            nextPage(document);
            BracketPdfSection.addTo(document, data);
            nextPage(document);
            addTeams(document, statistics);
            nextPage(document);
            addReferees(document, statistics);
            nextPage(document);
            PlayerPdfSection.addTo(document, data, statistics, options);
        }
    }

    private static void addCover(Document document, ReportData data,
                                 ChampionshipStatistics statistics, ReportOptions options) {
        document.add(new Paragraph("CONTINENTAL CUP").setFontSize(28).simulateBold().setFontColor(PdfReportStyle.NAVY).setMarginTop(45));
        document.add(new Paragraph("Reports, statistics and rankings").setFontSize(19));
        document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        int players = data.getTeams().stream().mapToInt(t -> t.getSquad().size()).sum();
        document.add(new Paragraph(data.getTeams().size() + " teams | " + players + " players | " + data.getReferees().size() + " referees"));
        document.add(new Paragraph("Completed matches: " + statistics.getPlayedMatchCount()));
        document.add(new Paragraph("I. Participant identification cards\nII. Top scorers\nIII. Fair Play ranking\n"
                + "IV. Most minutes played\nV. Championship bracket\nVI. Team statistics\nVII. Referee ranking\n"
                + "VIII. Player information").setMultipliedLeading(1.7f).setMarginTop(25));
        PdfReportStyle.note(document, "Rankings and match statistics include only completed matches in this championship. "
                + "Penalty shootouts are excluded from goals and team match scores.");
        PdfReportStyle.note(document, "Player information filter: "
                + (options.position() == null ? "All positions" : PdfReportStyle.position(options.position()))
                + " / " + (options.player() == null ? "All players" : options.player().getName())
                + ". Sections I to VII always include the complete tournament.");
        if (statistics.getPlayedMatchCount() == 0) {
            PdfReportStyle.note(document, "The championship has no completed matches yet. Match totals are zero and unresolved rounds remain pending.");
        }
        if (statistics.getIncompleteMatchCount() > 0) {
            PdfReportStyle.note(document, "Additional report details are missing for " + statistics.getIncompleteMatchCount()
                    + " completed matches. Minutes, penalty goals and goalkeeper statistics show only the known totals; "
                    + "they remain incomplete until starting lineups and goal details are supplied.");
        }
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
            PdfReportStyle.row(table, rank, row.player().getName(), row.team().getName(), row.goals(), row.penaltyGoals());
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

    private static void addTeams(Document document, ChampionshipStatistics statistics) {
        PdfReportStyle.heading(document, "VI. Team statistics");
        PdfReportStyle.note(document, "Teams in alphabetical order. Efficiency = points / (3 x matches played) x 100. "
                + "Each match, including each leg, awards 3 points for a win and 1 for a draw before a shootout. With no matches played, efficiency is 0%.");
        Table table = PdfReportStyle.table(new float[]{2.2f, 0.7f, 2.4f, 0.7f, 0.6f, 0.6f, 0.7f, 1},
                "Team", "Avg. age", "Coach / nationality", "Coach age", "GF", "GA", "Points", "Efficiency");
        for (TeamStats row : statistics.getTeamsAlphabetically()) {
            Coach coach = row.team().getCoach();
            PdfReportStyle.row(table, row.team().getName(), PdfReportStyle.number(row.averageAge()),
                    coach == null ? "Not assigned" : coach.getName() + "\n" + PdfReportStyle.country(coach),
                    coach == null ? "-" : coach.getAge(), row.goalsFor(), row.goalsAgainst(), row.points(), PdfReportStyle.number(row.efficiency()) + "%");
        }
        if (statistics.getTeamsAlphabetically().isEmpty()) PdfReportStyle.empty(table, 8, "No teams registered.");
        document.add(table);
    }

    private static void addReferees(Document document, ChampionshipStatistics statistics) {
        PdfReportStyle.heading(document, "VII. Referee ranking");
        PdfReportStyle.note(document, "Ranked by completed matches officiated in this championship. Equal match counts share a rank.");
        Table table = PdfReportStyle.table(new float[]{0.6f, 3, 2, 1.1f, 1.4f}, "Rank", "Referee", "Nationality", "Matches", "Years of experience");
        List<RefereeStats> referees = statistics.getRefereeRanking();
        int previous = -1, rank = 0;
        for (int i = 0; i < referees.size(); i++) {
            RefereeStats row = referees.get(i);
            if (row.matches() != previous) rank = i + 1;
            previous = row.matches();
            PdfReportStyle.row(table, rank, row.referee().getName(), PdfReportStyle.country(row.referee()), row.matches(),
                    row.yearsOfExperience() == null ? "Not supplied" : row.yearsOfExperience());
        }
        if (referees.isEmpty()) PdfReportStyle.empty(table, 5, "No referees registered.");
        document.add(table);
        int knownExperience = statistics.getRefereesWithKnownExperienceCount();
        document.add(new Paragraph("Average refereeing experience: " + (knownExperience == 0 ? "Not available"
                : PdfReportStyle.number(statistics.getAverageRefereeExperience()) + " years")
                + " (" + knownExperience + " of " + referees.size() + " referees with supplied experience)").simulateBold());
    }

    private static void nextPage(Document document) { document.add(new AreaBreak(AreaBreakType.NEXT_PAGE)); }

    private static class PageFooter extends AbstractPdfDocumentEventHandler {
        private final PdfFont font;
        PageFooter(PdfFont font) { this.font = font; }
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
