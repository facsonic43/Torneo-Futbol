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
        // 1. Armamos las formaciones de ambos equipos
        Lineup homeLineup = new Lineup(home, away);
        Lineup awayLineup = new Lineup(away, home);
        match.setFormations(homeLineup.getFormation(), awayLineup.getFormation());
        match.setStartingPlayers(List.copyOf(homeLineup.getStarters()), List.copyOf(awayLineup.getStarters()));

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
        // Control de sustituciones realizadas
        int[] homeSubsCount = {0};
        int[] awaySubsCount = {0};
        int maxSubs = 5;

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
    private void simulateMinutesRange(int startMin, int endMin, Match match,
                                      List<Player> homePitch, List<Player> awayPitch,
                                      List<Player> homeSubs, List<Player> awaySubs,
                                      Set<Player> matchYellows,
                                      int[] homeSubsCount, int[] awaySubsCount, int maxSubs) {

        Team home = match.getHomeTeam();
        Team away = match.getAwayTeam();

        double homePower = home.getTeamPower();
        double awayPower = away.getTeamPower();
        double totalPower = homePower + awayPower;

        // Probabilidad por minuto con ventaja de localía directa
        double homeGoalChance = (homePower / totalPower) * 0.04;
        double awayGoalChance = (awayPower / totalPower) * 0.03;

        for (int min = startMin; min <= endMin; min++) {
            double roll = random.nextDouble();

            // --- A) GOL LOCAL ---
            if (roll < homeGoalChance && !homePitch.isEmpty()) {
                Player scorer = pickScorer(homePitch);
                Player assist = pickAssister(homePitch, scorer);

                scorer.addGoal();
                if (assist != null) {
                    assist.addAssist();
                }

                match.setHomeGoals(match.getHomeGoals() + 1);
                match.addEvent(new Goal(min, home, scorer, assist));
            }
            // --- B) GOL VISITANTE ---
            else if (roll < (homeGoalChance + awayGoalChance) && !awayPitch.isEmpty()) {
                Player scorer = pickScorer(awayPitch);
                Player assist = pickAssister(awayPitch, scorer);

                scorer.addGoal();
                if (assist != null) {
                    assist.addAssist();
                }

                match.setAwayGoals(match.getAwayGoals() + 1);
                match.addEvent(new Goal(min, away, scorer, assist));
            }

            // --- C) TARJETAS (1.8% de probabilidad por minuto) ---
            if (random.nextDouble() < 0.018) {
                boolean isHome = random.nextBoolean();
                List<Player> pitch = isHome ? homePitch : awayPitch;
                Team team = isHome ? home : away;

                if (!pitch.isEmpty()) {
                    Player foulPlayer = pitch.get(random.nextInt(pitch.size()));
                    handleCard(min, team, foulPlayer, pitch, matchYellows, match);
                }
            }

            // --- D) ROJAS DIRECTAS (0.4% de probabilidad por minuto) ---
            if (random.nextDouble() < 0.004) {
                boolean isHome = random.nextBoolean();
                List<Player> pitch = isHome ? homePitch : awayPitch;
                Team team = isHome ? home : away;

                if (!pitch.isEmpty()) {
                    Player foulPlayer = pitch.get(random.nextInt(pitch.size()));
                    if (!matchYellows.contains(foulPlayer)) {
                        foulPlayer.addRedCard();
                        match.addEvent(new RedCard(min, team, foulPlayer, true));
                        pitch.remove(foulPlayer);
                    }
                }
            }

            // --- E) LESIONES (0.2% de probabilidad por minuto) ---
            if (random.nextDouble() < 0.002) {
                boolean isHome = random.nextBoolean();
                List<Player> pitch = isHome ? homePitch : awayPitch;
                List<Player> subs = isHome ? homeSubs : awaySubs;
                Team team = isHome ? home : away;

                if (!pitch.isEmpty()) {
                    Player injuredPlayer = pitch.get(random.nextInt(pitch.size()));
                    int matchesOut = (random.nextInt(100) < 70) ? 1 : 2;
                    injuredPlayer.injure(matchesOut);
                    match.addEvent(new Injury(min, team, injuredPlayer, matchesOut));

                    if (!subs.isEmpty() && ((isHome && homeSubsCount[0] < maxSubs) || (!isHome && awaySubsCount[0] < maxSubs))) {
                        Player incoming = chooseSubstitute(subs, injuredPlayer);
                        if (incoming != null) {
                            pitch.remove(injuredPlayer);
                            pitch.add(incoming);
                            subs.remove(incoming);
                            match.addEvent(new Substitution(min, team, injuredPlayer, incoming));
                            if (isHome) homeSubsCount[0]++; else awaySubsCount[0]++;
                        }
                    }
                }
            }

            // --- F) SUSTITUCIONES REGULARES (Minuto 55 en adelante) ---
            if (min >= 55 && random.nextDouble() < 0.12) {
                if (homeSubsCount[0] < maxSubs && !homeSubs.isEmpty() && !homePitch.isEmpty()) {
                    executeSub(min, home, homePitch, homeSubs, match, homeSubsCount);
                }
                if (awaySubsCount[0] < maxSubs && !awaySubs.isEmpty() && !awayPitch.isEmpty()) {
                    executeSub(min, away, awayPitch, awaySubs, match, awaySubsCount);
                }
            }
        }
    }

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
    private void executeSub(int min, Team team, List<Player> pitch, List<Player> subs, Match match, int[] subsCount) {
        Player out = choosePlayerToLeave(pitch);
        if (out == null) {
            return;
        }

        Player in = chooseSubstitute(subs, out);
        if (in == null) {
            return;
        }

        pitch.remove(out);
        pitch.add(in);
        subs.remove(in);
        match.addEvent(new Substitution(min, team, out, in));
        subsCount[0]++;
    }

    private Player choosePlayerToLeave(List<Player> pitch) {
        List<Player> candidates = new ArrayList<>(pitch);
        candidates.sort((a, b) -> Double.compare(b.getOverall(), a.getOverall()));

        for (Player player : candidates) {
            if (player.getPosition() != Position.GOALKEEPER) {
                return player;
            }
        }

        return pitch.isEmpty() ? null : pitch.get(0);
    }

    private Player chooseSubstitute(List<Player> subs, Player outgoing) {
        if (subs.isEmpty()) {
            return null;
        }

        if (outgoing.getPosition() == Position.GOALKEEPER) {
            for (Player candidate : subs) {
                if (candidate.getPosition() == Position.GOALKEEPER) {
                    return candidate;
                }
            }
        }

        List<Player> candidates = new ArrayList<>();
        for (Player candidate : subs) {
            if (candidate.getPosition() == outgoing.getPosition()) {
                candidates.add(candidate);
            }
        }
        if (!candidates.isEmpty()) {
            return candidates.get(random.nextInt(candidates.size()));
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

    private Player pickScorer(List<Player> pitch) {
        List<Player> weightedList = new ArrayList<>();
        for (Player p : pitch) {
            int previousGoalsWeight = 1 + (p.getGoals() * 3);
            int positionWeight;
            if (p.getPosition() == Position.FORWARD) {
                positionWeight = 5;
            } else if (p.getPosition() == Position.MIDFIELDER) {
                positionWeight = 2;
            } else if (p.getPosition() == Position.DEFENDER) {
                positionWeight = 1;
            } else {
                positionWeight = 1;
            }
            for (int i = 0; i < positionWeight * previousGoalsWeight; i++) {
                weightedList.add(p);
            }
        }
        if (weightedList.isEmpty()) {
            return pitch.get(0);
        }
    }

    private void registerPlayerStats(
            Map<Player, PlayerParticipation> participations) {
    private Player pickAssister(List<Player> pitch, Player scorer) {
        if (pitch.size() < 2) {
            return null;
        }

        List<Player> candidates = new ArrayList<>();
        for (Player p : pitch) {
            if (p != scorer && p.getPosition() != Position.GOALKEEPER) {
                candidates.add(p);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        if (random.nextDouble() < 0.68) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        return null;
    }

    public void simulatePenaltyShootout(Match match, List<Player> homePitch, List<Player> awayPitch) {
        int homePens = 0;
        int awayPens = 0;

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