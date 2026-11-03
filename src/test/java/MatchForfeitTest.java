import control.MatchSimulator;
import main.loader.TournamentData;
import model.match.FinalMatch;
import model.match.FirstLegMatch;
import model.match.GroupMatch;
import model.match.Match;
import model.match.ResolutionCriterion;
import model.match.SecondLegMatch;
import model.participant.Coach;
import model.participant.Country;
import model.participant.FieldPlayer;
import model.participant.Goalkeeper;
import model.participant.Player;
import model.participant.Position;
import model.participant.Referee;
import model.participant.Team;
import model.tournament.Group;
import model.tournament.Standing;
import org.junit.jupiter.api.Test;
import reports.ChampionshipStatistics;
import reports.ReportData;

import java.time.LocalDate;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica las derrotas administrativas por formación incompleta y su efecto en la tabla.
 * Los goles del resultado no crean incidencias ni modifican las estadísticas de los jugadores.
 */
class MatchForfeitTest {
    private static final LocalDate MATCH_DATE = LocalDate.of(2026, 10, 1);

    @Test
    void homeTeamWithTenPlayersLosesThreeNilWithoutPlayerStatistics() {
        Team home = team("Home", 1, 4, 4, 1);
        Team away = team("Away", 1, 4, 4, 2);
        GroupMatch match = new GroupMatch(home, away, null, null, MATCH_DATE);

        new MatchSimulator(1L).simulateMatch(match);

        assertAdministrativeScore(match, 0, 3, away);
        Group group = new Group("A", List.of(home, away));
        group.addMatch(match);
        Standing winner = group.getStandings().getFirst();
        assertSame(away, winner.getTeam());
        assertEquals(3, winner.getPoints());
        assertEquals(3, winner.getScored());
        assertEquals(0, winner.getConceded());
        assertEquals(1, group.getStandings().get(1).getLost());
    }

    @Test
    void awayTeamWithTenPlayersLosesFinalWithoutPenaltyShootout() {
        Team home = team("Home", 1, 4, 4, 2);
        Team away = team("Away", 1, 4, 4, 1);
        FinalMatch match = new FinalMatch(home, away, null, null, MATCH_DATE);

        new MatchSimulator(2L).simulateMatch(match);

        assertAdministrativeScore(match, 3, 0, home);
        assertFalse(match.requiresTieBreak());
        assertNull(match.getHomePenalties());
        assertNull(match.getAwayPenalties());
    }

    @Test
    void elevenPlayersWhoCannotFillAnyFormationAlsoForfeit() {
        Team home = team("Home", 1, 10, 0, 0);
        Team away = team("Away", 1, 4, 4, 2);
        GroupMatch match = new GroupMatch(home, away, null, null, MATCH_DATE);

        new MatchSimulator(3L).simulateRegularTime(match);

        assertAdministrativeScore(match, 0, 3, away);
    }

    @Test
    void suspendedPlayerCannotCompleteTheLineupAndServesTheMissedMatch() {
        Team home = team("Home", 1, 4, 4, 2);
        Team away = team("Away", 1, 4, 4, 2);
        Player suspended = home.getSquad().getLast();
        suspended.addRedCard();
        GroupMatch match = new GroupMatch(home, away, null, null, MATCH_DATE);

        new MatchSimulator(4L).simulateMatch(match);

        assertAdministrativeScore(match, 0, 3, away);
        assertFalse(suspended.isSuspended());
    }

    @Test
    void completeAlternativeFormationIsPlayedNormally() {
        Team home = team("Home", 1, 4, 4, 2);
        Team away = team("Away", 1, 4, 4, 2);
        GroupMatch match = new GroupMatch(home, away, null, null, MATCH_DATE);

        new MatchSimulator(5L).simulateRegularTime(match);

        assertTrue(match.isPlayed());
        assertEquals(11, match.getHomeStarters().size());
        assertEquals(11, match.getAwayStarters().size());
        assertFalse(match.getPlayerParticipations().isEmpty());
        assertTrue(home.getSquad().stream().anyMatch(player -> player.getMinutesPlayed() > 0));
    }

    @Test
    void anExtraFieldPlayerCanStillCompleteTheLineupAsEmergencyGoalkeeper() {
        Team home = team("Home", 0, 5, 4, 2);
        Team away = team("Away", 1, 4, 4, 2);
        GroupMatch match = new GroupMatch(home, away, null, null, MATCH_DATE);

        new MatchSimulator(6L).simulateRegularTime(match);

        assertTrue(match.isPlayed());
        assertEquals(11, match.getHomeStarters().size());
        assertFalse(match.getPlayerParticipations().isEmpty());
    }

    @Test
    void simulatingAnAwardedMatchAgainDoesNotChangeItsResult() {
        Team home = team("Home", 1, 4, 4, 1);
        Team away = team("Away", 1, 4, 4, 2);
        GroupMatch match = new GroupMatch(home, away, null, null, MATCH_DATE);
        MatchSimulator simulator = new MatchSimulator(7L);

        simulator.simulateMatch(match);
        simulator.simulateMatch(match);

        assertAdministrativeScore(match, 0, 3, away);
    }

    @Test
    void administrativeResultIsCompleteReportDataWithoutPlayerOrRefereeActivity() {
        Team home = team("Home", 1, 4, 4, 1);
        Team away = team("Away", 1, 4, 4, 2);
        Referee referee = new Referee("Referee", 100, "DNI", MATCH_DATE.minusYears(40),
                new Country("Neutral"), 12, 10);
        GroupMatch match = new GroupMatch(home, away, referee, null, MATCH_DATE);
        ReportData data = new ReportData(new TournamentData(List.of(home, away), List.of(referee)));

        new MatchSimulator(8L).simulateMatch(match);
        data.addMatch(match);
        ChampionshipStatistics statistics = new ChampionshipStatistics(data);

        assertEquals(1, statistics.getPlayedMatchCount());
        assertEquals(0, statistics.getIncompleteMatchCount());
        assertTrue(statistics.hasCompleteStartingLineups());
        assertTrue(statistics.hasCompleteGoalInformation());
        assertTrue(statistics.getTopScorers().isEmpty());
        for (ChampionshipStatistics.PlayerStats player : statistics.getMinutesRanking()) {
            assertEquals(0, player.matches());
            assertEquals(0, player.minutes());
            assertEquals(0, player.goals());
            assertEquals(0, player.goalsConceded());
        }
        ChampionshipStatistics.TeamStats winner = statistics.getTeamsAlphabetically().stream()
                .filter(stats -> stats.team() == away).findFirst().orElseThrow();
        assertEquals(1, winner.played());
        assertEquals(1, winner.won());
        assertEquals(3, winner.points());
        assertEquals(3, winner.goalsFor());
        assertEquals(0, winner.goalsAgainst());
        assertEquals(0, statistics.getRefereeRanking().getFirst().matches());
        assertEquals(12, referee.getMatchesOfficiated());
    }

    @Test
    void bothIncompleteTeamsLeaveMatchAndAbsencesUnchanged() {
        Team home = team("Home", 1, 4, 4, 2);
        Team away = team("Away", 1, 4, 4, 1);
        Player suspended = home.getSquad().getLast();
        Player injured = away.getSquad().getLast();
        suspended.addRedCard();
        injured.injure(2);
        GroupMatch match = new GroupMatch(home, away, null, null, MATCH_DATE);
        match.setHomeGoals(1);
        match.setAwayGoals(2);

        assertThrows(IllegalStateException.class, () -> new MatchSimulator(9L).simulateMatch(match));

        assertFalse(match.isPlayed());
        assertFalse(match.isForfeited());
        assertEquals(1, match.getHomeGoals());
        assertEquals(2, match.getAwayGoals());
        assertEquals(ResolutionCriterion.NOT_PLAYED, match.getResolutionCriterion());
        assertNull(match.getWinner());
        assertTrue(match.getEvents().isEmpty());
        assertTrue(match.getPlayerParticipations().isEmpty());
        assertTrue(match.getHomeStarters().isEmpty());
        assertTrue(match.getAwayStarters().isEmpty());
        assertEquals(1, suspended.getSuspensionMatchesLeft());
        assertEquals(2, injured.getInjuryMatchesLeft());
    }

    @Test
    void forfeitedSecondLegContributesThreePointsToTheSeries() {
        Team a = team("A", 1, 4, 4, 1);
        Team b = team("B", 1, 4, 4, 2);
        FirstLegMatch first = playedFirstLeg(a, b, 0, 1);
        SecondLegMatch second = new SecondLegMatch(b, a, null, null, MATCH_DATE.plusDays(7), first);

        new MatchSimulator(10L).simulateMatch(second);

        assertTrue(second.isForfeited());
        assertEquals(3, second.getHomeGoals());
        assertEquals(0, second.getAwayGoals());
        assertEquals(6, second.getSeriesPoints(b));
        assertEquals(0, second.getSeriesPoints(a));
        assertSame(b, second.getWinner());
        assertEquals(ResolutionCriterion.SERIES_POINTS, second.getResolutionCriterion());
        assertTrue(second.getEvents().isEmpty());
    }

    @Test
    void forfeitingTheSecondLegDoesNotEraseTheFirstLegAdvantage() {
        Team a = team("A", 1, 4, 4, 1);
        Team b = team("B", 1, 4, 4, 2);
        FirstLegMatch first = playedFirstLeg(a, b, 10, 0);
        SecondLegMatch second = new SecondLegMatch(b, a, null, null, MATCH_DATE.plusDays(7), first);

        new MatchSimulator(11L).simulateMatch(second);

        assertTrue(second.isForfeited());
        assertEquals(3, second.getHomeGoals());
        assertEquals(0, second.getAwayGoals());
        assertEquals(3, second.getSeriesPoints(a));
        assertEquals(3, second.getSeriesPoints(b));
        assertEquals(10, second.getWeightedGoals(a));
        assertEquals(3, second.getWeightedGoals(b));
        assertSame(a, second.getWinner());
        assertEquals(ResolutionCriterion.AGGREGATE_GOALS, second.getResolutionCriterion());
        assertTrue(second.getEvents().isEmpty());
    }

    @Test
    void tiedSeriesWithForfeitedSecondLegRemainsUnresolvedWithoutShootout() {
        Team a = team("A", 1, 4, 4, 1);
        Team b = team("B", 1, 4, 4, 2);
        FirstLegMatch first = playedFirstLeg(a, b, 3, 0);
        SecondLegMatch second = new SecondLegMatch(b, a, null, null, MATCH_DATE.plusDays(7), first);
        MatchSimulator simulator = new MatchSimulator(12L);

        simulator.simulateMatch(second);

        assertTrue(second.isPlayed());
        assertTrue(second.isForfeited());
        assertNull(second.getWinner());
        assertEquals(ResolutionCriterion.UNRESOLVED, second.getResolutionCriterion());
        assertFalse(second.requiresTieBreak());
        assertNull(second.getHomePenalties());
        assertNull(second.getAwayPenalties());
        assertTrue(second.getEvents().isEmpty());
        assertThrows(IllegalStateException.class, () -> simulator.simulatePenaltyShootout(second));
    }

    private static FirstLegMatch playedFirstLeg(Team home, Team away, int homeGoals, int awayGoals) {
        FirstLegMatch match = new FirstLegMatch(home, away, null, null, MATCH_DATE);
        match.setHomeGoals(homeGoals);
        match.setAwayGoals(awayGoals);
        match.setPlayed(true);
        return match;
    }

    private static void assertAdministrativeScore(Match match, int homeGoals, int awayGoals, Team winner) {
        assertTrue(match.isPlayed());
        assertEquals(homeGoals, match.getHomeGoals());
        assertEquals(awayGoals, match.getAwayGoals());
        assertSame(winner, match.getWinner());
        assertTrue(match.isForfeited());
        assertEquals(ResolutionCriterion.FORFEIT, match.getResolutionCriterion());
        assertEquals("FORFEIT", match.getResolutionCriteria());
        assertTrue(match.getEvents().isEmpty());
        assertTrue(match.getPlayerParticipations().isEmpty());
        for (Team team : List.of(match.getHomeTeam(), match.getAwayTeam())) {
            for (Player player : team.getSquad()) {
                assertEquals(0, player.getGoals());
                assertEquals(0, player.getAssists());
                assertEquals(0, player.getGoalsConceded());
                assertEquals(0, player.getMatchesPlayed());
                assertEquals(0, player.getMinutesPlayed());
            }
        }
    }

    @Test
    void savedAdministrativeResultKeepsItsCriterionWithoutInventingPlayerStatistics() throws Exception {
        GroupMatch match = new GroupMatch(team("Home", 1, 4, 4, 1), team("Away", 1, 4, 4, 2),
                null, null, MATCH_DATE);
        new MatchSimulator(13L).simulateMatch(match);
        ByteArrayOutputStream saved = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(saved)) {
            output.writeObject(match);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(saved.toByteArray()))) {
            Match restored = (Match) input.readObject();
            assertAdministrativeScore(restored, 0, 3, restored.getAwayTeam());
        }
    }

    private static Team team(String name, int goalkeepers, int defenders, int midfielders, int forwards) {
        Country country = new Country(name);
        Coach coach = new Coach(name + " Coach", 1, "DNI", MATCH_DATE.minusYears(40), country, 0);
        Team team = new Team(name, country, 1, coach, 0, 0, 0, null);
        for (int i = 0; i < goalkeepers; i++) {
            team.addPlayer(new Goalkeeper(name + " GK " + i, i, "DNI", MATCH_DATE.minusYears(25),
                    country, 0, 0, 0, 0, 0, 0, 70, 70, 70, 70, 70, 70));
        }
        addFieldPlayers(team, Position.DEFENDER, defenders);
        addFieldPlayers(team, Position.MIDFIELDER, midfielders);
        addFieldPlayers(team, Position.FORWARD, forwards);
        return team;
    }

    private static void addFieldPlayers(Team team, Position position, int count) {
        for (int i = 0; i < count; i++) {
            int id = team.getSquad().size();
            team.addPlayer(new FieldPlayer(team.getName() + " " + position + " " + i, id, "DNI",
                    MATCH_DATE.minusYears(25), team.getCountry(), 0, 0, 0, 0, 0, 0, position,
                    70, 70, 70, 70, 70, 70, 70, 70));
        }
    }
}
