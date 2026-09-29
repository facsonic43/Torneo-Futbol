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

    private static final int MATCH_LAST_MINUTE = 90;
    private static final int MAX_SUBSTITUTIONS = 5;

    private final MatchEventSimulator eventSimulator;
    private final PenaltyShootoutSimulator penaltySimulator;

    public MatchSimulator() {
        this(new Random());
    }

    // Permite repetir siempre la misma simulación durante los tests.
    public MatchSimulator(long seed) {
        this(new Random(seed));
    }

    private MatchSimulator(
            Random random) {

        this.eventSimulator =
                new MatchEventSimulator(
                        random
                );

        this.penaltySimulator =
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

        // Contadores de sustituciones realizadas (arreglos de 1 elemento
        // para que MatchEventSimulator pueda modificarlos).
        int[] homeSubsCount = {
                0
        };

        int[] awaySubsCount = {
                0
        };

        eventSimulator.simulateMinutesRange(
                1,
                MATCH_LAST_MINUTE,
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
                MAX_SUBSTITUTIONS,
                participations
        );

        if (match.getReferee() != null) {
            match.getReferee()
                    .addMatchOfficiated();
        }

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
                match
        );
    }

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

    // Suma partidos y minutos jugados a cada jugador según su participación real.
    private void registerPlayerStats(
            Match match) {

        for (PlayerParticipation participation :
                match.getPlayerParticipations()) {

            Player player =
                    participation.getPlayer();

            player.addMatchesPlayed();

            player.addMinutesPlayed(
                    participation.getMinutesPlayed()
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