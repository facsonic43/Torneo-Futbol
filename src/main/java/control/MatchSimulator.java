package control;

import model.match.*;
import model.participant.Player;
import model.participant.Position;
import model.participant.Team;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MatchSimulator {

    private Random random = new Random();

    public void simulateMatch(Match match) {
        Team home = match.getHomeTeam();
        Team away = match.getAwayTeam();

        // 1. Armamos las formaciones de ambos equipos
        Lineup homeLineup = new Lineup(home, away);
        Lineup awayLineup = new Lineup(away, home);
        match.setFormations(homeLineup.getFormation(), awayLineup.getFormation());

        List<Player> homePitch = new ArrayList<>(homeLineup.getStarters());
        List<Player> awayPitch = new ArrayList<>(awayLineup.getStarters());
        List<Player> homeSubs = new ArrayList<>(homeLineup.getSubs());
        List<Player> awaySubs = new ArrayList<>(awayLineup.getSubs());

        // Control de amonestados en ESTE partido (para detectar doble amarilla)
        Set<Player> matchYellows = new HashSet<>();

        // Control de sustituciones realizadas
        int[] homeSubsCount = {0};
        int[] awaySubsCount = {0};
        int maxSubs = 5;

        // 2. Simular 90 minutos reglamentarios
        simulateMinutesRange(1, 90, match, homePitch, awayPitch, homeSubs, awaySubs,
                matchYellows, homeSubsCount, awaySubsCount, maxSubs);

        // 3. Evaluar prórroga si el tipo de partido lo exige
        if (match.requiresTieBreak()) {
            match.setExtraTimePlayed(true);
            maxSubs = 6; // Cambio adicional permitido en tiempo suplementario

            simulateMinutesRange(91, 120, match, homePitch, awayPitch, homeSubs, awaySubs,
                    matchYellows, homeSubsCount, awaySubsCount, maxSubs);
        }

        // 4. Si persiste la necesidad de desempate tras los 120', vamos a tanda de penales
        if (match.requiresTieBreak()) {
            simulatePenaltyShootout(match, homePitch, awayPitch);
        }

        // 5. Cierre del encuentro y registro de minutos/partidos
        match.setPlayed(true);
        int totalMinutes = match.isExtraTimePlayed() ? 120 : 90;
        registerPlayerStats(homePitch, awayPitch, totalMinutes);
    }

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

    private void handleCard(int min, Team team, Player player, List<Player> pitch, Set<Player> matchYellows, Match match) {
        // Si ya tenía amarilla en este partido -> Doble amarilla = Expulsión inmediata
        if (matchYellows.contains(player)) {
            player.addRedCard();
            match.addEvent(new RedCard(min, team, player, false));
            pitch.remove(player);
        } else {
            // Primera amarilla del partido (si llega a 3 del torneo, Player maneja su sanción a partir del próximo)
            matchYellows.add(player);
            player.addYellowCard();
            match.addEvent(new YellowCard(min, team, player));
        }
    }

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
        return weightedList.get(random.nextInt(weightedList.size()));
    }

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

        // Serie de 5 penales
        for (int i = 0; i < 5; i++) {
            Player homeTaker = homePitch.get(i % homePitch.size());
            Player awayTaker = awayPitch.get(i % awayPitch.size());

            boolean homeScore = random.nextDouble() < 0.75;
            boolean awayScore = random.nextDouble() < 0.75;

            if (homeScore) { homePens++; homeTaker.addGoal(); }
            if (awayScore) { awayPens++; awayTaker.addGoal(); }

            match.addEvent(new PenaltyTaken(120, match.getHomeTeam(), homeTaker, homeScore));
            match.addEvent(new PenaltyTaken(120, match.getAwayTeam(), awayTaker, awayScore));
        }

        // Muerte súbita en caso de persistir el empate
        int round = 5;
        while (homePens == awayPens) {
            Player homeTaker = homePitch.get(round % homePitch.size());
            Player awayTaker = awayPitch.get(round % awayPitch.size());

            boolean homeScore = random.nextDouble() < 0.75;
            boolean awayScore = random.nextDouble() < 0.75;

            if (homeScore) { homePens++; homeTaker.addGoal(); }
            if (awayScore) { awayPens++; awayTaker.addGoal(); }

            match.addEvent(new PenaltyTaken(120, match.getHomeTeam(), homeTaker, homeScore));
            match.addEvent(new PenaltyTaken(120, match.getAwayTeam(), awayTaker, awayScore));
            round++;
        }

        match.setHomePenalties(homePens);
        match.setAwayPenalties(awayPens);
    }

    private void registerPlayerStats(List<Player> homePitch, List<Player> awayPitch, int minutes) {
        for (Player p : homePitch) {
            p.addMatchesPlayed();
            p.addMinutesPlayed(minutes);
        }
        for (Player p : awayPitch) {
            p.addMatchesPlayed();
            p.addMinutesPlayed(minutes);
        }
    }
}