import main.loader.TournamentData;
import main.loader.TournamentDataLoader;
import model.match.Match;
import model.match.Stadium;
import model.tournament.Tournament;
import model.tournament.TournamentStage;
import org.junit.jupiter.api.Test;
import service.TournamentExecutionService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Comprueba la ejecución manual del campeonato.
 * Verifica partidos individuales, fechas, fases, bloqueos cronológicos y que
 * cualquier tanda de penales quede pendiente hasta que el usuario la resuelva.
 */
public class TournamentExecutionTest {

    @Test
    void userShouldControlTournamentExecution()
            throws Exception {

        TournamentData data =
                new TournamentDataLoader()
                        .load(
                                "torneo.json"
                        );

        Tournament tournament =
                new Tournament(
                        data.getTeams(),
                        data.getReferees()
                );

        List<Stadium> stadiums =
                createTestStadiums();

        tournament.drawGroups();

        tournament.generateGroupMatches(
                stadiums
        );

        TournamentExecutionService execution =
                new TournamentExecutionService(
                        20260920L
                );

        assertEquals(
                TournamentStage.GROUP_STAGE,
                execution.getCurrentStage(tournament)
        );

        assertEquals(
                24,
                execution
                        .getCurrentPhaseMatches(tournament)
                        .size()
        );

        List<Match> firstMatchday =
                execution
                        .getCurrentPlayableMatches(tournament);

        assertEquals(
                8,
                firstMatchday.size()
        );

        assertEquals(
                "GROUP STAGE - MATCHDAY 1",
                execution.getCurrentRoundName(tournament)
        );

        Match selectedMatch =
                firstMatchday.get(0);

        execution.playMatch(
                tournament,
                selectedMatch,
                stadiums
        );

        assertTrue(
                selectedMatch.isPlayed()
        );

        assertEquals(
                1,
                countPlayedMatches(tournament)
        );

        assertEquals(
                7,
                execution
                        .getCurrentPlayableMatches(tournament)
                        .size()
        );

        Match futureMatch =
                findFutureMatch(
                        execution,
                        tournament,
                        selectedMatch
                );

        assertNotNull(
                futureMatch
        );

        assertFalse(
                execution.isMatchPlayable(
                        tournament,
                        futureMatch
                )
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        execution.playMatch(
                                tournament,
                                futureMatch,
                                stadiums
                        )
        );

        int simulatedMatchday =
                execution.simulateCurrentMatchday(
                        tournament,
                        stadiums
                );

        assertEquals(
                7,
                simulatedMatchday
        );

        assertEquals(
                8,
                countPlayedMatches(tournament)
        );

        assertEquals(
                "GROUP STAGE - MATCHDAY 2",
                execution.getCurrentRoundName(tournament)
        );

        assertEquals(
                8,
                execution
                        .getCurrentPlayableMatches(tournament)
                        .size()
        );

        int remainingGroupMatches =
                execution.simulateRemainingPhase(
                        tournament,
                        stadiums
                );

        assertEquals(
                16,
                remainingGroupMatches
        );

        assertEquals(
                24,
                countPlayedGroupMatches(tournament)
        );

        assertEquals(
                TournamentStage.QUARTER_FINALS,
                execution.getCurrentStage(tournament)
        );

        assertEquals(
                4,
                tournament
                        .getQuarterFinalFirstLegs()
                        .size()
        );

        assertEquals(
                4,
                tournament
                        .getQuarterFinalSecondLegs()
                        .size()
        );

        assertEquals(
                4,
                execution
                        .getCurrentPlayableMatches(tournament)
                        .size()
        );

        assertEquals(
                "QUARTER-FINALS - FIRST LEG",
                execution.getCurrentRoundName(tournament)
        );

        Match quarterFinal =
                execution
                        .getCurrentPlayableMatches(tournament)
                        .get(0);

        execution.playMatch(
                tournament,
                quarterFinal,
                stadiums
        );

        assertTrue(
                quarterFinal.isPlayed()
        );

        assertEquals(
                3,
                execution
                        .getCurrentPlayableMatches(tournament)
                        .size()
        );

        execution.simulateCurrentMatchday(
                tournament,
                stadiums
        );

        assertEquals(
                "QUARTER-FINALS - SECOND LEG",
                execution.getCurrentRoundName(tournament)
        );

        assertEquals(
                4,
                execution
                        .getCurrentPlayableMatches(tournament)
                        .size()
        );

        /*
         * Se simulan los 90 minutos de los cuartos.
         * Si alguna serie necesita penales, los resolvemos
         * manualmente mediante simulatePenalties().
         */
        execution.simulateRemainingPhase(
                tournament,
                stadiums
        );

        resolvePendingPenalties(
                execution,
                tournament,
                stadiums
        );

        assertEquals(
                TournamentStage.SEMI_FINALS,
                execution.getCurrentStage(tournament)
        );

        assertEquals(
                2,
                tournament
                        .getSemiFinalFirstLegs()
                        .size()
        );

        assertEquals(
                2,
                tournament
                        .getSemiFinalSecondLegs()
                        .size()
        );

        assertEquals(
                2,
                execution
                        .getCurrentPlayableMatches(tournament)
                        .size()
        );

        execution.simulateRemainingPhase(
                tournament,
                stadiums
        );

        resolvePendingPenalties(
                execution,
                tournament,
                stadiums
        );

        assertEquals(
                TournamentStage.FINAL,
                execution.getCurrentStage(tournament)
        );

        assertNotNull(
                tournament.getFinalMatch()
        );

        assertEquals(
                1,
                execution
                        .getCurrentPlayableMatches(tournament)
                        .size()
        );

        Match finalMatch =
                tournament.getFinalMatch();

        execution.playMatch(
                tournament,
                finalMatch,
                stadiums
        );

        assertTrue(
                finalMatch.isPlayed()
        );

        /*
         * Si la final termina empatada, todavía no debe existir campeón.
         * Recién después de simulatePenalties() queda definido.
         */
        if (execution.requiresPenalties(finalMatch)) {
            assertNull(
                    tournament.getChampion()
            );

            assertNull(
                    finalMatch.getHomePenalties()
            );

            assertNull(
                    finalMatch.getAwayPenalties()
            );

            execution.simulatePenalties(
                    tournament,
                    finalMatch,
                    stadiums
            );
        }

        assertNotNull(
                tournament.getChampion()
        );

        assertEquals(
                TournamentStage.FINISHED,
                execution.getCurrentStage(tournament)
        );

        assertEquals(
                37,
                countPlayedMatches(tournament)
        );

        assertTrue(
                execution
                        .getCurrentPlayableMatches(tournament)
                        .isEmpty()
        );

        assertTrue(
                execution
                        .getPendingPenaltyMatches(tournament)
                        .isEmpty()
        );
    }

    private void resolvePendingPenalties(
            TournamentExecutionService execution,
            Tournament tournament,
            List<Stadium> stadiums) {

        while (!execution
                .getPendingPenaltyMatches(tournament)
                .isEmpty()) {

            Match pending =
                    execution
                            .getPendingPenaltyMatches(tournament)
                            .get(0);

            assertTrue(
                    pending.isPlayed()
            );

            assertTrue(
                    execution.requiresPenalties(pending)
            );

            assertNull(
                    pending.getHomePenalties()
            );

            assertNull(
                    pending.getAwayPenalties()
            );

            execution.simulatePenalties(
                    tournament,
                    pending,
                    stadiums
            );

            assertNotNull(
                    pending.getHomePenalties()
            );

            assertNotNull(
                    pending.getAwayPenalties()
            );

            assertNotNull(
                    pending.getWinner()
            );
        }
    }

    private Match findFutureMatch(
            TournamentExecutionService execution,
            Tournament tournament,
            Match selectedMatch) {

        for (Match match :
                execution.getCurrentPhaseMatches(tournament)) {

            if (!match.isPlayed()
                    && match.getMatchDate()
                    .isAfter(
                            selectedMatch.getMatchDate()
                    )) {

                return match;
            }
        }

        return null;
    }

    private int countPlayedMatches(
            Tournament tournament) {

        int count =
                0;

        for (Match match :
                tournament.getAllMatches()) {

            if (match.isPlayed()) {
                count++;
            }
        }

        return count;
    }

    private int countPlayedGroupMatches(
            Tournament tournament) {

        int count =
                0;

        for (model.tournament.Group group :
                tournament.getGroups()) {

            for (Match match :
                    group.getMatches()) {

                if (match.isPlayed()) {
                    count++;
                }
            }
        }

        return count;
    }

    private List<Stadium> createTestStadiums() {
        List<Stadium> stadiums =
                new ArrayList<>();

        for (int i = 1;
             i <= 16;
             i++) {

            stadiums.add(
                    new Stadium(
                            "Execution Stadium "
                                    + i,
                            i
                    )
            );
        }

        return stadiums;
    }
}