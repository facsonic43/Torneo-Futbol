package control;

import model.match.Injury;
import model.match.Match;
import model.match.Substitution;
import model.participant.Player;
import model.participant.PlayerParticipation;
import model.participant.Position;
import model.participant.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

// Gestiona jugadores en cancha, suplentes, lesiones, cambios y expulsiones.
// Actualiza las participaciones y resuelve el reemplazo del arquero para mantener
// la continuidad del partido junto con el simulador de incidencias.

class MatchSquadManager {
    private Random random;

    MatchSquadManager(Random random) {
        this.random = random;
    }

    public Random getRandom() {
        return random;
    }

    void sendOffPlayer(int minute, Team team, Player player, List<Player> pitch, List<Player> subs,
            Player[] activeGoalkeeper, int[] substitutionsCount, int maxSubs, Match match, Map<Player,
            PlayerParticipation> participations) {
        boolean goalkeeperSentOff = player == activeGoalkeeper[0];
        endParticipation(player, minute, participations);
        pitch.remove(player);
        if (!goalkeeperSentOff) {
            return;
        }
        Player substituteGoalkeeper = findGoalkeeperSubstitute(subs);
        if (substituteGoalkeeper != null && substitutionsCount[0] < maxSubs && !pitch.isEmpty()) {
            Player outgoing = choosePlayerToLeave(pitch, null);
            if (outgoing != null) {
                endParticipation(outgoing, minute, participations);
                pitch.remove(outgoing);
                pitch.add(substituteGoalkeeper);
                subs.remove(substituteGoalkeeper);
                startParticipation(substituteGoalkeeper, team, minute, participations, match);
                match.addEvent(new Substitution(minute, team, outgoing, substituteGoalkeeper));
                substitutionsCount[0]++;
                activeGoalkeeper[0] = substituteGoalkeeper;
                return;
            }
        }
        activeGoalkeeper[0] = chooseEmergencyGoalkeeper(pitch);
    }

    void handleInjury(int minute, Team team, Player injuredPlayer, List<Player> pitch, List<Player> subs,
            Player[] activeGoalkeeper, int[] substitutionsCount, int maxSubs, Match match, Map<Player,
            PlayerParticipation> participations) {
        int matchesOut = random.nextInt(100) < 70 ? 1 : 2;
        injuredPlayer.injure(matchesOut);
        match.addEvent(new Injury(minute, team, injuredPlayer, matchesOut));
        boolean goalkeeperInjured = injuredPlayer == activeGoalkeeper[0];
        endParticipation(injuredPlayer, minute, participations);
        pitch.remove(injuredPlayer);
        if (!subs.isEmpty() && substitutionsCount[0] < maxSubs) {
            Player incoming;
            if (goalkeeperInjured) {
                incoming = findGoalkeeperSubstitute(subs);
                if (incoming == null) {
                    incoming = chooseSubstitute(subs, injuredPlayer);
                }
            } else {
                incoming = chooseSubstitute(subs, injuredPlayer);
            }
            if (incoming != null) {
                pitch.add(incoming);
                subs.remove(incoming);
                startParticipation(incoming, team, minute, participations, match);
                match.addEvent(new Substitution(minute, team, injuredPlayer, incoming));
                substitutionsCount[0]++;
                if (goalkeeperInjured) {
                    if (incoming.isGoalkeeper()) {
                        activeGoalkeeper[0] = incoming;
                    } else {
                        activeGoalkeeper[0] = chooseEmergencyGoalkeeper(pitch);
                    }
                }
                return;
            }
        }
        if (goalkeeperInjured) {
            activeGoalkeeper[0] = chooseEmergencyGoalkeeper(pitch);
        }
    }

    void executeSub(int minute, Team team, List<Player> pitch, List<Player> subs, Player activeGoalkeeper, Match match,
            int[] substitutionsCount, Map<Player, PlayerParticipation> participations) {
        Player outgoing = choosePlayerToLeave(pitch, activeGoalkeeper);
        if (outgoing == null) {
            return;
        }
        Player incoming = chooseSubstitute(subs, outgoing);
        if (incoming == null) {
            return;
        }
        endParticipation(outgoing, minute, participations);
        startParticipation(incoming, team, minute, participations, match);
        pitch.remove(outgoing);
        pitch.add(incoming);
        subs.remove(incoming);
        match.addEvent(new Substitution(minute, team, outgoing, incoming));
        substitutionsCount[0]++;
    }

    private Player choosePlayerToLeave(List<Player> pitch, Player activeGoalkeeper) {
        List<Player> candidates = new ArrayList<>(pitch);
        candidates.sort((player1, player2) -> Double.compare(player1.getOverall(), player2.getOverall()));
        for (Player player : candidates) {
            if (player != activeGoalkeeper && player.getPosition() != Position.GOALKEEPER) {
                return player;
            }
        }
        return null;
    }

    private Player chooseSubstitute(List<Player> subs, Player outgoing) {
        if (subs.isEmpty()) {
            return null;
        }
        if (outgoing.getPosition() == Position.GOALKEEPER) {
            Player goalkeeper = findGoalkeeperSubstitute(subs);
            if (goalkeeper != null) {
                return goalkeeper;
            }
        }
        List<Player> samePosition = new ArrayList<>();
        for (Player candidate : subs) {
            if (candidate.getPosition() == outgoing.getPosition()) {
                samePosition.add(candidate);
            }
        }
        if (!samePosition.isEmpty()) {
            return samePosition.get(random.nextInt(samePosition.size()));
        }
        for (Player candidate : subs) {
            if (candidate.getPosition() == Position.MIDFIELDER || candidate.getPosition() == Position.DEFENDER) {
                return candidate;
            }
        }
        for (Player candidate : subs) {
            if (candidate.getPosition() == Position.FORWARD) {
                return candidate;
            }
        }
        return subs.get(0);
    }

    private Player findGoalkeeperSubstitute(List<Player> subs) {
        for (Player player : subs) {
            if (player.isGoalkeeper()) {
                return player;
            }
        }
        return null;
    }

    Player findActiveGoalkeeper(List<Player> pitch) {
        for (Player player : pitch) {
            if (player.isGoalkeeper()) {
                return player;
            }
        }
        return chooseEmergencyGoalkeeper(pitch);
    }

    private Player chooseEmergencyGoalkeeper(List<Player> pitch) {
        Player selected = null;
        for (Player player : pitch) {
            if (player.getPosition() == Position.DEFENDER) {
                if (selected == null || player.getOverall() > selected.getOverall()) {
                    selected = player;
                }
            }
        }
        if (selected != null) {
            return selected;
        }
        for (Player player : pitch) {
            if (selected == null || player.getOverall() > selected.getOverall()) {
                selected = player;
            }
        }
        return selected;
    }

    private void startParticipation(Player player, Team team, int minute, Map<Player,
            PlayerParticipation> participations, Match match) {
        PlayerParticipation participation = new PlayerParticipation(player, team, minute);
        participations.put(player, participation);
        match.addPlayerParticipation(participation);
    }

    private void endParticipation(Player player, int minute, Map<Player, PlayerParticipation> participations) {
        PlayerParticipation participation = participations.get(player);
        if (participation != null) {
            participation.setExitMinute(minute);
        }
    }
}
