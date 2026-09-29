import control.MatchSimulator;
import main.loader.TournamentData;
import main.loader.TournamentDataLoader;
import model.match.Event;
import model.match.FinalMatch;
import model.match.Goal;
import model.match.Match;
import model.match.PenaltyTaken;
import model.match.SecondLegMatch;
import model.match.Stadium;
import model.participant.Player;
import model.participant.Team;
import model.tournament.Group;
import model.tournament.Tournament;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Comprueba el funcionamiento integral del campeonato.
 * Verifica las 37 fechas, las eliminatorias, árbitros, estadios,
 * incidencias, campeón y estadísticas de goles.
 */
public class TournamentSimulationTest {

    @Test
    void fullTournamentShouldFinishCorrectly() {

        TournamentDataLoader loader =
                new TournamentDataLoader();

        TournamentData data =
                loader.load("torneo.json");

        int initialGoals =
                countPlayerGoals(
                        data.getTeams()
                );

        Tournament tournament =
                new Tournament(
                        data.getTeams(),
                        data.getReferees()
                );

        List<Group> groups =
                tournament.drawGroups();

        List<Stadium> stadiums =
                createTestStadiums();

        tournament.generateGroupMatches(
                stadiums
        );

        // La semilla hace que este test repita siempre la misma simulación.
        MatchSimulator simulator =
                new MatchSimulator(
                        20260920L
                );

        tournament.simulateGroupStage(
                simulator
        );

        tournament.generateQuarterFinals(
                stadiums
        );

        tournament.simulateQuarterFinals(
                simulator
        );

        tournament.generateSemiFinals();

        tournament.simulateSemiFinals(
                simulator
        );

        tournament.generateFinal();

        tournament.simulateFinal(
                simulator
        );

        List<Match> allMatches =
                tournament.getAllMatches();

        assertEquals(
                4,
                groups.size()
        );

        assertEquals(
                24,
                countGroupMatches(groups)
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

        assertNotNull(
                tournament.getFinalMatch()
        );

        assertEquals(
                37,
                allMatches.size()
        );

        assertNotNull(
                tournament.getChampion()
        );

        validateAllMatchesPlayed(
                allMatches
        );

        validateReferees(
                allMatches
        );

        validateEventMinutes(
                allMatches
        );

        validateGoalsHaveGoalkeeper(
                allMatches
        );

        validateKnockoutWinners(
                tournament
        );

        validateKnockoutStadiums(
                allMatches
        );

        int matchGoals =
                countScorerGoalsFromEvents(
                        allMatches
                );

        int finalPlayerGoals =
                countPlayerGoals(
                        tournament.getTeams()
                );

        assertEquals(
                initialGoals + matchGoals,
                finalPlayerGoals
        );
    }

    // Comprueba directamente que una tanda de penales no sume goles a los jugadores.
    @Test
    void penaltyShootoutShouldNotIncreasePlayerGoals() {

        TournamentDataLoader loader =
                new TournamentDataLoader();

        TournamentData data =
                loader.load("torneo.json");

        Team home =
                data.getTeams().get(0);

        Team away =
                data.getTeams().get(1);

        FinalMatch match =
                new FinalMatch(
                        home,
                        away,
                        data.getReferees().get(0),
                        new Stadium(
                                "Penalty Test Stadium",
                                1
                        ),
                        java.time.LocalDate.now()
                );

        List<Player> homePlayers =
                new ArrayList<>(
                        home.getSquad()
                                .subList(0, 11)
                );

        List<Player> awayPlayers =
                new ArrayList<>(
                        away.getSquad()
                                .subList(0, 11)
                );

        int goalsBefore =
                countPlayerGoals(
                        data.getTeams()
                );

        MatchSimulator simulator =
                new MatchSimulator(
                        12345L
                );

        simulator.simulatePenaltyShootout(
                match,
                homePlayers,
                awayPlayers
        );

        int goalsAfter =
                countPlayerGoals(
                        data.getTeams()
                );

        assertEquals(
                goalsBefore,
                goalsAfter
        );

        assertNotNull(
                match.getHomePenalties()
        );

        assertNotNull(
                match.getAwayPenalties()
        );

        assertNotEquals(
                match.getHomePenalties(),
                match.getAwayPenalties()
        );

        boolean penaltyEventFound =
                false;

        for (Event event :
                match.getEvents()) {

            if (event
                    instanceof PenaltyTaken) {

                penaltyEventFound =
                        true;
            }
        }

        assertTrue(
                penaltyEventFound
        );
    }

    // Comprueba específicamente el criterio de goles de visitante ponderados.
    @Test
    void secondLegShouldUseWeightedGoalDifference() {

        TournamentDataLoader loader =
                new TournamentDataLoader();

        TournamentData data =
                loader.load("torneo.json");

        Team teamA =
                data.getTeams().get(0);

        Team teamB =
                data.getTeams().get(1);

        Stadium firstStadium =
                new Stadium(
                        "First Leg Stadium",
                        1
                );

        Stadium secondStadium =
                new Stadium(
                        "Second Leg Stadium",
                        2
                );

        model.match.FirstLegMatch firstLeg =
                new model.match.FirstLegMatch(
                        teamA,
                        teamB,
                        data.getReferees().get(0),
                        firstStadium,
                        java.time.LocalDate.now()
                );

        firstLeg.setHomeGoals(1);
        firstLeg.setAwayGoals(0);
        firstLeg.setPlayed(true);

        SecondLegMatch secondLeg =
                new SecondLegMatch(
                        teamB,
                        teamA,
                        data.getReferees().get(1),
                        secondStadium,
                        java.time.LocalDate.now()
                                .plusDays(7),
                        firstLeg
                );

        secondLeg.setHomeGoals(2);
        secondLeg.setAwayGoals(1);
        secondLeg.setPlayed(true);

        /*
         * Ida:
         * A 1 - 0 B
         *
         * Vuelta:
         * B 2 - 1 A
         *
         * Cada equipo obtiene 3 puntos.
         *
         * Goles ponderados:
         * A = 1 de local + 1 visitante x 2 = 3
         * B = 0 visitante + 2 de local = 2
         *
         * Clasifica A.
         */

        assertEquals(
                3,
                secondLeg.getSeriesPoints(
                        teamA
                )
        );

        assertEquals(
                3,
                secondLeg.getSeriesPoints(
                        teamB
                )
        );

        assertTrue(
                secondLeg
                        .getWeightedGoalDifference(
                                teamA
                        ) > 0
        );

        assertEquals(
                teamA,
                secondLeg.getWinner()
        );

        assertEquals(
                "WEIGHTED_GOAL_DIFFERENCE",
                secondLeg
                        .getResolutionCriteria()
        );
    }

    private List<Stadium> createTestStadiums() {

        List<Stadium> stadiums =
                new ArrayList<>();

        for (int i = 1;
             i <= 16;
             i++) {

            stadiums.add(
                    new Stadium(
                            "Test Stadium "
                                    + i,
                            i
                    )
            );
        }

        return stadiums;
    }

    private int countGroupMatches(
            List<Group> groups) {

        int total = 0;

        for (Group group :
                groups) {

            total +=
                    group.getMatches()
                            .size();
        }

        return total;
    }

    private void validateAllMatchesPlayed(
            List<Match> matches) {

        for (Match match :
                matches) {

            assertTrue(
                    match.isPlayed()
            );
        }
    }

    private void validateReferees(
            List<Match> matches) {

        for (Match match :
                matches) {

            assertNotNull(
                    match.getReferee()
            );

            assertTrue(
                    match.getReferee()
                            .canOfficiate(
                                    match.getHomeTeam(),
                                    match.getAwayTeam()
                            )
            );
        }
    }

    private void validateEventMinutes(
            List<Match> matches) {

        for (Match match :
                matches) {

            for (Event event :
                    match.getEvents()) {

                assertTrue(
                        event.getMinute() <= 90
                );

                assertTrue(
                        event.getMinute() >= 1
                );
            }
        }
    }

    private void validateGoalsHaveGoalkeeper(
            List<Match> matches) {

        for (Match match :
                matches) {

            for (Event event :
                    match.getEvents()) {

                if (event instanceof Goal) {

                    Goal goal =
                            (Goal) event;

                    assertNotNull(
                            goal.getGoalkeeperConceded()
                    );
                }
            }
        }
    }

    private void validateKnockoutWinners(
            Tournament tournament) {

        for (SecondLegMatch match :
                tournament
                        .getQuarterFinalSecondLegs()) {

            assertNotNull(
                    match.getWinner()
            );
        }

        for (SecondLegMatch match :
                tournament
                        .getSemiFinalSecondLegs()) {

            assertNotNull(
                    match.getWinner()
            );
        }

        assertNotNull(
                tournament
                        .getFinalMatch()
                        .getWinner()
        );
    }

    private void validateKnockoutStadiums(
            List<Match> matches) {

        Set<String> stadiumNames =
                new HashSet<>();

        int knockoutMatches = 0;

        for (Match match :
                matches) {

            if (match.isKnockout()) {

                knockoutMatches++;

                assertNotNull(
                        match.getStadium()
                );

                stadiumNames.add(
                        match.getStadium()
                                .getName()
                );
            }
        }

        assertEquals(
                13,
                knockoutMatches
        );

        assertEquals(
                13,
                stadiumNames.size()
        );
    }

    private int countScorerGoalsFromEvents(
            List<Match> matches) {

        int goals = 0;

        for (Match match :
                matches) {

            for (Event event :
                    match.getEvents()) {

                if (event instanceof Goal) {

                    Goal goal =
                            (Goal) event;

                    if (!goal.isOwnGoal()) {
                        goals++;
                    }
                }
            }
        }

        return goals;
    }

    private int countPlayerGoals(
            List<Team> teams) {

        int goals = 0;

        for (Team team :
                teams) {

            for (Player player :
                    team.getSquad()) {

                goals +=
                        player.getGoals();
            }
        }

        return goals;
    }
}