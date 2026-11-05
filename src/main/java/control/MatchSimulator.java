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

// Coordina la simulación de los 90 minutos y de las tandas de penales, que pueden
// ejecutarse por separado. Si un equipo no completa la formación, registra la derrota
// por 3-0 sin sumar goles ni participaciones a futbolistas y actualiza las ausencias cumplidas.
// Si ninguno puede presentar once titulares, conserva el partido pendiente e informa el problema.

public class MatchSimulator {
    private static final int MATCH_LAST_MINUTE = 90;
    private static final int MAX_SUBSTITUTIONS = 5;

    private final MatchEventSimulator eventSimulator;
    private final PenaltyShootoutSimulator penaltySimulator;

    public MatchSimulator() {
        this(new Random());
    }

    public MatchSimulator(long seed) {
        this(new Random(seed));
    }

    private MatchSimulator(Random random) {
        eventSimulator = new MatchEventSimulator(random);
        penaltySimulator = new PenaltyShootoutSimulator(random);
    }

    public static int getMatchLastMinute() {
        return MATCH_LAST_MINUTE;
    }

    public static int getMaxSubstitutions() {
        return MAX_SUBSTITUTIONS;
    }

    public MatchEventSimulator getEventSimulator() {
        return eventSimulator;
    }

    public PenaltyShootoutSimulator getPenaltySimulator() {
        return penaltySimulator;
    }

    private List<Player> getPlayersAvailableForShootout(Match match, Team team, List<Player> fallbackPlayers) {
        List<Player> players = new ArrayList<>();
        for (PlayerParticipation participation : match.getPlayerParticipations()) {
            if (participation.getTeam() == team && participation.getExitMinute() >= MATCH_LAST_MINUTE
                    && !players.contains(participation.getPlayer())) {
                players.add(participation.getPlayer());
            }
        }
        if (players.isEmpty()) {
            players.addAll(fallbackPlayers);
        }
        return players;
    }

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
        if (!homeLineup.hasCompleteFormation() && !awayLineup.hasCompleteFormation()) {
            throw new IllegalStateException("Neither team can complete a starting lineup of 11 players.");
        }
        if (!homeLineup.hasCompleteFormation() || !awayLineup.hasCompleteFormation()) {
            match.forfeit(homeLineup.hasCompleteFormation() ? away : home);
            updatePreviousAbsences(home);
            updatePreviousAbsences(away);
            return;
        }
        match.setFormations(homeLineup.getFormation(), awayLineup.getFormation());
        match.setStartingLineups(homeLineup.getStarters(), awayLineup.getStarters());
        List<Player> homePitch = new ArrayList<>(homeLineup.getStarters());
        List<Player> awayPitch = new ArrayList<>(awayLineup.getStarters());
        List<Player> homeSubs = new ArrayList<>(homeLineup.getSubs());
        List<Player> awaySubs = new ArrayList<>(awayLineup.getSubs());
        Player[] homeGoalkeeper = {
                eventSimulator.findActiveGoalkeeper(homePitch)
        };
        Player[] awayGoalkeeper = {
                eventSimulator.findActiveGoalkeeper(awayPitch)
        };
        Map<Player, PlayerParticipation> participations = new HashMap<>();
        registerStartingPlayers(home, homePitch, participations, match);
        registerStartingPlayers(away, awayPitch, participations, match);
        updatePreviousAbsences(home);
        updatePreviousAbsences(away);
        Set<Player> matchYellows = new HashSet<>();
        int[] homeSubsCount = {0};
        int[] awaySubsCount = {0};
        eventSimulator.simulateMinutesRange(1, MATCH_LAST_MINUTE, match, homePitch, awayPitch, homeSubs, awaySubs,
                homeGoalkeeper, awayGoalkeeper, matchYellows, homeSubsCount, awaySubsCount, MAX_SUBSTITUTIONS,
                participations);
        if (match.getReferee() != null) {
            match.getReferee().addMatchOfficiated();
        }
        match.setPlayed(true);
        registerPlayerStats(match);
    }

    public void simulatePenaltyShootout(Match match) {
        if (match == null) {
            throw new IllegalArgumentException("Match cannot be null.");
        }
        if (!match.isPlayed()) {
            throw new IllegalStateException("Regular time must be played before penalties.");
        }
        if (!match.requiresTieBreak()) {
            throw new IllegalStateException("This match does not require a penalty shootout.");
        }
        List<Player> homePitch = getPlayersAvailableForShootout(match, match.getHomeTeam(), match.getHomeStarters());
        List<Player> awayPitch = getPlayersAvailableForShootout(match, match.getAwayTeam(), match.getAwayStarters());
        penaltySimulator.simulate(match, homePitch, awayPitch);
    }

    public void simulatePenaltyShootout(Match match, List<Player> homePitch, List<Player> awayPitch) {
        penaltySimulator.simulate(match, homePitch, awayPitch);
    }

    private void registerStartingPlayers(Team team, List<Player> starters, Map<Player,
            PlayerParticipation> participations, Match match) {
        for (Player player : starters) {
            PlayerParticipation participation = new PlayerParticipation(player, team, 0);
            participations.put(player, participation);
            match.addPlayerParticipation(participation);
        }
    }

    private void registerPlayerStats(Match match) {
        for (PlayerParticipation participation : match.getPlayerParticipations()) {
            Player player = participation.getPlayer();
            player.addMatchesPlayed();
            player.addMinutesPlayed(participation.getMinutesPlayed());
        }
    }

    private void updatePreviousAbsences(Team team) {
        for (Player player : team.getSquad()) {
            player.updateMatchAvailability();
        }
    }
}
