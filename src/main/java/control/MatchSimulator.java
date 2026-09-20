package control;

import model.match.Lineup;
import model.match.Match;
import model.participant.Player;
import model.participant.PlayerParticipation;
import model.participant.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/*
 * Coordina la simulación completa de un partido.
 * Delega las incidencias del juego a MatchEventSimulator
 * y las tandas de penales a PenaltyShootoutSimulator.
 */
public class MatchSimulator {

    private Random random;
    private MatchEventSimulator eventSimulator;
    private PenaltyShootoutSimulator penaltySimulator;

    public MatchSimulator() {
        random = new Random();

        eventSimulator =
                new MatchEventSimulator(
                        random
                );

        penaltySimulator =
                new PenaltyShootoutSimulator(
                        random
                );
    }

    // Permite repetir siempre la misma simulación durante los tests.
    public MatchSimulator(long seed) {
        random = new Random(seed);

        eventSimulator =
                new MatchEventSimulator(
                        random
                );

        penaltySimulator =
                new PenaltyShootoutSimulator(
                        random
                );
    }

    public void simulateMatch(
            Match match) {

        Team home =
                match.getHomeTeam();

        Team away =
                match.getAwayTeam();

        Lineup homeLineup =
                new Lineup(
                        home,
                        away
                );

        Lineup awayLineup =
                new Lineup(
                        away,
                        home
                );

        match.setFormations(
                homeLineup.getFormation(),
                awayLineup.getFormation()
        );

        match.setStartingLineups(
                homeLineup.getStarters(),
                awayLineup.getStarters()
        );

        List<Player> homePitch =
                new ArrayList<>(
                        homeLineup.getStarters()
                );

        List<Player> awayPitch =
                new ArrayList<>(
                        awayLineup.getStarters()
                );

        List<Player> homeSubs =
                new ArrayList<>(
                        homeLineup.getSubs()
                );

        List<Player> awaySubs =
                new ArrayList<>(
                        awayLineup.getSubs()
                );

        Player[] homeGoalkeeper = {
                eventSimulator.findActiveGoalkeeper(
                        homePitch
                )
        };

        Player[] awayGoalkeeper = {
                eventSimulator.findActiveGoalkeeper(
                        awayPitch
                )
        };

        Map<Player, PlayerParticipation> participations =
                new HashMap<>();

        registerStartingPlayers(
                home,
                homePitch,
                participations,
                match
        );

        registerStartingPlayers(
                away,
                awayPitch,
                participations,
                match
        );

        updatePreviousAbsences(
                home
        );

        updatePreviousAbsences(
                away
        );

        Set<Player> matchYellows =
                new HashSet<>();

        int[] homeSubsCount = {
                0
        };

        int[] awaySubsCount = {
                0
        };

        int maxSubs = 5;

        eventSimulator.simulateMinutesRange(
                1,
                90,
                match,
                homePitch,
                awayPitch,
                homeSubs,
                awaySubs,
                homeGoalkeeper,
                awayGoalkeeper,
                matchYellows,
                homeSubsCount,
                awaySubsCount,
                maxSubs,
                participations
        );

        if (match.requiresTieBreak()) {
            penaltySimulator.simulate(
                    match,
                    homePitch,
                    awayPitch
            );
        }

        match.setPlayed(
                true
        );

        registerPlayerStats(
                participations
        );

        if (match.getReferee() != null) {
            match.getReferee()
                    .addMatchOfficiated();
        }
    }

    /*
     * Se mantiene público porque ya lo utiliza TournamentSimulationTest.
     * Internamente la responsabilidad ahora pertenece a PenaltyShootoutSimulator.
     */
    public void simulatePenaltyShootout(
            Match match,
            List<Player> homePitch,
            List<Player> awayPitch) {

        penaltySimulator.simulate(
                match,
                homePitch,
                awayPitch
        );
    }

    private void registerStartingPlayers(
            Team team,
            List<Player> starters,
            Map<Player, PlayerParticipation> participations,
            Match match) {

        for (Player player :
                starters) {

            PlayerParticipation participation =
                    new PlayerParticipation(
                            player,
                            team,
                            true,
                            0
                    );

            participations.put(
                    player,
                    participation
            );

            match.addPlayerParticipation(
                    participation
            );
        }
    }

    private void registerPlayerStats(
            Map<Player, PlayerParticipation> participations) {

        for (PlayerParticipation participation :
                participations.values()) {

            Player player =
                    participation.getPlayer();

            player.addMatchesPlayed();

            player.addMinutesPlayed(
                    participation
                            .getMinutesPlayed()
            );
        }
    }

    private void updatePreviousAbsences(
            Team team) {

        for (Player player :
                team.getSquad()) {

            player.updateMatchAvailability();
        }
    }
}