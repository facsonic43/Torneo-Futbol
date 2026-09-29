import control.MatchSimulator;
import main.loader.TournamentData;
import main.loader.TournamentDataLoader;
import model.match.Stadium;
import model.participant.Player;
import model.tournament.Tournament;
import model.tournament.TournamentStage;
import org.junit.jupiter.api.Test;
import persistence.TournamentPersistenceService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Comprueba que un campeonato pueda guardarse, cerrarse conceptualmente,
 * recuperarse desde archivo y continuar normalmente hasta obtener un campeón.
 */
public class TournamentPersistenceTest {

    @Test
    void tournamentShouldBeSavedLoadedAndContinued()
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

        MatchSimulator simulator =
                new MatchSimulator(
                        20260920L
                );

        tournament.drawGroups();

        tournament.generateGroupMatches(
                stadiums
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

        TournamentPersistenceService persistence =
                new TournamentPersistenceService();

        Path savePath =
                Path.of(
                        "target",
                        "test-saves",
                        "tournament-test.dat"
                );

        String fileName =
                savePath.toString();

        Player playerBeforeSave =
                tournament
                        .getTeams()
                        .get(0)
                        .getSquad()
                        .get(0);

        int minutesBeforeSave =
                playerBeforeSave
                        .getMinutesPlayed();

        int goalsBeforeSave =
                playerBeforeSave
                        .getGoals();

        String groupLeaderBeforeSave =
                tournament
                        .getGroups()
                        .get(0)
                        .getStandings()
                        .get(0)
                        .getTeam()
                        .getName();

        try {

            assertEquals(
                    TournamentStage.QUARTER_FINALS,
                    persistence.getStage(
                            tournament
                    )
            );

            assertEquals(
                    32,
                    tournament
                            .getAllMatches()
                            .size()
            );

            persistence.saveTournament(
                    tournament,
                    fileName
            );

            assertTrue(
                    persistence.saveExists(
                            fileName
                    )
            );

            /*
             * A partir de este punto no seguimos utilizando el torneo original.
             * Toda la continuación se hace con el objeto recuperado del archivo.
             */
            Tournament loadedTournament =
                    persistence.loadTournament(
                            fileName
                    );

            assertNotNull(
                    loadedTournament
            );

            assertEquals(
                    TournamentStage.QUARTER_FINALS,
                    persistence.getStage(
                            loadedTournament
                    )
            );

            assertEquals(
                    16,
                    loadedTournament
                            .getTeams()
                            .size()
            );

            assertEquals(
                    4,
                    loadedTournament
                            .getGroups()
                            .size()
            );

            assertEquals(
                    32,
                    loadedTournament
                            .getAllMatches()
                            .size()
            );

            assertEquals(
                    4,
                    loadedTournament
                            .getQuarterFinalSecondLegs()
                            .size()
            );

            assertNull(
                    loadedTournament
                            .getChampion()
            );

            Player loadedPlayer =
                    loadedTournament
                            .getTeams()
                            .get(0)
                            .getSquad()
                            .get(0);

            assertEquals(
                    minutesBeforeSave,
                    loadedPlayer
                            .getMinutesPlayed()
            );

            assertEquals(
                    goalsBeforeSave,
                    loadedPlayer
                            .getGoals()
            );

            String loadedGroupLeader =
                    loadedTournament
                            .getGroups()
                            .get(0)
                            .getStandings()
                            .get(0)
                            .getTeam()
                            .getName();

            assertEquals(
                    groupLeaderBeforeSave,
                    loadedGroupLeader
            );

            /*
             * Acá demostramos realmente el requisito:
             * el campeonato cargado continúa desde semifinales.
             */
            loadedTournament
                    .generateSemiFinals();

            loadedTournament
                    .simulateSemiFinals(
                            simulator
                    );

            loadedTournament
                    .generateFinal();

            loadedTournament
                    .simulateFinal(
                            simulator
                    );

            assertNotNull(
                    loadedTournament
                            .getChampion()
            );

            assertEquals(
                    TournamentStage.FINISHED,
                    persistence.getStage(
                            loadedTournament
                    )
            );

            assertEquals(
                    37,
                    loadedTournament
                            .getAllMatches()
                            .size()
            );

            String championBeforeSecondSave =
                    loadedTournament
                            .getChampion()
                            .getName();

            /*
             * Lo guardamos nuevamente ya terminado
             * para comprobar que también persiste el campeón.
             */
            persistence.saveTournament(
                    loadedTournament,
                    fileName
            );

            Tournament finishedTournament =
                    persistence.loadTournament(
                            fileName
                    );

            assertEquals(
                    TournamentStage.FINISHED,
                    persistence.getStage(
                            finishedTournament
                    )
            );

            assertNotNull(
                    finishedTournament
                            .getChampion()
            );

            assertEquals(
                    championBeforeSecondSave,
                    finishedTournament
                            .getChampion()
                            .getName()
            );

            assertEquals(
                    37,
                    finishedTournament
                            .getAllMatches()
                            .size()
            );

        } finally {

            persistence.deleteSave(
                    fileName
            );
        }
    }

    private List<Stadium> createTestStadiums() {

        List<Stadium> stadiums =
                new ArrayList<>();

        for (int i = 1;
             i <= 16;
             i++) {

            stadiums.add(
                    new Stadium(
                            "Persistence Stadium "
                                    + i,
                            i
                    )
            );
        }

        return stadiums;
    }
}