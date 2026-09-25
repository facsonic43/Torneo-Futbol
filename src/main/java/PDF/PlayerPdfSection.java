package PDF;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import model.participant.*;
import reports.ChampionshipStatistics;
import reports.ChampionshipStatistics.PlayerStats;
import reports.ReportOptions;
import reports.ReportData;

import java.util.List;

final class PlayerPdfSection {
    private PlayerPdfSection() { }

    static void addTo(Document document, ReportData data, ChampionshipStatistics statistics, ReportOptions options) {
        PdfReportStyle.heading(document, "VIII. Player information");
        PdfReportStyle.note(document, "Position: "
                + (options.position() == null ? "All" : PdfReportStyle.position(options.position()))
                + ". Player: " + (options.player() == null ? "All" : options.player().getName()) + ".");
        boolean hasCompleteStartingLineups = statistics.hasCompleteStartingLineups();
        boolean hasCompleteGoalInformation = statistics.hasCompleteGoalInformation();
        if (!hasCompleteStartingLineups) {
            PdfReportStyle.note(document,
                    "Matches and minutes are Not available until starting lineups are supplied for every completed match.");
        }
        if (!hasCompleteGoalInformation) {
            PdfReportStyle.note(document,
                    "Penalty goals, assists and goalkeeper statistics are Not available until goal information is supplied.");
        }
        List<PlayerStats> players = statistics.getPlayers(options.position(), options.player());
        if (players.isEmpty()) {
            document.add(new Paragraph("No players match the selected filters."));
            return;
        }
        for (PlayerStats row : players) {
            Player player = row.player();
            Div card = new Div().setKeepTogether(true).setPadding(9).setMarginBottom(12).setBackgroundColor(PdfReportStyle.LIGHT);
            card.add(new Paragraph(player.getName() + " | " + row.team().getName())
                    .setFontSize(12).simulateBold().setFontColor(PdfReportStyle.NAVY).setMargin(0));
            card.add(new Paragraph("Document: " + player.getIdType() + " " + player.getIdNumber()
                    + " | Born: " + player.getBirthDate() + " | Age: " + player.getAge()
                    + "\nNationality: " + PdfReportStyle.country(player)
                    + " | Position: " + PdfReportStyle.position(player.getPosition())
                    + " | Overall rating: " + PdfReportStyle.number(player.getOverall())).setFontSize(9));
            Table totals = PdfReportStyle.table(new float[]{1, 1, 1, 1, 1, 1, 1},
                    "Matches", "Minutes", "Goals", "Pen. goals", "Assists", "Yellows", "Reds");
            PdfReportStyle.row(totals,
                    hasCompleteStartingLineups ? row.matches() : "Not available",
                    hasCompleteStartingLineups ? row.minutes() : "Not available",
                    row.goals(),
                    hasCompleteGoalInformation ? row.penaltyGoals() : "Not available",
                    hasCompleteGoalInformation ? row.assists() : "Not available",
                    row.yellowCards(), row.redCards());
            card.add(new Paragraph("This championship").simulateBold().setFontSize(9).setMarginBottom(3));
            card.add(totals);
            if (player.getPosition() == Position.GOALKEEPER) {
                if (hasCompleteStartingLineups && hasCompleteGoalInformation) {
                    card.add(new Paragraph("Goals conceded: " + row.goalsConceded()
                            + " | Goals conceded per match: " + PdfReportStyle.number(row.averageGoalsConceded()))
                            .simulateBold().setFontSize(9));
                } else {
                    card.add(new Paragraph("Goals conceded: Not available | Goals conceded per match: Not available")
                            .simulateBold().setFontSize(9));
                }
            }
            card.add(new Paragraph(skills(data, player)).setFontSize(9).setMarginBottom(4));
            card.add(new Paragraph("Available: " + (player.isAvailable() ? "Yes" : "No")
                    + " | Suspension remaining: " + player.getSuspensionMatchesLeft() + " matches"
                    + " | Injury recovery: " + player.getInjuryMatchesLeft() + " matches").setFontSize(8));
            card.add(new Paragraph("Registered player totals - Matches: " + player.getMatchesPlayed()
                    + ", minutes: " + player.getMinutesPlayed() + ", goals: " + player.getGoals()
                    + ", assists: " + player.getAssists() + ", yellows: " + player.getYellowCards()
                    + ", reds: " + player.getRedCards()).setFontSize(8));
            document.add(card);
        }
    }

    private static String skills(ReportData data, Player player) {
        if (data.getPlayerSkills(player).isEmpty()) return "Individual skill details: not supplied.";
        return "Skills (0-100) - " + data.getPlayerSkills(player).entrySet().stream()
                .map(skill -> skill.getKey().replaceAll("([a-z])([A-Z])", "$1 $2") + ": " + skill.getValue())
                .collect(java.util.stream.Collectors.joining(" | "));
    }
}
