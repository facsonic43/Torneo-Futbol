import control.MatchSimulator;
import main.loader.TournamentData;
import main.loader.TournamentDataLoader;
import model.match.GroupMatch;
import model.match.Stadium;
import model.tournament.Group;
import model.tournament.Tournament;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GroupStageSimulationTest {

    /*
     * Comprueba que el torneo pueda crear cuatro grupos, generar los 24 partidos
     * correspondientes a la fase de grupos, simularlos y obtener dos clasificados
     * por cada grupo.
     */
    @Test
    void groupStageShouldGenerateAndSimulateMatches() {

        TournamentDataLoader loader =
                new TournamentDataLoader();

        TournamentData data =
                loader.load("torneo.json");

        Tournament tournament =
                new Tournament(
                        data.getTeams(),
                        data.getReferees()
                );

        List<Group> groups =
                tournament.drawGroups();

        assertEquals(
                4,
                groups.size()
        );

        List<Stadium> stadiums =
                createTestStadiums();

        tournament.generateGroupMatches(
                stadiums
        );

        MatchSimulator simulator =
                new MatchSimulator();

        tournament.simulateGroupStage(
                simulator
        );

        int totalMatches = 0;

        for (Group group : groups) {

            assertEquals(
                    4,
                    group.getTeams().size()
            );

            assertEquals(
                    6,
                    group.getMatches().size()
            );

            assertEquals(
                    4,
                    group.getStandings().size()
            );

            assertEquals(
                    2,
                    group.getQualifiedTeams().size()
            );

            totalMatches +=
                    group.getMatches().size();

            for (GroupMatch match :
                    group.getMatches()) {

                assertTrue(
                        match.isPlayed()
                );

                assertNotNull(
                        match.getReferee()
                );

                assertNotNull(
                        match.getStadium()
                );

                assertNotNull(
                        match.getHomeFormation()
                );

                assertNotNull(
                        match.getAwayFormation()
                );

                assertEquals(
                        11,
                        match.getHomeStarters().size()
                );

                assertEquals(
                        11,
                        match.getAwayStarters().size()
                );
            }
        }

        assertEquals(
                24,
                totalMatches
        );
    }

    // Crea estadios simples para probar la fase de grupos sin depender de PostgreSQL.
    private List<Stadium> createTestStadiums() {

        List<Stadium> stadiums =
                new ArrayList<>();

        for (int i = 1; i <= 16; i++) {

            stadiums.add(
                    new Stadium(
                            "Test Stadium " + i,
                            i
                    )
            );
        }

        return stadiums;
    }
}