package control;

import model.match.Goal;
import model.match.Injury;
import model.match.Match;
import model.match.RedCard;
import model.match.Substitution;
import model.match.YellowCard;
import model.participant.Goalkeeper;
import model.participant.Player;
import model.participant.PlayerParticipation;
import model.participant.Position;
import model.participant.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/*
 * Simula las incidencias que pueden ocurrir durante los 90 minutos.
 * Maneja goles, tarjetas, expulsiones, lesiones, cambios
 * y los jugadores que se encuentran actualmente en cancha.
 */
public class MatchEventSimulator {

    private Random random;

    public MatchEventSimulator(
            Random random) {

        this.random =
                random;
    }

    public void simulateMinutesRange(
            int startMinute,
            int endMinute,
            Match match,
            List<Player> homePitch,
            List<Player> awayPitch,
            List<Player> homeSubs,
            List<Player> awaySubs,
            Player[] homeGoalkeeper,
            Player[] awayGoalkeeper,
            Set<Player> matchYellows,
            int[] homeSubsCount,
            int[] awaySubsCount,
            int maxSubs,
            Map<Player, PlayerParticipation> participations) {

        Team home =
                match.getHomeTeam();

        Team away =
                match.getAwayTeam();

        double homePower =
                home.getTeamPower();

        double awayPower =
                away.getTeamPower();

        double totalPower =
                homePower
                        + awayPower;

        double homeGoalChance =
                (homePower / totalPower)
                        * 0.04;

        double awayGoalChance =
                (awayPower / totalPower)
                        * 0.03;

        for (int minute = startMinute;
             minute <= endMinute;
             minute++) {

            simulatePossibleGoal(
                    minute,
                    match,
                    home,
                    away,
                    homePitch,
                    awayPitch,
                    homeGoalkeeper,
                    awayGoalkeeper,
                    homeGoalChance,
                    awayGoalChance
            );

            simulatePossibleYellowCard(
                    minute,
                    match,
                    home,
                    away,
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

            simulatePossibleDirectRed(
                    minute,
                    match,
                    home,
                    away,
                    homePitch,
                    awayPitch,
                    homeSubs,
                    awaySubs,
                    homeGoalkeeper,
                    awayGoalkeeper,
                    homeSubsCount,
                    awaySubsCount,
                    maxSubs,
                    participations
            );

            simulatePossibleInjury(
                    minute,
                    match,
                    home,
                    away,
                    homePitch,
                    awayPitch,
                    homeSubs,
                    awaySubs,
                    homeGoalkeeper,
                    awayGoalkeeper,
                    homeSubsCount,
                    awaySubsCount,
                    maxSubs,
                    participations
            );

            simulatePossibleSubstitutions(
                    minute,
                    match,
                    home,
                    away,
                    homePitch,
                    awayPitch,
                    homeSubs,
                    awaySubs,
                    homeGoalkeeper,
                    awayGoalkeeper,
                    homeSubsCount,
                    awaySubsCount,
                    maxSubs,
                    participations
            );
        }
    }

    private void simulatePossibleGoal(
            int minute,
            Match match,
            Team home,
            Team away,
            List<Player> homePitch,
            List<Player> awayPitch,
            Player[] homeGoalkeeper,
            Player[] awayGoalkeeper,
            double homeGoalChance,
            double awayGoalChance) {

        double roll =
                random.nextDouble();

        if (roll < homeGoalChance
                && !homePitch.isEmpty()) {

            simulateGoal(
                    minute,
                    home,
                    homePitch,
                    awayPitch,
                    awayGoalkeeper[0],
                    match,
                    true
            );

        } else if (roll
                < homeGoalChance
                + awayGoalChance
                && !awayPitch.isEmpty()) {

            simulateGoal(
                    minute,
                    away,
                    awayPitch,
                    homePitch,
                    homeGoalkeeper[0],
                    match,
                    false
            );
        }
    }

    private void simulatePossibleYellowCard(
            int minute,
            Match match,
            Team home,
            Team away,
            List<Player> homePitch,
            List<Player> awayPitch,
            List<Player> homeSubs,
            List<Player> awaySubs,
            Player[] homeGoalkeeper,
            Player[] awayGoalkeeper,
            Set<Player> matchYellows,
            int[] homeSubsCount,
            int[] awaySubsCount,
            int maxSubs,
            Map<Player, PlayerParticipation> participations) {

        if (random.nextDouble()
                >= 0.018) {

            return;
        }

        boolean homeTeamCard =
                random.nextBoolean();

        List<Player> pitch =
                homeTeamCard
                        ? homePitch
                        : awayPitch;

        List<Player> subs =
                homeTeamCard
                        ? homeSubs
                        : awaySubs;

        Team team =
                homeTeamCard
                        ? home
                        : away;

        Player[] activeGoalkeeper =
                homeTeamCard
                        ? homeGoalkeeper
                        : awayGoalkeeper;

        int[] substitutionsCount =
                homeTeamCard
                        ? homeSubsCount
                        : awaySubsCount;

        if (pitch.isEmpty()) {
            return;
        }

        Player player =
                pitch.get(
                        random.nextInt(
                                pitch.size()
                        )
                );

        handleCard(
                minute,
                team,
                player,
                pitch,
                subs,
                activeGoalkeeper,
                matchYellows,
                match,
                substitutionsCount,
                maxSubs,
                participations
        );
    }

    private void simulatePossibleDirectRed(
            int minute,
            Match match,
            Team home,
            Team away,
            List<Player> homePitch,
            List<Player> awayPitch,
            List<Player> homeSubs,
            List<Player> awaySubs,
            Player[] homeGoalkeeper,
            Player[] awayGoalkeeper,
            int[] homeSubsCount,
            int[] awaySubsCount,
            int maxSubs,
            Map<Player, PlayerParticipation> participations) {

        if (random.nextDouble()
                >= 0.004) {

            return;
        }

        boolean homeTeamCard =
                random.nextBoolean();

        List<Player> pitch =
                homeTeamCard
                        ? homePitch
                        : awayPitch;

        List<Player> subs =
                homeTeamCard
                        ? homeSubs
                        : awaySubs;

        Team team =
                homeTeamCard
                        ? home
                        : away;

        Player[] activeGoalkeeper =
                homeTeamCard
                        ? homeGoalkeeper
                        : awayGoalkeeper;

        int[] substitutionsCount =
                homeTeamCard
                        ? homeSubsCount
                        : awaySubsCount;

        if (pitch.isEmpty()) {
            return;
        }

        Player player =
                pitch.get(
                        random.nextInt(
                                pitch.size()
                        )
                );

        player.addRedCard();

        match.addEvent(
                new RedCard(
                        minute,
                        team,
                        player,
                        true
                )
        );

        sendOffPlayer(
                minute,
                team,
                player,
                pitch,
                subs,
                activeGoalkeeper,
                substitutionsCount,
                maxSubs,
                match,
                participations
        );
    }

    private void simulatePossibleInjury(
            int minute,
            Match match,
            Team home,
            Team away,
            List<Player> homePitch,
            List<Player> awayPitch,
            List<Player> homeSubs,
            List<Player> awaySubs,
            Player[] homeGoalkeeper,
            Player[] awayGoalkeeper,
            int[] homeSubsCount,
            int[] awaySubsCount,
            int maxSubs,
            Map<Player, PlayerParticipation> participations) {

        if (random.nextDouble()
                >= 0.002) {

            return;
        }

        boolean homeTeamInjury =
                random.nextBoolean();

        List<Player> pitch =
                homeTeamInjury
                        ? homePitch
                        : awayPitch;

        List<Player> subs =
                homeTeamInjury
                        ? homeSubs
                        : awaySubs;

        Team team =
                homeTeamInjury
                        ? home
                        : away;

        Player[] activeGoalkeeper =
                homeTeamInjury
                        ? homeGoalkeeper
                        : awayGoalkeeper;

        int[] substitutionsCount =
                homeTeamInjury
                        ? homeSubsCount
                        : awaySubsCount;

        if (pitch.isEmpty()) {
            return;
        }

        Player injuredPlayer =
                pitch.get(
                        random.nextInt(
                                pitch.size()
                        )
                );

        handleInjury(
                minute,
                team,
                injuredPlayer,
                pitch,
                subs,
                activeGoalkeeper,
                substitutionsCount,
                maxSubs,
                match,
                participations
        );
    }

    private void simulatePossibleSubstitutions(
            int minute,
            Match match,
            Team home,
            Team away,
            List<Player> homePitch,
            List<Player> awayPitch,
            List<Player> homeSubs,
            List<Player> awaySubs,
            Player[] homeGoalkeeper,
            Player[] awayGoalkeeper,
            int[] homeSubsCount,
            int[] awaySubsCount,
            int maxSubs,
            Map<Player, PlayerParticipation> participations) {

        if (minute < 55
                || random.nextDouble() >= 0.12) {

            return;
        }

        if (homeSubsCount[0] < maxSubs
                && !homeSubs.isEmpty()
                && !homePitch.isEmpty()) {

            executeSub(
                    minute,
                    home,
                    homePitch,
                    homeSubs,
                    homeGoalkeeper[0],
                    match,
                    homeSubsCount,
                    participations
            );
        }

        if (awaySubsCount[0] < maxSubs
                && !awaySubs.isEmpty()
                && !awayPitch.isEmpty()) {

            executeSub(
                    minute,
                    away,
                    awayPitch,
                    awaySubs,
                    awayGoalkeeper[0],
                    match,
                    awaySubsCount,
                    participations
            );
        }
    }

    private void simulateGoal(
            int minute,
            Team scoringTeam,
            List<Player> attackingPitch,
            List<Player> defendingPitch,
            Player goalkeeperConceded,
            Match match,
            boolean homeGoal) {

        boolean ownGoal =
                random.nextDouble()
                        < 0.03;

        boolean penalty =
                !ownGoal
                        && random.nextDouble()
                        < 0.15;

        Player scorer;
        Player assist = null;

        if (ownGoal) {

            scorer =
                    pickOwnGoalPlayer(
                            defendingPitch
                    );

        } else {

            scorer =
                    pickScorer(
                            attackingPitch
                    );

            scorer.addGoal();

            if (penalty) {

                scorer.addPenaltyGoal();

            } else {

                assist =
                        pickAssister(
                                attackingPitch,
                                scorer
                        );

                if (assist != null) {
                    assist.addAssist();
                }
            }
        }

        if (goalkeeperConceded
                instanceof Goalkeeper) {

            ((Goalkeeper) goalkeeperConceded)
                    .addGoalConceded();
        }

        if (homeGoal) {

            match.setHomeGoals(
                    match.getHomeGoals()
                            + 1
            );

        } else {

            match.setAwayGoals(
                    match.getAwayGoals()
                            + 1
            );
        }

        match.addEvent(
                new Goal(
                        minute,
                        scoringTeam,
                        scorer,
                        assist,
                        penalty,
                        ownGoal,
                        goalkeeperConceded
                )
        );
    }

    private void handleCard(
            int minute,
            Team team,
            Player player,
            List<Player> pitch,
            List<Player> subs,
            Player[] activeGoalkeeper,
            Set<Player> matchYellows,
            Match match,
            int[] substitutionsCount,
            int maxSubs,
            Map<Player, PlayerParticipation> participations) {

        if (matchYellows.contains(
                player
        )) {

            player.addYellowCard();

            match.addEvent(
                    new YellowCard(
                            minute,
                            team,
                            player
                    )
            );

            player.addRedCard();

            match.addEvent(
                    new RedCard(
                            minute,
                            team,
                            player,
                            false
                    )
            );

            sendOffPlayer(
                    minute,
                    team,
                    player,
                    pitch,
                    subs,
                    activeGoalkeeper,
                    substitutionsCount,
                    maxSubs,
                    match,
                    participations
            );

        } else {

            matchYellows.add(
                    player
            );

            player.addYellowCard();

            match.addEvent(
                    new YellowCard(
                            minute,
                            team,
                            player
                    )
            );
        }
    }

    // Expulsa al jugador y reorganiza el arco si el expulsado era el arquero.
    private void sendOffPlayer(
            int minute,
            Team team,
            Player player,
            List<Player> pitch,
            List<Player> subs,
            Player[] activeGoalkeeper,
            int[] substitutionsCount,
            int maxSubs,
            Match match,
            Map<Player, PlayerParticipation> participations) {

        boolean goalkeeperSentOff =
                player
                        == activeGoalkeeper[0];

        endParticipation(
                player,
                minute,
                participations
        );

        pitch.remove(
                player
        );

        if (!goalkeeperSentOff) {
            return;
        }

        Player substituteGoalkeeper =
                findGoalkeeperSubstitute(
                        subs
                );

        if (substituteGoalkeeper != null
                && substitutionsCount[0] < maxSubs
                && !pitch.isEmpty()) {

            Player outgoing =
                    choosePlayerToLeave(
                            pitch,
                            null
                    );

            if (outgoing != null) {

                endParticipation(
                        outgoing,
                        minute,
                        participations
                );

                pitch.remove(
                        outgoing
                );

                pitch.add(
                        substituteGoalkeeper
                );

                subs.remove(
                        substituteGoalkeeper
                );

                startParticipation(
                        substituteGoalkeeper,
                        team,
                        minute,
                        participations,
                        match
                );

                match.addEvent(
                        new Substitution(
                                minute,
                                team,
                                outgoing,
                                substituteGoalkeeper
                        )
                );

                substitutionsCount[0]++;

                activeGoalkeeper[0] =
                        substituteGoalkeeper;

                return;
            }
        }

        activeGoalkeeper[0] =
                chooseEmergencyGoalkeeper(
                        pitch
                );
    }

    private void handleInjury(
            int minute,
            Team team,
            Player injuredPlayer,
            List<Player> pitch,
            List<Player> subs,
            Player[] activeGoalkeeper,
            int[] substitutionsCount,
            int maxSubs,
            Match match,
            Map<Player, PlayerParticipation> participations) {

        int matchesOut =
                random.nextInt(100) < 70
                        ? 1
                        : 2;

        injuredPlayer.injure(
                matchesOut
        );

        match.addEvent(
                new Injury(
                        minute,
                        team,
                        injuredPlayer,
                        matchesOut
                )
        );

        boolean goalkeeperInjured =
                injuredPlayer
                        == activeGoalkeeper[0];

        endParticipation(
                injuredPlayer,
                minute,
                participations
        );

        pitch.remove(
                injuredPlayer
        );

        if (!subs.isEmpty()
                && substitutionsCount[0] < maxSubs) {

            Player incoming;

            if (goalkeeperInjured) {

                incoming =
                        findGoalkeeperSubstitute(
                                subs
                        );

                if (incoming == null) {

                    incoming =
                            chooseSubstitute(
                                    subs,
                                    injuredPlayer
                            );
                }

            } else {

                incoming =
                        chooseSubstitute(
                                subs,
                                injuredPlayer
                        );
            }

            if (incoming != null) {

                pitch.add(
                        incoming
                );

                subs.remove(
                        incoming
                );

                startParticipation(
                        incoming,
                        team,
                        minute,
                        participations,
                        match
                );

                match.addEvent(
                        new Substitution(
                                minute,
                                team,
                                injuredPlayer,
                                incoming
                        )
                );

                substitutionsCount[0]++;

                if (goalkeeperInjured) {

                    if (incoming
                            instanceof Goalkeeper) {

                        activeGoalkeeper[0] =
                                incoming;

                    } else {

                        activeGoalkeeper[0] =
                                chooseEmergencyGoalkeeper(
                                        pitch
                                );
                    }
                }

                return;
            }
        }

        if (goalkeeperInjured) {

            activeGoalkeeper[0] =
                    chooseEmergencyGoalkeeper(
                            pitch
                    );
        }
    }

    private void executeSub(
            int minute,
            Team team,
            List<Player> pitch,
            List<Player> subs,
            Player activeGoalkeeper,
            Match match,
            int[] substitutionsCount,
            Map<Player, PlayerParticipation> participations) {

        Player outgoing =
                choosePlayerToLeave(
                        pitch,
                        activeGoalkeeper
                );

        if (outgoing == null) {
            return;
        }

        Player incoming =
                chooseSubstitute(
                        subs,
                        outgoing
                );

        if (incoming == null) {
            return;
        }

        endParticipation(
                outgoing,
                minute,
                participations
        );

        startParticipation(
                incoming,
                team,
                minute,
                participations,
                match
        );

        pitch.remove(
                outgoing
        );

        pitch.add(
                incoming
        );

        subs.remove(
                incoming
        );

        match.addEvent(
                new Substitution(
                        minute,
                        team,
                        outgoing,
                        incoming
                )
        );

        substitutionsCount[0]++;
    }

    private Player choosePlayerToLeave(
            List<Player> pitch,
            Player activeGoalkeeper) {

        List<Player> candidates =
                new ArrayList<>(
                        pitch
                );

        candidates.sort(
                (player1, player2) ->
                        Double.compare(
                                player1.getOverall(),
                                player2.getOverall()
                        )
        );

        for (Player player :
                candidates) {

            if (player != activeGoalkeeper
                    && player.getPosition()
                    != Position.GOALKEEPER) {

                return player;
            }
        }

        return null;
    }

    private Player chooseSubstitute(
            List<Player> subs,
            Player outgoing) {

        if (subs.isEmpty()) {
            return null;
        }

        if (outgoing.getPosition()
                == Position.GOALKEEPER) {

            Player goalkeeper =
                    findGoalkeeperSubstitute(
                            subs
                    );

            if (goalkeeper != null) {
                return goalkeeper;
            }
        }

        List<Player> samePosition =
                new ArrayList<>();

        for (Player candidate :
                subs) {

            if (candidate.getPosition()
                    == outgoing.getPosition()) {

                samePosition.add(
                        candidate
                );
            }
        }

        if (!samePosition.isEmpty()) {

            return samePosition.get(
                    random.nextInt(
                            samePosition.size()
                    )
            );
        }

        for (Player candidate :
                subs) {

            if (candidate.getPosition()
                    == Position.MIDFIELDER
                    || candidate.getPosition()
                    == Position.DEFENDER) {

                return candidate;
            }
        }

        for (Player candidate :
                subs) {

            if (candidate.getPosition()
                    == Position.FORWARD) {

                return candidate;
            }
        }

        return subs.get(0);
    }

    private Player findGoalkeeperSubstitute(
            List<Player> subs) {

        for (Player player :
                subs) {

            if (player
                    instanceof Goalkeeper) {

                return player;
            }
        }

        return null;
    }

    // Busca el arquero real o designa uno de emergencia.
    public Player findActiveGoalkeeper(
            List<Player> pitch) {

        for (Player player :
                pitch) {

            if (player
                    instanceof Goalkeeper) {

                return player;
            }
        }

        return chooseEmergencyGoalkeeper(
                pitch
        );
    }

    private Player chooseEmergencyGoalkeeper(
            List<Player> pitch) {

        Player selected =
                null;

        for (Player player :
                pitch) {

            if (player.getPosition()
                    == Position.DEFENDER) {

                if (selected == null
                        || player.getOverall()
                        > selected.getOverall()) {

                    selected =
                            player;
                }
            }
        }

        if (selected != null) {
            return selected;
        }

        for (Player player :
                pitch) {

            if (selected == null
                    || player.getOverall()
                    > selected.getOverall()) {

                selected =
                        player;
            }
        }

        return selected;
    }

    private Player pickScorer(
            List<Player> pitch) {

        List<Player> weightedList =
                new ArrayList<>();

        for (Player player :
                pitch) {

            int previousGoalsWeight =
                    1
                            + player.getGoals()
                            * 3;

            int positionWeight;

            if (player.getPosition()
                    == Position.FORWARD) {

                positionWeight = 5;

            } else if (player.getPosition()
                    == Position.MIDFIELDER) {

                positionWeight = 2;

            } else {

                positionWeight = 1;
            }

            for (int i = 0;
                 i < positionWeight
                         * previousGoalsWeight;
                 i++) {

                weightedList.add(
                        player
                );
            }
        }

        if (weightedList.isEmpty()) {
            return pitch.get(0);
        }

        return weightedList.get(
                random.nextInt(
                        weightedList.size()
                )
        );
    }

    private Player pickAssister(
            List<Player> pitch,
            Player scorer) {

        if (pitch.size() < 2) {
            return null;
        }

        List<Player> candidates =
                new ArrayList<>();

        for (Player player :
                pitch) {

            if (player != scorer
                    && player.getPosition()
                    != Position.GOALKEEPER) {

                candidates.add(
                        player
                );
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        if (random.nextDouble()
                < 0.68) {

            return candidates.get(
                    random.nextInt(
                            candidates.size()
                    )
            );
        }

        return null;
    }

    private Player pickOwnGoalPlayer(
            List<Player> defendingPitch) {

        List<Player> candidates =
                new ArrayList<>();

        for (Player player :
                defendingPitch) {

            if (player.getPosition()
                    != Position.GOALKEEPER) {

                candidates.add(
                        player
                );
            }
        }

        if (candidates.isEmpty()) {

            return defendingPitch.get(
                    random.nextInt(
                            defendingPitch.size()
                    )
            );
        }

        return candidates.get(
                random.nextInt(
                        candidates.size()
                )
        );
    }

    private void startParticipation(
            Player player,
            Team team,
            int minute,
            Map<Player, PlayerParticipation> participations,
            Match match) {

        PlayerParticipation participation =
                new PlayerParticipation(
                        player,
                        team,
                        false,
                        minute
                );

        participations.put(
                player,
                participation
        );

        match.addPlayerParticipation(
                participation
        );
    }

    private void endParticipation(
            Player player,
            int minute,
            Map<Player, PlayerParticipation> participations) {

        PlayerParticipation participation =
                participations.get(
                        player
                );

        if (participation != null) {

            participation.setExitMinute(
                    minute
            );
        }
    }
}