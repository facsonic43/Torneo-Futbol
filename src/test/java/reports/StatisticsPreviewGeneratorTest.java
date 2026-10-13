package reports;

import main.loader.TournamentData;
import model.match.Goal;
import model.match.GroupMatch;
import model.match.Match;
import model.match.PenaltyTaken;
import model.match.Substitution;
import model.match.YellowCard;
import model.participant.Coach;
import model.participant.Country;
import model.participant.FieldPlayer;
import model.participant.Player;
import model.participant.Position;
import model.participant.Team;
import model.tournament.Group;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import reports.pdf.StatisticsPreviewGenerator;
import reports.pdf.StatisticsPreviewGenerator.Section;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsPreviewGeneratorTest {
    private static final Country COUNTRY = new Country("Argentina");
    private static final LocalDate BIRTH = LocalDate.of(2000, 1, 1);

    @ParameterizedTest
    @EnumSource(Section.class)
    void eachSelectedReportProducesReadableAndRenderablePages(Section section) throws Exception {
        byte[] bytes = StatisticsPreviewGenerator.generate(fixture(), section);

        try (PDDocument document = Loader.loadPDF(bytes)) {
            assertTrue(document.getNumberOfPages() > 0);
            String text = new PDFTextStripper().getText(document);
            String heading = switch (section) {
                case TOP_SCORERS -> "Top scorers";
                case FAIR_PLAY -> "Fair Play ranking";
                case MOST_MINUTES -> "Most minutes played";
                case BRACKET -> "Championship bracket";
            };
            assertTrue(text.contains(heading), text);
            assertFalse(text.contains("Participant identification cards"));

            PDFRenderer renderer = new PDFRenderer(document);
            PDFTextStripper pageReader = new PDFTextStripper();
            for (int index = 0; index < document.getNumberOfPages(); index++) {
                pageReader.setStartPage(index + 1);
                pageReader.setEndPage(index + 1);
                String pageText = pageReader.getText(document)
                        .replaceAll("Continental Cup \\| Reports \\| Page \\d+", "");
                assertFalse(pageText.isBlank(), "Blank report page " + (index + 1));
                BufferedImage page = renderer.renderImage(index);
                try {
                    assertTrue(page.getWidth() > 100 && page.getHeight() > 100);
                } finally {
                    page.flush();
                }
            }
        }
    }

    @Test
    void scorersUsePlayedMatchGoalsAndShareRanksWithoutHistoricalOrShootoutGoals() throws Exception {
        String text = text(fixture(), Section.TOP_SCORERS);

        assertTrue(text.contains("1 Ana Alpha 2 1"), text);
        assertTrue(text.contains("1 Bruno Beta 2 0"), text);
        assertTrue(text.contains("3 Carla Alpha 1 0"), text);
        assertTrue(text.indexOf("Ana Alpha") < text.indexOf("Bruno Beta"));
        assertTrue(text.indexOf("Bruno Beta") < text.indexOf("Carla Alpha"));
        assertFalse(text.contains("Historic star"), "An unplayed goal must not put a player in the ranking");
        assertFalse(text.contains("Not available"));
    }

    @Test
    void fairPlayUsesMatchCardsAndSharesRanksIncludingTeamsWithoutPlayedMatches() throws Exception {
        String text = text(fixture(), Section.FAIR_PLAY);

        assertTrue(text.contains("1 Delta 0 0 0 0"), text);
        assertTrue(text.contains("1 Gamma 0 0 0 0"), text);
        assertTrue(text.contains("3 Alpha 1 1 0 1"), text);
        assertTrue(text.contains("3 Beta 1 1 0 1"), text);
        assertTrue(text.indexOf("Delta 0") < text.indexOf("Alpha 1"));
    }

    @Test
    void minutesUseLineupsAndSubstitutionsAndPreserveCompetitionRanks() throws Exception {
        String text = text(fixture(), Section.MOST_MINUTES);

        assertTrue(text.contains("1 Ana Alpha 1 90"), text);
        assertTrue(text.contains("1 Bruno Beta 1 90"), text);
        assertTrue(text.contains("3 Carla Alpha 1 60"), text);
        assertTrue(text.contains("4 Historic star Alpha 1 30"), text);
        assertFalse(text.contains("9000"), "Historical minutes must not appear in this championship");
    }

    @Test
    void bracketShowsPartialQualificationAndPendingChampion() throws Exception {
        String text = text(fixture(), Section.BRACKET);

        assertTrue(text.contains("1 / Group A Pending Pending 1/6 In progress"), text);
        assertTrue(text.contains("Quarter-final"), text);
        assertTrue(text.contains("Semi-final"), text);
        assertTrue(text.contains("Champion: Pending"), text);
        assertTrue(text.contains("Match details"), text);
    }

    @ParameterizedTest
    @EnumSource(Section.class)
    void reportsExplainEmptyTournamentState(Section section) throws Exception {
        ReportData empty = new ReportData(new TournamentData(List.of(), List.of()));
        String text = text(empty, section);

        String expected = switch (section) {
            case TOP_SCORERS -> "No goals have been scored in this championship.";
            case FAIR_PLAY -> "No teams registered.";
            case MOST_MINUTES -> "No players registered.";
            case BRACKET -> "Champion: Pending";
        };
        assertTrue(text.contains(expected), text);
        if (section == Section.BRACKET) assertTrue(text.contains("Not drawn"), text);
    }

    @Test
    void missingStartingLineupsDoNotPresentHistoricalMinutesAsKnownResults() throws Exception {
        ReportData data = fixture();
        Match missingLineup = new GroupMatch(data.getTeams().get(2), data.getTeams().get(3),
                null, null, LocalDate.of(2026, 1, 3));
        missingLineup.setPlayed(true);
        data.addMatch(missingLineup);

        String text = text(data, Section.MOST_MINUTES);

        assertTrue(text.contains("Not available: starting lineups were not supplied for one or more completed matches."), text);
        assertFalse(text.contains("Ana"), "An incomplete tournament must not show a seemingly complete minutes ranking");
        assertFalse(text.contains("9000"));
    }

    @Test
    void generatingEveryPreviewPreservesAllSerializedDomainState() throws Exception {
        ReportData data = fixture();
        byte[] before = domainSnapshot(data);

        for (Section section : Section.values()) {
            StatisticsPreviewGenerator.generate(data, section);
            assertArrayEquals(before, domainSnapshot(data), "Preview changed domain state: " + section);
        }
    }

    private static String text(ReportData data, Section section) throws IOException {
        try (PDDocument document = Loader.loadPDF(StatisticsPreviewGenerator.generate(data, section))) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ").trim();
        }
    }

    private static byte[] domainSnapshot(ReportData data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(new ArrayList<>(data.getTeams()));
            output.writeObject(new ArrayList<>(data.getReferees()));
            output.writeObject(new ArrayList<>(data.getGroups()));
            output.writeObject(new ArrayList<>(data.getMatches()));
        }
        return bytes.toByteArray();
    }

    private static ReportData fixture() {
        Team home = team("Alpha", 101);
        Team away = team("Beta", 102);
        Team gamma = team("Gamma", 103);
        Team delta = team("Delta", 104);
        Player ana = player("Ana", 201);
        Player bruno = player("Bruno", 202);
        Player carla = player("Carla", 203);
        Player historical = player("Historic star", 204);
        List.of(ana, carla, historical).forEach(home::addPlayer);
        away.addPlayer(bruno);
        historical.injure(2);
        historical.addRedCard();
        ReportData data = new ReportData(new TournamentData(List.of(home, away, gamma, delta), List.of()));

        GroupMatch played = new GroupMatch(home, away, null, null, LocalDate.of(2026, 1, 1));
        played.setStartingLineups(List.of(ana, carla), List.of(bruno));
        addGoal(data, played, new Goal(10, home, ana, null, true, false, null));
        addGoal(data, played, new Goal(20, away, bruno, null, false, false, null));
        addGoal(data, played, new Goal(30, home, ana, null, false, false, null));
        addGoal(data, played, new Goal(40, away, bruno, null, false, false, null));
        addGoal(data, played, new Goal(50, home, carla, null, false, false, null));
        played.addEvent(new Substitution(60, home, carla, historical));
        addGoal(data, played, new Goal(70, home, bruno, null, false, true, null));
        played.addEvent(new YellowCard(75, home, ana));
        played.addEvent(new YellowCard(80, away, bruno));
        played.addEvent(new PenaltyTaken(120, home, ana, true));
        played.setHomeGoals(4);
        played.setAwayGoals(2);
        played.setPlayed(true);

        Group group = new Group("Group A", new ArrayList<>(data.getTeams()));
        group.addMatch(played);
        data.addGroup(group);
        data.addMatch(played); // Registering the same match twice must not double the displayed totals.

        GroupMatch pending = new GroupMatch(home, gamma, null, null, LocalDate.of(2026, 1, 2));
        pending.addEvent(new Goal(10, home, historical, null, false, false, null));
        pending.setHomeGoals(1);
        data.addMatch(pending);
        return data;
    }

    private static void addGoal(ReportData data, Match match, Goal goal) {
        match.addEvent(goal);
        data.getMatchDetails(match).setGoalDetails(goal, goal.isPenalty(), goal.isOwnGoal(), null);
    }

    private static Team team(String name, int coachId) {
        return new Team(name, COUNTRY, 1,
                new Coach("Coach " + name, coachId, "DU", BIRTH.minusYears(20), COUNTRY, 3),
                4, 2, 3, null);
    }

    private static Player player(String name, int id) {
        return new FieldPlayer(name, id, "DU", BIRTH, COUNTRY, 100, 9000, 20, 5, 50, 25,
                Position.FORWARD, 70, 70, 70, 70, 70, 70, 70, 70);
    }
}
