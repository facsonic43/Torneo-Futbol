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
 * Comprueba que el usuario pueda controlar manualmente la ejecución.
 * Verifica partidos individuales, fechas completas, fases completas,
 * bloqueos cronológicos y avance hasta el campeón.
 */
public class TournamentExecutionTest {

    @Test
    void userShouldControlTournamentExecution()
            throws Exception {

        TournamentDataLoader loader =
                new TournamentDataLoader();

        TournamentData data =
                loader.load(
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

        /*
         * --------------------------------------------------
         * INICIO
         * --------------------------------------------------
         */

        assertEquals(
                TournamentStage.GROUP_STAGE,
                execution.getCurrentStage(
                        tournament
                )
        );

        assertEquals(
                24,
                execution
                        .getCurrentPhaseMatches(
                                tournament
                        )
                        .size()
        );

        /*
         * Cada fecha de grupos tiene:
         * 4 grupos × 2 partidos = 8 partidos.
         */
        List<Match> firstMatchday =
                execution
                        .getCurrentPlayableMatches(
                                tournament
                        );

        assertEquals(
                8,
                firstMatchday.size()
        );

        assertEquals(
                "GROUP STAGE - MATCHDAY 1",
                execution
                        .getCurrentRoundName(
                                tournament
                        )
        );

        /*
         * --------------------------------------------------
         * PARTIDO INDIVIDUAL
         * --------------------------------------------------
         */

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
                countPlayedMatches(
                        tournament
                )
        );

        /*
         * Quedan 7 partidos de la misma fecha.
         */
        assertEquals(
                7,
                execution
                        .getCurrentPlayableMatches(
                                tournament
                        )
                        .size()
        );

        /*
         * Buscamos un partido de la siguiente fecha.
         * Todavía tiene que estar bloqueado.
         */
        Match futureMatch =
                null;

        for (Match match :
                execution.getCurrentPhaseMatches(
                        tournament
                )) {

            if (!match.isPlayed()
                    && match.getMatchDate()
                    .isAfter(
                            selectedMatch
                                    .getMatchDate()
                    )) {

                futureMatch =
                        match;

                break;
            }
        }

        assertNotNull(
                futureMatch
        );

        assertFalse(
                execution.isMatchPlayable(
                        tournament,
                        futureMatch
                )
        );

        Match lockedMatch =
                futureMatch;

        assertThrows(
                IllegalStateException.class,
                () ->
                        execution.playMatch(
                                tournament,
                                lockedMatch,
                                stadiums
                        )
        );

        /*
         * --------------------------------------------------
         * COMPLETAR FECHA
         * --------------------------------------------------
         */

        int simulatedMatchday =
                execution
                        .simulateCurrentMatchday(
                                tournament,
                                stadiums
                        );

        assertEquals(
                7,
                simulatedMatchday
        );

        assertEquals(
                8,
                countPlayedMatches(
                        tournament
                )
        );

        assertEquals(
                "GROUP STAGE - MATCHDAY 2",
                execution
                        .getCurrentRoundName(
                                tournament
                        )
        );

        assertEquals(
                8,
                execution
                        .getCurrentPlayableMatches(
                                tournament
                        )
                        .size()
        );

        /*
         * --------------------------------------------------
         * COMPLETAR FASE DE GRUPOS
         * --------------------------------------------------
         */

        int remainingGroupMatches =
                execution
                        .simulateRemainingPhase(
                                tournament,
                                stadiums
                        );

        assertEquals(
                16,
                remainingGroupMatches
        );

        assertEquals(
                24,
                countPlayedGroupMatches(
                        tournament
                )
        );

        /*
         * Al terminar grupos se generan cuartos,
         * pero todavía no se juegan.
         */
        assertEquals(
                TournamentStage.QUARTER_FINALS,
                execution.getCurrentStage(
                        tournament
                )
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
                        .getCurrentPlayableMatches(
                                tournament
                        )
                        .size()
        );

        assertEquals(
                "QUARTER-FINALS - FIRST LEG",
                execution
                        .getCurrentRoundName(
                                tournament
                        )
        );

        /*
         * --------------------------------------------------
         * JUGAR UNA IDA DE CUARTOS
         * --------------------------------------------------
         */

        Match quarterFinal =
                execution
                        .getCurrentPlayableMatches(
                                tournament
                        )
                        .get(0);

        execution.playMatch(
                tournament,
                quarterFinal,
                stadiums
        );

        assertTrue(
                quarterFinal.isPlayed()
        );

        /*
         * Quedan tres partidos de ida.
         */
        assertEquals(
                3,
                execution
                        .getCurrentPlayableMatches(
                                tournament
                        )
                        .size()
        );

        /*
         * --------------------------------------------------
         * COMPLETAR TODAS LAS IDAS
         * --------------------------------------------------
         */

        execution.simulateCurrentMatchday(
                tournament,
                stadiums
        );

        assertEquals(
                "QUARTER-FINALS - SECOND LEG",
                execution
                        .getCurrentRoundName(
                                tournament
                        )
        );

        assertEquals(
                4,
                execution
                        .getCurrentPlayableMatches(
                                tournament
                        )
                        .size()
        );

        /*
         * --------------------------------------------------
         * COMPLETAR CUARTOS
         * --------------------------------------------------
         */

        execution.simulateRemainingPhase(
                tournament,
                stadiums
        );

        assertEquals(
                TournamentStage.SEMI_FINALS,
                execution.getCurrentStage(
                        tournament
                )
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
                        .getCurrentPlayableMatches(
                                tournament
                        )
                        .size()
        );

        /*
         * --------------------------------------------------
         * COMPLETAR SEMIFINALES
         * --------------------------------------------------
         */

        execution.simulateRemainingPhase(
                tournament,
                stadiums
        );

        assertEquals(
                TournamentStage.FINAL,
                execution.getCurrentStage(
                        tournament
                )
        );

        assertNotNull(
                tournament.getFinalMatch()
        );

        assertEquals(
                1,
                execution
                        .getCurrentPlayableMatches(
                                tournament
                        )
                        .size()
        );

        /*
         * --------------------------------------------------
         * JUGAR FINAL MANUALMENTE
         * --------------------------------------------------
         */

        Match finalMatch =
                tournament
                        .getFinalMatch();

        execution.playMatch(
                tournament,
                finalMatch,
                stadiums
        );

        assertTrue(
                finalMatch.isPlayed()
        );

        assertNotNull(
                tournament.getChampion()
        );

        assertEquals(
                TournamentStage.FINISHED,
                execution.getCurrentStage(
                        tournament
                )
        );

        assertEquals(
                37,
                countPlayedMatches(
                        tournament
                )
        );

        assertTrue(
                execution
                        .getCurrentPlayableMatches(
                                tournament
                        )
                        .isEmpty()
        );
    }

    private int countPlayedMatches(
            Tournament tournament) {

        int count = 0;

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

        int count = 0;

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