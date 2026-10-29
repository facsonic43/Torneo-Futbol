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
 * Coordina la simulación de los partidos.
 * La GUI puede simular sólo los 90 minutos y dejar los penales pendientes,
 * mientras que simulateMatch() conserva la simulación completa para consola y tests.
 */
public class MatchSimulator {
    private static final int MATCH_LAST_MINUTE = 90;
    private static final int MAX_SUBSTITUTIONS = 5;

    private final MatchEventSimulator eventSimulator;
    private final PenaltyShootoutSimulator penaltySimulator;

    public MatchSimulator() {
        this(new Random());
    }

    // Permite repetir una simulación durante los tests.
    public MatchSimulator(long seed) {
        this(new Random(seed));
    }

    private MatchSimulator(Random random) {
        eventSimulator = new MatchEventSimulator(random);
        penaltySimulator = new PenaltyShootoutSimulator(random);
    }

    /*
     * Simula un partido completo. Se mantiene para RunChampionship y tests.
     * Si el partido ya fue jugado, no vuelve a simular sus 90 minutos.
     */
    public void simulateMatch(Match match) {
        if (match == null) {
            throw new IllegalArgumentException("Match cannot be null.");
        }

        if (match.isPlayed()) {
            return;
        }

        simulateRegularTime(match);

        if (match.requiresTieBreak()) {
            simulatePenaltyShootout(match);
        }
    }

    /*
     * Simula exclusivamente los 90 minutos. Si hace falta una tanda de penales,
     * el partido queda jugado pero sin ganador definitivo hasta que el usuario la ejecute.
     */
    public void simulateRegularTime(Match match) {
        if (match == null) {
            throw new IllegalArgumentException("Match cannot be null.");
        }

        if (match.isPlayed()) {
            throw new IllegalStateException("This match has already been played.");
        }

        Team home = match.getHomeTeam();
        Team away = match.getAwayTeam();

        Lineup homeLineup = new Lineup(home, away);
        Lineup awayLineup = new Lineup(away, home);

        match.setFormations(
                homeLineup.getFormation(),
                awayLineup.getFormation()
        );

        match.setStartingLineups(
                homeLineup.getStarters(),
                awayLineup.getStarters()
        );

        List<Player> homePitch =
                new ArrayList<>(homeLineup.getStarters());

        List<Player> awayPitch =
                new ArrayList<>(awayLineup.getStarters());

        List<Player> homeSubs =
                new ArrayList<>(homeLineup.getSubs());

        List<Player> awaySubs =
                new ArrayList<>(awayLineup.getSubs());

        Player[] homeGoalkeeper = {
                eventSimulator.findActiveGoalkeeper(homePitch)
        };

        Player[] awayGoalkeeper = {
                eventSimulator.findActiveGoalkeeper(awayPitch)
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

        updatePreviousAbsences(home);
        updatePreviousAbsences(away);

        Set<Player> matchYellows =
                new HashSet<>();

        int[] homeSubsCount = {0};
        int[] awaySubsCount = {0};

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
            match.getReferee().addMatchOfficiated();
        }

        match.setPlayed(true);

        registerPlayerStats(match);
    }

    /*
     * Ejecuta únicamente una tanda pendiente. Los pateadores se obtienen de
     * quienes terminaron el partido en cancha; si no se pueden reconstruir,
     * se utilizan los titulares registrados.
     */
    public void simulatePenaltyShootout(Match match) {
        if (match == null) {
            throw new IllegalArgumentException("Match cannot be null.");
        }

        if (!match.isPlayed()) {
            throw new IllegalStateException(
                    "Regular time must be played before penalties."
            );
        }

        if (!match.requiresTieBreak()) {
            throw new IllegalStateException(
                    "This match does not require a penalty shootout."
            );
        }

        List<Player> homePitch =
                getPlayersAvailableForShootout(
                        match,
                        match.getHomeTeam(),
                        match.getHomeStarters()
                );

        List<Player> awayPitch =
                getPlayersAvailableForShootout(
                        match,
                        match.getAwayTeam(),
                        match.getAwayStarters()
                );

        penaltySimulator.simulate(
                match,
                homePitch,
                awayPitch
        );
    }

    // Se conserva para los tests específicos de PenaltyShootoutSimulator.
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

    private List<Player> getPlayersAvailableForShootout(
            Match match,
            Team team,
            List<Player> fallbackPlayers) {

        List<Player> players =
                new ArrayList<>();

        for (PlayerParticipation participation :
                match.getPlayerParticipations()) {

            if (participation.getTeam() == team
                    && participation.getExitMinute() >= MATCH_LAST_MINUTE
                    && !players.contains(participation.getPlayer())) {

                players.add(
                        participation.getPlayer()
                );
            }
        }

        if (players.isEmpty()) {
            players.addAll(
                    fallbackPlayers
            );
        }

        return players;
    }

    private void registerStartingPlayers(
            Team team,
            List<Player> starters,
            Map<Player, PlayerParticipation> participations,
            Match match) {

        for (Player player : starters) {
            PlayerParticipation participation =
                    new PlayerParticipation(
                            player,
                            team,
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

    // Suma partidos y minutos según la participación real de cada jugador.
    private void registerPlayerStats(Match match) {
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

    private void updatePreviousAbsences(Team team) {
        for (Player player : team.getSquad()) {
            player.updateMatchAvailability();
        }
    }
}