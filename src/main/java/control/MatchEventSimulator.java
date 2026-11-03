package control;

import model.match.Match;
import model.match.RedCard;
import model.match.YellowCard;
import model.participant.Player;
import model.participant.PlayerParticipation;
import model.participant.Team;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Simula las incidencias de los 90 minutos y actualiza jugadores y marcador.
 * Coordina goles, tarjetas, lesiones y sustituciones mediante colaboradores que comparten
 * el generador aleatorio y conservan el orden de los acontecimientos.
 */
public class MatchEventSimulator {
    private Random random;
    private GoalEventSimulator goalSimulator;
    private MatchSquadManager squadManager;

    public MatchEventSimulator(Random random) {
        this.random = random;
        this.goalSimulator = new GoalEventSimulator(random);
        this.squadManager = new MatchSquadManager(random);
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
        Team home = match.getHomeTeam();
        Team away = match.getAwayTeam();
        double homePower = home.getTeamPower();
        double awayPower = away.getTeamPower();
        double totalPower = homePower + awayPower;
        double homeGoalChance = (homePower / totalPower) * 0.04;
        double awayGoalChance = (awayPower / totalPower) * 0.03;

        for (int minute = startMinute; minute <= endMinute; minute++) {
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
        double roll = random.nextDouble();

        if (roll < homeGoalChance && !homePitch.isEmpty()) {
            goalSimulator.simulateGoal(minute, home, homePitch, awayPitch, awayGoalkeeper[0], match, true);
        } else if (roll < homeGoalChance + awayGoalChance && !awayPitch.isEmpty()) {
            goalSimulator.simulateGoal(minute, away, awayPitch, homePitch, homeGoalkeeper[0], match, false);
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
        if (random.nextDouble() >= 0.018) {
            return;
        }

        boolean homeTeamCard = random.nextBoolean();
        List<Player> pitch = homeTeamCard ? homePitch : awayPitch;
        List<Player> subs = homeTeamCard ? homeSubs : awaySubs;
        Team team = homeTeamCard ? home : away;
        Player[] activeGoalkeeper = homeTeamCard ? homeGoalkeeper : awayGoalkeeper;
        int[] substitutionsCount = homeTeamCard ? homeSubsCount : awaySubsCount;

        if (pitch.isEmpty()) {
            return;
        }

        Player player = pitch.get(random.nextInt(pitch.size()));
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
        if (random.nextDouble() >= 0.004) {
            return;
        }

        boolean homeTeamCard = random.nextBoolean();
        List<Player> pitch = homeTeamCard ? homePitch : awayPitch;
        List<Player> subs = homeTeamCard ? homeSubs : awaySubs;
        Team team = homeTeamCard ? home : away;
        Player[] activeGoalkeeper = homeTeamCard ? homeGoalkeeper : awayGoalkeeper;
        int[] substitutionsCount = homeTeamCard ? homeSubsCount : awaySubsCount;

        if (pitch.isEmpty()) {
            return;
        }

        Player player = pitch.get(random.nextInt(pitch.size()));
        player.addRedCard();
        match.addEvent(new RedCard(minute, team, player, true));
        squadManager.sendOffPlayer(
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
        if (random.nextDouble() >= 0.002) {
            return;
        }

        boolean homeTeamInjury = random.nextBoolean();
        List<Player> pitch = homeTeamInjury ? homePitch : awayPitch;
        List<Player> subs = homeTeamInjury ? homeSubs : awaySubs;
        Team team = homeTeamInjury ? home : away;
        Player[] activeGoalkeeper = homeTeamInjury ? homeGoalkeeper : awayGoalkeeper;
        int[] substitutionsCount = homeTeamInjury ? homeSubsCount : awaySubsCount;

        if (pitch.isEmpty()) {
            return;
        }

        Player injuredPlayer = pitch.get(random.nextInt(pitch.size()));
        squadManager.handleInjury(
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
        if (minute < 55 || random.nextDouble() >= 0.12) {
            return;
        }

        if (homeSubsCount[0] < maxSubs && !homeSubs.isEmpty() && !homePitch.isEmpty()) {
            squadManager.executeSub(
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

        if (awaySubsCount[0] < maxSubs && !awaySubs.isEmpty() && !awayPitch.isEmpty()) {
            squadManager.executeSub(
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
        if (matchYellows.contains(player)) {
            player.addYellowCard();
            match.addEvent(new YellowCard(minute, team, player));
            player.addRedCard();
            match.addEvent(new RedCard(minute, team, player, false));
            squadManager.sendOffPlayer(
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
            matchYellows.add(player);
            player.addYellowCard();
            match.addEvent(new YellowCard(minute, team, player));
        }
    }

    public Player findActiveGoalkeeper(List<Player> pitch) {
        return squadManager.findActiveGoalkeeper(pitch);
    }
}
