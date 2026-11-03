import model.match.FinalMatch;
import model.match.FirstLegMatch;
import model.match.GroupMatch;
import model.match.Match;
import model.match.ResolutionCriterion;
import model.match.SecondLegMatch;
import model.participant.Team;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Comprueba que ganador, criterio y necesidad de penales respeten el mismo orden de resolución.
// Cubre partidos pendientes, series por puntos y goles ponderados, y tandas incompletas o empatadas.

class MatchResolutionCriteriaTest {
    private static final LocalDate MATCH_DATE = LocalDate.of(2026, 10, 1);
    private final Team a = new Team("A", null, 1, null, 0, 0, 0, null);
    private final Team b = new Team("B", null, 2, null, 0, 0, 0, null);

    @Test
    void allMatchTypesExposeTheSamePendingCriterion() {
        FirstLegMatch first = firstLeg(0, 0);
        for (Match match : List.of(new GroupMatch(a, b, null, null, MATCH_DATE), new FirstLegMatch(a, b, null, null,
                MATCH_DATE), new SecondLegMatch(b, a, null, null, MATCH_DATE, first), new FinalMatch(a, b, null, null,
                MATCH_DATE))) {
            assertCriterion(match, ResolutionCriterion.NOT_PLAYED);
            assertNull(match.getWinner());
            assertFalse(match.requiresTieBreak());
        }
    }

    @Test
    void groupAndFirstLegUseConsistentRegularTimeAndDrawCriteria() {
        for (Match match : List.of(new GroupMatch(a, b, null, null, MATCH_DATE), new FirstLegMatch(a, b, null, null,
                MATCH_DATE))) {
            score(match, 2, 1);
            assertCriterion(match, ResolutionCriterion.REGULAR_TIME);
            assertSame(a, match.getWinner());
            score(match, 1, 1);
            assertCriterion(match, ResolutionCriterion.DRAW);
            assertNull(match.getWinner());
            assertFalse(match.requiresTieBreak());
        }
    }

    @Test
    void seriesCannotResolveBeforeBothLegsHaveBeenPlayed() {
        FirstLegMatch first = new FirstLegMatch(a, b, null, null, MATCH_DATE);
        SecondLegMatch second = new SecondLegMatch(b, a, null, null, MATCH_DATE.plusDays(7), first);
        score(second, 2, 0);
        assertNull(second.getWinner());
        assertFalse(second.requiresTieBreak());
        assertCriterion(second, ResolutionCriterion.NOT_PLAYED);
    }

    @Test
    void seriesPointsTakePriorityOverAnyRecordedPenalties() {
        FirstLegMatch first = firstLeg(1, 0);
        SecondLegMatch second = secondLeg(first, 0, 1);
        second.setHomePenalties(5);
        second.setAwayPenalties(0);
        assertSame(a, second.getWinner());
        assertCriterion(second, ResolutionCriterion.SERIES_POINTS);
        assertFalse(second.requiresTieBreak());
    }

    @Test
    void aggregateWinAndAwayGoalsWinHaveDifferentDescriptions() {
        SecondLegMatch aggregate = secondLeg(firstLeg(3, 0), 1, 0);
        SecondLegMatch awayGoals = secondLeg(firstLeg(1, 0), 2, 1);
        assertSame(a, aggregate.getWinner());
        assertEquals(3, aggregate.getSeriesPoints(a));
        assertEquals(3, aggregate.getSeriesPoints(b));
        assertEquals(3, aggregate.getAggregateGoals(a));
        assertEquals(1, aggregate.getAggregateGoals(b));
        assertCriterion(aggregate, ResolutionCriterion.AGGREGATE_GOALS);
        assertEquals("BY AGGREGATE GOALS", aggregate.getResolutionCriterion().getDescription());
        assertFalse(aggregate.requiresTieBreak());
        assertSame(a, awayGoals.getWinner());
        assertEquals(2, awayGoals.getAggregateGoals(a));
        assertEquals(2, awayGoals.getAggregateGoals(b));
        assertCriterion(awayGoals, ResolutionCriterion.AWAY_GOALS);
        assertEquals("BY AWAY GOALS", awayGoals.getResolutionCriterion().getDescription());
        assertFalse(awayGoals.requiresTieBreak());
    }

    @Test
    void descriptionsAlsoIdentifyTheWinnerWhenFirstLegAwayTeamAdvances() {
        SecondLegMatch aggregate = secondLeg(firstLeg(0, 3), 0, 1);
        SecondLegMatch awayGoals = secondLeg(firstLeg(2, 1), 1, 0);
        assertSame(b, aggregate.getWinner());
        assertCriterion(aggregate, ResolutionCriterion.AGGREGATE_GOALS);
        assertSame(b, awayGoals.getWinner());
        assertCriterion(awayGoals, ResolutionCriterion.AWAY_GOALS);
    }

    @Test
    void weightedDrawStillRequiresPenaltiesEvenWithAnAggregateAdvantage() {
        SecondLegMatch second = secondLeg(firstLeg(4, 2), 2, 1);
        assertEquals(5, second.getAggregateGoals(a));
        assertEquals(4, second.getAggregateGoals(b));
        assertEquals(6, second.getWeightedGoals(a));
        assertEquals(6, second.getWeightedGoals(b));
        assertUnresolved(second);
    }

    @Test
    void weightedGoalDifferenceTakesPriorityOverAnyRecordedPenalties() {
        FirstLegMatch first = firstLeg(4, 2);
        SecondLegMatch second = secondLeg(first, 1, 0);
        second.setHomePenalties(0);
        second.setAwayPenalties(5);
        assertEquals(3, second.getSeriesPoints(a));
        assertEquals(3, second.getSeriesPoints(b));
        assertEquals(4, second.getWeightedGoals(a));
        assertEquals(5, second.getWeightedGoals(b));
        assertSame(b, second.getWinner());
        assertCriterion(second, ResolutionCriterion.WEIGHTED_GOAL_DIFFERENCE);
        assertFalse(second.requiresTieBreak());
    }

    @Test
    void seriesNeedsPenaltiesUntilBothScoresAreRecordedAndDifferent() {
        SecondLegMatch second = secondLeg(firstLeg(0, 0), 0, 0);
        assertUnresolved(second);
        second.setHomePenalties(3);
        assertUnresolved(second);
        second.setAwayPenalties(3);
        assertUnresolved(second);
        second.setAwayPenalties(4);
        assertSame(a, second.getWinner());
        assertCriterion(second, ResolutionCriterion.PENALTIES);
        assertFalse(second.requiresTieBreak());
    }

    @Test
    void tiedFinalNeedsPenaltiesUntilBothScoresAreRecordedAndDifferent() {
        FinalMatch match = new FinalMatch(a, b, null, null, MATCH_DATE);
        score(match, 1, 1);
        assertUnresolved(match);
        match.setAwayPenalties(4);
        assertUnresolved(match);
        match.setHomePenalties(4);
        assertUnresolved(match);
        match.setHomePenalties(5);
        assertSame(a, match.getWinner());
        assertCriterion(match, ResolutionCriterion.PENALTIES);
        assertFalse(match.requiresTieBreak());
    }

    @Test
    void finalRegularTimeResultTakesPriorityOverAnyRecordedPenalties() {
        FinalMatch match = new FinalMatch(a, b, null, null, MATCH_DATE);
        score(match, 2, 0);
        match.setHomePenalties(0);
        match.setAwayPenalties(5);
        assertSame(a, match.getWinner());
        assertCriterion(match, ResolutionCriterion.REGULAR_TIME);
        assertFalse(match.requiresTieBreak());
    }

    private static void assertUnresolved(Match match) {
        assertNull(match.getWinner());
        assertTrue(match.requiresTieBreak());
        assertCriterion(match, ResolutionCriterion.UNRESOLVED);
    }

    private static void assertCriterion(Match match, ResolutionCriterion criterion) {
        assertEquals(criterion, match.getResolutionCriterion());
        assertEquals(criterion.name(), match.getResolutionCriteria());
    }

    private FirstLegMatch firstLeg(int homeGoals, int awayGoals) {
        FirstLegMatch match = new FirstLegMatch(a, b, null, null, MATCH_DATE);
        score(match, homeGoals, awayGoals);
        return match;
    }

    private SecondLegMatch secondLeg(FirstLegMatch first, int homeGoals, int awayGoals) {
        SecondLegMatch match = new SecondLegMatch(b, a, null, null, MATCH_DATE.plusDays(7), first);
        score(match, homeGoals, awayGoals);
        return match;
    }

    private static void score(Match match, int homeGoals, int awayGoals) {
        match.setHomeGoals(homeGoals);
        match.setAwayGoals(awayGoals);
        match.setPlayed(true);
    }
}
