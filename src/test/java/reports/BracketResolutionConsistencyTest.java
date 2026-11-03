package reports;

import main.loader.TournamentData;
import model.match.FinalMatch;
import model.match.FirstLegMatch;
import model.match.GroupMatch;
import model.match.Match;
import model.match.ResolutionCriterion;
import model.match.SecondLegMatch;
import model.participant.Team;
import model.tournament.Group;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

// Comprueba que el cuadro publicado coincida con los ganadores y criterios del modelo,
// incluidos los resultados administrativos y las resoluciones pendientes.

class BracketResolutionConsistencyTest {
    private static final LocalDate MATCH_DATE = LocalDate.of(2026, 10, 1);

    @Test
    void reportDistinguishesAggregateVictoryFromAwayGoalsTieBreak() {
        ReportData data = completedGroups();
        BracketReport.BracketNode quarter = new BracketReport(data).getNodes().getFirst();
        FirstLegMatch first = firstLeg(quarter.homeTeam(), quarter.awayTeam(), 3, 0);
        SecondLegMatch second = new SecondLegMatch(quarter.awayTeam(), quarter.homeTeam(), null, null,
                MATCH_DATE.plusDays(7), first);
        score(second, 1, 0);
        data.addMatch(second);
        BracketReport.BracketNode aggregate = new BracketReport(data).getNodes().getFirst();
        assertSame(quarter.homeTeam(), aggregate.winner());
        assertEquals("Aggregate goals: 3 - 1", aggregate.resolution());
        score(first, 1, 0);
        score(second, 2, 1);
        BracketReport.BracketNode awayGoals = new BracketReport(data).getNodes().getFirst();
        assertSame(quarter.homeTeam(), awayGoals.winner());
        assertEquals("Away goals break aggregate tie: 2 - 2", awayGoals.resolution());
    }

    @Test
    void forfeitedFinalPublishesAdministrativeCriterionAndActualChampion() {
        ReportData data = tournamentAwaitingFinal();
        BracketReport.BracketNode pending = new BracketReport(data).getNodes().get(6);
        FinalMatch match = new FinalMatch(pending.homeTeam(), pending.awayTeam(), null, null, MATCH_DATE);
        match.forfeit(match.getHomeTeam());
        data.addMatch(match);
        BracketReport report = new BracketReport(data);
        BracketReport.BracketNode finalNode = report.getNodes().get(6);
        assertEquals(ResolutionCriterion.FORFEIT, match.getResolutionCriterion());
        assertSame(match.getWinner(), finalNode.winner());
        assertSame(match.getAwayTeam(), report.getChampion());
        assertEquals("Completed", finalNode.status());
        assertEquals(ResolutionCriterion.FORFEIT.getDescription(), finalNode.resolution());
        assertEquals(0, finalNode.finalMatch().homeGoals());
        assertEquals(3, finalNode.finalMatch().awayGoals());
    }

    @Test
    void administrativeTieDoesNotAdvertiseAPenaltyShootoutThatCannotBePlayed() {
        ReportData data = completedGroups();
        BracketReport.BracketNode quarter = new BracketReport(data).getNodes().getFirst();
        FirstLegMatch first = firstLeg(quarter.homeTeam(), quarter.awayTeam(), 3, 0);
        SecondLegMatch second = new SecondLegMatch(quarter.awayTeam(), quarter.homeTeam(), null, null,
                MATCH_DATE.plusDays(7), first);
        second.forfeit(quarter.homeTeam());
        data.addMatch(second);
        BracketReport.BracketNode result = new BracketReport(data).getNodes().getFirst();
        assertEquals(ResolutionCriterion.UNRESOLVED, second.getResolutionCriterion());
        assertFalse(second.requiresTieBreak());
        assertNull(second.getWinner());
        assertNull(result.winner());
        assertEquals("Unresolved tie", result.status());
        assertTrue(result.resolution().contains("resolution pending"));
        assertFalse(result.resolution().toLowerCase(Locale.ROOT).contains("penalty"));
    }

    @Test
    void reportPreservesTheSeriesAdvantageWhenTheWinnerForfeitsTheReturnLeg() {
        ReportData data = completedGroups();
        BracketReport.BracketNode quarter = new BracketReport(data).getNodes().getFirst();
        FirstLegMatch first = firstLeg(quarter.homeTeam(), quarter.awayTeam(), 10, 0);
        SecondLegMatch second = new SecondLegMatch(quarter.awayTeam(), quarter.homeTeam(), null, null,
                MATCH_DATE.plusDays(7), first);
        second.forfeit(quarter.homeTeam());
        data.addMatch(second);
        BracketReport.BracketNode result = new BracketReport(data).getNodes().getFirst();
        assertEquals(ResolutionCriterion.AGGREGATE_GOALS, second.getResolutionCriterion());
        assertSame(second.getWinner(), result.winner());
        assertSame(quarter.homeTeam(), result.winner());
        assertEquals("Aggregate goals: 10 - 3", result.resolution());
    }

    @Test
    void finalReportKeepsPartialOrTiedPenaltiesPendingUntilTheModelHasAWinner() {
        ReportData data = tournamentAwaitingFinal();
        BracketReport.BracketNode pending = new BracketReport(data).getNodes().get(6);
        FinalMatch match = new FinalMatch(pending.homeTeam(), pending.awayTeam(), null, null, MATCH_DATE);
        score(match, 1, 1);
        match.setHomePenalties(4);
        data.addMatch(match);
        BracketReport.BracketNode partial = new BracketReport(data).getNodes().get(6);
        assertNull(partial.winner());
        assertEquals("Penalty shootout pending", partial.resolution());
        match.setAwayPenalties(4);
        BracketReport.BracketNode tied = new BracketReport(data).getNodes().get(6);
        assertNull(tied.winner());
        assertEquals("Penalty shootout pending", tied.resolution());
        match.setAwayPenalties(5);
        BracketReport.BracketNode resolved = new BracketReport(data).getNodes().get(6);
        assertEquals(ResolutionCriterion.PENALTIES, match.getResolutionCriterion());
        assertSame(match.getWinner(), resolved.winner());
        assertEquals("Penalty shootout", resolved.resolution());
    }

    private static ReportData tournamentAwaitingFinal() {
        ReportData data = completedGroups();
        for (BracketReport.BracketNode quarter : new BracketReport(data).getNodes().subList(0, 4)) {
            addDecidedTie(data, quarter.homeTeam(), quarter.awayTeam());
        }
        for (BracketReport.BracketNode semi : new BracketReport(data).getNodes().subList(4, 6)) {
            addDecidedTie(data, semi.homeTeam(), semi.awayTeam());
        }
        return data;
    }

    private static ReportData completedGroups() {
        List<Team> allTeams = new ArrayList<>();
        ReportData data = new ReportData(new TournamentData(allTeams, List.of()));
        for (int zone = 1; zone <= 4; zone++) {
            List<Team> teams = new ArrayList<>();
            for (int place = 1; place <= 4; place++) {
                teams.add(new Team("Z" + zone + "T" + place, null, 1, null, 0, 0, 0, null));
            }
            allTeams.addAll(teams);
            Group group = new Group("Group " + zone, teams);
            for (int home = 0; home < 4; home++) {
                for (int away = home + 1; away < 4; away++) {
                    GroupMatch match = new GroupMatch(teams.get(home), teams.get(away), null, null, MATCH_DATE);
                    score(match, 1, 0);
                    group.addMatch(match);
                }
            }
            data.addGroup(group);
        }
        return data;
    }

    private static void addDecidedTie(ReportData data, Team home, Team away) {
        FirstLegMatch first = firstLeg(home, away, 1, 0);
        SecondLegMatch second = new SecondLegMatch(away, home, null, null, MATCH_DATE.plusDays(7), first);
        score(second, 0, 1);
        data.addMatch(second);
    }

    private static FirstLegMatch firstLeg(Team home, Team away, int homeGoals, int awayGoals) {
        FirstLegMatch match = new FirstLegMatch(home, away, null, null, MATCH_DATE);
        score(match, homeGoals, awayGoals);
        return match;
    }

    private static void score(Match match, int homeGoals, int awayGoals) {
        match.setHomeGoals(homeGoals);
        match.setAwayGoals(awayGoals);
        match.setPlayed(true);
    }
}
