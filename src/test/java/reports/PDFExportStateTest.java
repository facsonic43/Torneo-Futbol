package reports;

import main.loader.TournamentData;
import model.match.*;
import model.participant.*;
import model.tournament.Group;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reports.pdf.PDFGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PDFExportStateTest {
    private static final Country COUNTRY = new Country("Argentina");
    private static final LocalDate BIRTH = LocalDate.of(2000, 1, 1);

    @TempDir Path directory;

    @Test
    void repeatedReportExportsPreserveStatisticsAndEverySerializedDomainField() throws Exception {
        Fixture fixture = fixture();
        assertExpectedStatistics(fixture);
        StatisticsSnapshot statisticsBefore = statisticsSnapshot(fixture.reports());
        byte[] stateBefore = domainSnapshot(fixture.reports());

        for (int export = 0; export < 3; export++) {
            Path output = directory.resolve("report.pdf");
            PDFGenerator.generate(fixture.reports(), output, new ReportOptions(null, null, directory));
            assertTrue(Files.size(output) > 0);
            assertUnchanged(fixture, statisticsBefore, stateBefore);
        }
    }

    @Test
    void frontendTournamentDataOverloadPreservesExistingMatchesAndRankings() throws Exception {
        Fixture fixture = fixture();
        assertExpectedStatistics(fixture);
        StatisticsSnapshot statisticsBefore = statisticsSnapshot(fixture.reports());
        byte[] stateBefore = domainSnapshot(fixture.reports());

        for (int export = 0; export < 2; export++) {
            Path output = directory.resolve("frontend.pdf");
            PDFGenerator.generate(fixture.participants(), output, new ReportOptions(null, null, directory));
            assertTrue(Files.size(output) > 0);
            assertUnchanged(fixture, statisticsBefore, stateBefore);
        }
    }

    @Test
    void failedExportPreservesStatisticsDomainStateAndPreviousPdf() throws Exception {
        Fixture fixture = fixture();
        assertExpectedStatistics(fixture);
        StatisticsSnapshot statisticsBefore = statisticsSnapshot(fixture.reports());
        byte[] stateBefore = domainSnapshot(fixture.reports());
        Path output = directory.resolve("report.pdf");
        ReportOptions options = new ReportOptions(null, null, directory);
        PDFGenerator.generate(fixture.reports(), output, options);
        byte[] previousPdf = Files.readAllBytes(output);
        Files.writeString(directory.resolve("DU-300.jpg"), "invalid photograph");

        assertThrows(IOException.class, () -> PDFGenerator.generate(fixture.reports(), output, options));

        assertArrayEquals(previousPdf, Files.readAllBytes(output));
        assertUnchanged(fixture, statisticsBefore, stateBefore);
        try (var files = Files.list(directory)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".championship-report-")));
        }
    }

    private static void assertUnchanged(Fixture fixture, StatisticsSnapshot before, byte[] stateBefore)
            throws IOException {
        // Check the actual shared model before rebuilding derived statistics.
        assertArrayEquals(stateBefore, domainSnapshot(fixture.reports()),
                "PDF export changed persisted participant, group, match, lineup, participation or event state");
        assertEquals(before, statisticsSnapshot(fixture.reports()), "PDF export changed a ranking or calculation");
        assertExpectedStatistics(fixture);
    }

    private static void assertExpectedStatistics(Fixture fixture) {
        ChampionshipStatistics stats = new ChampionshipStatistics(fixture.reports());
        assertEquals(2, stats.getPlayedMatchCount());
        assertEquals(0, stats.getIncompleteMatchCount());
        assertTrue(stats.hasCompleteStartingLineups());
        assertTrue(stats.hasCompleteGoalInformation());

        ChampionshipStatistics.PlayerStats scorer = stats.getPlayerStats(fixture.scorer());
        assertEquals(2, scorer.matches());
        assertEquals(210, scorer.minutes()); // 90 minutes plus the 120-minute final.
        assertEquals(2, scorer.goals()); // Own goals and shootout penalties do not count.
        assertEquals(1, scorer.penaltyGoals());
        assertEquals(0, scorer.assists());
        assertEquals(List.of(fixture.scorer(), fixture.opponent()),
                stats.getTopScorers().stream().map(ChampionshipStatistics.PlayerStats::player).toList());
        assertEquals(1, stats.getPlayerStats(fixture.opponent()).goals());
        assertEquals(1, stats.getPlayerStats(fixture.assister()).assists());
        assertEquals(80, stats.getPlayerStats(fixture.assister()).minutes());
        assertEquals(75, stats.getPlayerStats(fixture.opponent()).minutes());
        assertEquals(2, stats.getPlayerStats(fixture.opponent()).yellowCards());
        assertEquals(1, stats.getPlayerStats(fixture.opponent()).redCards());
        assertEquals(0, stats.getPlayerStats(fixture.bench()).matches());
        assertEquals(0, stats.getPlayerStats(fixture.bench()).minutes());

        ChampionshipStatistics.PlayerStats homeKeeper = stats.getPlayerStats(fixture.homeKeeper());
        ChampionshipStatistics.PlayerStats awayKeeper = stats.getPlayerStats(fixture.awayKeeper());
        ChampionshipStatistics.PlayerStats replacement = stats.getPlayerStats(fixture.replacement());
        assertEquals(210, homeKeeper.minutes());
        assertEquals(1, homeKeeper.goalsConceded());
        assertEquals(0.5, homeKeeper.averageGoalsConceded());
        assertEquals(60, awayKeeper.minutes());
        assertEquals(2, awayKeeper.goalsConceded());
        assertEquals(2.0, awayKeeper.averageGoalsConceded());
        assertEquals(150, replacement.minutes());
        assertEquals(1, replacement.goalsConceded());
        assertEquals(0.5, replacement.averageGoalsConceded());
        assertEquals(List.of(fixture.homeKeeper(), fixture.scorer(), fixture.replacement(),
                        fixture.assister(), fixture.opponent(), fixture.awayKeeper(), fixture.bench()),
                stats.getMinutesRanking().stream().map(ChampionshipStatistics.PlayerStats::player).toList());

        ChampionshipStatistics.TeamStats home = stats.getTeamsAlphabetically().getFirst();
        ChampionshipStatistics.TeamStats away = stats.getTeamsAlphabetically().getLast();
        assertSame(fixture.home(), home.team());
        assertEquals(2, home.played());
        assertEquals(1, home.won());
        assertEquals(1, home.drawn()); // Winning a shootout does not add a regular-time win.
        assertEquals(0, home.lost());
        assertEquals(4, home.points());
        assertEquals(3, home.goalsFor());
        assertEquals(1, home.goalsAgainst());
        assertEquals(66.66666666666667, home.efficiency(), 1e-10);
        assertEquals(4, home.fairPlayScore()); // One yellow plus one direct red.
        assertEquals(0, away.won());
        assertEquals(1, away.drawn());
        assertEquals(1, away.lost());
        assertEquals(1, away.points());
        assertEquals(1, away.goalsFor());
        assertEquals(3, away.goalsAgainst());
        assertEquals(16.666666666666668, away.efficiency(), 1e-10);
        assertEquals(5, away.fairPlayScore()); // Two yellows plus one red.
        assertEquals(List.of(fixture.home(), fixture.away()),
                stats.getFairPlayRanking().stream().map(ChampionshipStatistics.TeamStats::team).toList());
        assertEquals(2, stats.getRefereeRanking().getFirst().matches());
        assertEquals(0, stats.getRefereeRanking().getLast().matches());
        assertEquals(16.0, stats.getAverageRefereeExperience());
        assertEquals(2, stats.getRefereesWithKnownExperienceCount());

        // Historical counters intentionally differ from the current championship.
        assertEquals(50, fixture.scorer().getGoals());
        assertEquals(100, fixture.scorer().getMatchesPlayed());
        assertEquals(9000, fixture.scorer().getMinutesPlayed());
        assertEquals(100, fixture.participants().getReferees().getFirst().getMatchesOfficiated());
        assertEquals(2, fixture.bench().getInjuryMatchesLeft());
        assertEquals(1, fixture.bench().getSuspensionMatchesLeft());
    }

    private static StatisticsSnapshot statisticsSnapshot(ReportData data) {
        ChampionshipStatistics stats = new ChampionshipStatistics(data);
        return new StatisticsSnapshot(stats.getPlayedMatchCount(), stats.getIncompleteMatchCount(),
                stats.hasCompleteStartingLineups(), stats.hasCompleteGoalInformation(),
                stats.getPlayers(null, null), stats.getTopScorers(), stats.getMinutesRanking(),
                stats.getTeamsAlphabetically(), stats.getFairPlayRanking(), stats.getRefereeRanking(),
                stats.getTeamsAlphabetically().stream().map(ChampionshipStatistics.TeamStats::efficiency).toList(),
                stats.getTeamsAlphabetically().stream().map(ChampionshipStatistics.TeamStats::averageAge).toList(),
                stats.getPlayers(Position.GOALKEEPER, null).stream()
                        .map(ChampionshipStatistics.PlayerStats::averageGoalsConceded).toList(),
                stats.getAverageRefereeExperience(), stats.getRefereesWithKnownExperienceCount());
    }

    private static byte[] domainSnapshot(ReportData data) throws IOException {
        // These are the same Serializable domain objects used by tournament persistence.
        // Serializing them also checks private counters such as accumulated tournament yellows.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(new ArrayList<>(data.getTeams()));
            output.writeObject(new ArrayList<>(data.getReferees()));
            output.writeObject(new ArrayList<>(data.getGroups()));
            output.writeObject(new ArrayList<>(data.getMatches()));
        }
        return bytes.toByteArray();
    }

    private static Fixture fixture() {
        Team home = team("Alpha", 101);
        Team away = team("Beta", 102);
        Player scorer = player("Scorer", 201, Position.FORWARD);
        Player assister = player("Assister", 202, Position.MIDFIELDER);
        Player opponent = player("Opponent", 203, Position.DEFENDER);
        Goalkeeper homeKeeper = keeper("Home keeper", 204);
        Goalkeeper awayKeeper = keeper("Away keeper", 205);
        Goalkeeper replacement = keeper("Replacement keeper", 206);
        Player bench = player("Unused bench", 207, Position.FORWARD);
        bench.injure(2);
        bench.addYellowCard();
        bench.addRedCard();
        scorer.addPenaltyGoal();
        homeKeeper.addGoalConceded();
        List.of(scorer, assister, homeKeeper, bench).forEach(home::addPlayer);
        List.of(opponent, awayKeeper, replacement).forEach(away::addPlayer);
        Referee active = new Referee("Active referee", 300, "DU", BIRTH, COUNTRY, 100, 12);
        Referee unused = new Referee("Unused referee", 301, "DU", BIRTH, COUNTRY, 200, 20);
        TournamentData participants = new TournamentData(new ArrayList<>(List.of(home, away)),
                new ArrayList<>(List.of(active, unused)));
        ReportData data = new ReportData(participants);
        data.setRefereeExperience(active, 12);
        data.setRefereeExperience(unused, 20);

        GroupMatch match = new GroupMatch(home, away, active, null, LocalDate.of(2026, 1, 1));
        match.setStartingLineups(List.of(scorer, assister, homeKeeper), List.of(opponent, awayKeeper));
        match.setFormations(Formation.FOUR_FOUR_TWO, Formation.FOUR_THREE_THREE);
        addGoal(data, match, new Goal(10, home, scorer, assister, false, false, awayKeeper));
        addGoal(data, match, new Goal(20, away, opponent, null, false, false, homeKeeper));
        match.addEvent(new YellowCard(20, home, assister));
        match.addEvent(new YellowCard(25, away, opponent));
        // A stored assist on a penalty or own goal must not count as a real assist.
        addGoal(data, match, new Goal(40, home, scorer, assister, true, false, awayKeeper));
        match.addEvent(new Substitution(60, away, awayKeeper, replacement));
        addGoal(data, match, new Goal(70, home, opponent, scorer, false, true, replacement));
        match.addEvent(new YellowCard(75, away, opponent));
        match.addEvent(new RedCard(75, away, opponent, false));
        match.addEvent(new RedCard(80, home, assister, true));
        match.setHomeGoals(3);
        match.setAwayGoals(1);
        match.setPlayed(true);
        PlayerParticipation participation = new PlayerParticipation(replacement, away, false, 60);
        participation.setExitMinute(90);
        match.addPlayerParticipation(participation);
        Group group = new Group("A", new ArrayList<>(List.of(home, away)));
        group.addMatch(match);
        data.addGroup(group);
        data.addMatch(match); // The same game must be counted once.

        FinalMatch finalMatch = new FinalMatch(home, away, active, null, LocalDate.of(2026, 1, 10));
        finalMatch.setStartingLineups(List.of(scorer, homeKeeper), List.of(replacement));
        finalMatch.setExtraTimePlayed(true);
        finalMatch.setHomePenalties(5);
        finalMatch.setAwayPenalties(4);
        finalMatch.addEvent(new PenaltyTaken(120, home, scorer, true));
        finalMatch.addEvent(new PenaltyTaken(120, away, replacement, true));
        finalMatch.setPlayed(true);
        data.addMatch(finalMatch);

        GroupMatch pending = new GroupMatch(home, away, unused, null, LocalDate.of(2026, 1, 20));
        pending.setHomeGoals(10);
        pending.addEvent(new Goal(10, home, bench, null, false, false, awayKeeper));
        data.addMatch(pending); // Unplayed games must not affect any ranking.
        return new Fixture(participants, data, home, away, scorer, assister, opponent,
                homeKeeper, awayKeeper, replacement, bench);
    }

    private static void addGoal(ReportData data, Match match, Goal goal) {
        match.addEvent(goal);
        data.getMatchDetails(match).setGoalDetails(goal, goal.isPenalty(), goal.isOwnGoal(),
                goal.getGoalkeeperConceded());
    }

    private static Team team(String name, int coachId) {
        Coach coach = new Coach("Coach " + name, coachId, "DU", BIRTH.minusYears(20), COUNTRY, 3);
        return new Team(name, COUNTRY, 1, coach, 4, 2, 3, null);
    }

    private static Player player(String name, int id, Position position) {
        return new FieldPlayer(name, id, "DU", BIRTH, COUNTRY, 100, 9000, 20, 5, 50, 25,
                position, 70, 70, 70, 70, 70, 70, 70, 70);
    }

    private static Goalkeeper keeper(String name, int id) {
        return new Goalkeeper(name, id, "DU", BIRTH, COUNTRY, 100, 9000, 20, 5, 0, 25,
                70, 70, 70, 70, 70, 70);
    }

    private record Fixture(TournamentData participants, ReportData reports, Team home, Team away,
                           Player scorer, Player assister, Player opponent, Goalkeeper homeKeeper,
                           Goalkeeper awayKeeper, Goalkeeper replacement, Player bench) { }

    private record StatisticsSnapshot(int played, int incomplete, boolean completeLineups,
                                      boolean completeGoals, List<ChampionshipStatistics.PlayerStats> players,
                                      List<ChampionshipStatistics.PlayerStats> scorers,
                                      List<ChampionshipStatistics.PlayerStats> minutes,
                                      List<ChampionshipStatistics.TeamStats> teams,
                                      List<ChampionshipStatistics.TeamStats> fairPlay,
                                      List<ChampionshipStatistics.RefereeStats> referees,
                                      List<Double> efficiencies, List<Double> averageAges,
                                      List<Double> keeperAverages, double refereeExperience,
                                      int knownRefereeExperience) { }
}
