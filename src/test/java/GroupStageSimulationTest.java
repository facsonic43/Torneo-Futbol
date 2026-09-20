import control.MatchSimulator;
import main.loader.TournamentData;
import main.loader.TournamentDataLoader;
import model.match.GroupMatch;
import model.match.Stadium;
import model.participant.Referee;
import model.participant.Team;
import model.tournament.Group;
import model.tournament.Tournament;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GroupStageSimulationTest {

    @Test
    void groupStageShouldGenerateAndSimulateMatches() {
        TournamentDataLoader loader = new TournamentDataLoader();
        TournamentData data = loader.load("torneo.json");

        Tournament tournament = new Tournament();
        List<Group> groups = tournament.drawGroups(data.getTeams());

        assertEquals(4, groups.size());

        MatchSimulator simulator = new MatchSimulator();
        int totalMatches = 0;

        for (Group group : groups) {
            List<Team> teams = new ArrayList<>(group.getTeams());
            for (int i = 0; i < teams.size(); i++) {
                for (int j = i + 1; j < teams.size(); j++) {
                    Team home = teams.get(i);
                    Team away = teams.get(j);
                    Referee referee = data.getReferees().get((i + j) % data.getReferees().size());
                    Stadium stadium = new Stadium("Estadio de prueba", 1);
                    GroupMatch match = new GroupMatch(home, away, referee, stadium, LocalDate.now());
                    group.addMatch(match);
                    simulator.simulateMatch(match);
                    totalMatches++;
                }
            }
        }

        assertEquals(24, totalMatches);

        for (Group group : groups) {
            assertFalse(group.getMatches().isEmpty());
            assertEquals(6, group.getMatches().size());
            assertEquals(4, group.getStandings().size());
            assertEquals(2, group.getQualifiedTeams().size());
            for (GroupMatch match : group.getMatches()) {
                assertTrue(match.isPlayed());
            }
        }
    }
}
