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
        Lineup homeLineup = new Lineup(home);
        Lineup awayLineup = new Lineup(away);

        List<Player> homePitch = new ArrayList<>(homeLineup.getStarters());
        List<Player> awayPitch = new ArrayList<>(awayLineup.getStarters());
        List<Player> homeSubs = new ArrayList<>(homeLineup.getSubs());
        List<Player> awaySubs = new ArrayList<>(awayLineup.getSubs());

        // Control de amonestados en ESTE partido (para detectar doble amarilla)
        Set<Player> matchYellows = new HashSet<>();

        // Control de jugadores que ingresaron desde el banco (para que no vuelvan a salir en cambios tácticos)
        Set<Player> subbedInPlayers = new HashSet<>();

        // Control de sustituciones realizadas
        int homeSubsCount = 0;
        int awaySubsCount = 0;
        int maxSubs = 5;

        // 2. Simular 90 minutos reglamentarios
        simulateMinutesRange(1, 90, match, homePitch, awayPitch, homeSubs, awaySubs,
                matchYellows, subbedInPlayers, homeSubsCount, awaySubsCount, maxSubs);

        // 3. Evaluar prórroga si el tipo de partido lo exige
        if (match.requiresTieBreak()) {
            match.setExtraTimePlayed(true);
            maxSubs = 6; // Cambio adicional permitido en tiempo suplementario

            simulateMinutesRange(91, 120, match, homePitch, awayPitch, homeSubs, awaySubs,
                    matchYellows, subbedInPlayers, homeSubsCount, awaySubsCount, maxSubs);
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
                                      Set<Player> subbedInPlayers,
                                      int homeSubsCount, int awaySubsCount, int maxSubs) {

        Team home = match.getHomeTeam();
        Team away = match.getAwayTeam();

        double homePower = home.getTeamPower();
        double awayPower = away.getTeamPower();

        // Factor aleatorio de inspiración/rendimiento en el día del partido (entre 0.80 y 1.20)
        double homeDayForm = 0.80 + (random.nextDouble() * 0.40);
        double awayDayForm = 0.80 + (random.nextDouble() * 0.40);

        double effectiveHomePower = homePower * homeDayForm;
        double effectiveAwayPower = awayPower * awayDayForm;
        double totalEffectivePower = effectiveHomePower + effectiveAwayPower;

        // Probabilidades de gol por minuto ajustadas (con componente base para sorpresas)
        double homeGoalChance = 0.005 + ((effectiveHomePower / totalEffectivePower) * 0.030);
        double awayGoalChance = 0.005 + ((effectiveAwayPower / totalEffectivePower) * 0.028);

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
                List<Player> subs = isHome ? homeSubs : awaySubs;
                Team team = isHome ? home : away;

                if (!pitch.isEmpty()) {
                    Player foulPlayer = pitch.get(random.nextInt(pitch.size()));
                    handleCard(min, team, foulPlayer, pitch, subs, matchYellows, subbedInPlayers, match);
                }
            }

            // --- D) LESIONES (0.2% de probabilidad por minuto) ---
            if (random.nextDouble() < 0.002) {
                boolean isHome = random.nextBoolean();
                List<Player> pitch = isHome ? homePitch : awayPitch;
                List<Player> subs = isHome ? homeSubs : awaySubs;
                Team team = isHome ? home : away;

                if (!pitch.isEmpty()) {
                    Player injuredPlayer = pitch.get(random.nextInt(pitch.size()));
                    int matchesOut = (random.nextInt(100) < 70) ? 1 : 2; // 70% 1 fecha, 30% 2 fechas
                    injuredPlayer.injure(matchesOut);
                    match.addEvent(new Injury(min, team, injuredPlayer, matchesOut));

                    // Si quedan cambios disponibles, entra un suplente acorde por el lesionado
                    if (!subs.isEmpty() && ((isHome && homeSubsCount < maxSubs) || (!isHome && awaySubsCount < maxSubs))) {
                        Player incoming = pickSubstituteFor(injuredPlayer, subs, pitch);
                        if (incoming != null) {
                            subs.remove(incoming);
                            pitch.remove(injuredPlayer);
                            pitch.add(incoming);
                            subbedInPlayers.add(incoming);
                            match.addEvent(new Substitution(min, team, injuredPlayer, incoming));
                            if (isHome) {
                                homeSubsCount++;
                            } else {
                                awaySubsCount++;
                            }
                        }
                    }
                }
            }

            // --- E) SUSTITUCIONES REGULARES (Minuto 55 en adelante, sólo jugadores de campo iniciales) ---
            if (min >= 55 && random.nextDouble() < 0.03) {
                if (homeSubsCount < maxSubs && !homeSubs.isEmpty() && !homePitch.isEmpty()) {
                    boolean subMade = executeFieldSub(min, home, homePitch, homeSubs, subbedInPlayers, match);
                    if (subMade) {
                        homeSubsCount++;
                    }
                }
                if (awaySubsCount < maxSubs && !awaySubs.isEmpty() && !awayPitch.isEmpty()) {
                    boolean subMade = executeFieldSub(min, away, awayPitch, awaySubs, subbedInPlayers, match);
                    if (subMade) {
                        awaySubsCount++;
                    }
                }
            }
        }
    }

    private void handleCard(int min, Team team, Player player, List<Player> pitch, List<Player> subs,
                            Set<Player> matchYellows, Set<Player> subbedInPlayers, Match match) {
        // Si ya tenía amarilla en este partido -> Doble amarilla = Expulsión inmediata
        if (matchYellows.contains(player)) {
            player.addRedCard();
            match.addEvent(new RedCard(min, team, player, false));
            pitch.remove(player);

            // Si el expulsado fue el arquero, el DT se ve forzado a hacer entrar al arquero suplente
            if (player.getPosition() == Position.GOALKEEPER) {
                handleGoalkeeperExpulsion(min, team, pitch, subs, subbedInPlayers, match);
            }
        } else {
            // Primera amarilla del partido
            matchYellows.add(player);
            player.addYellowCard();
            match.addEvent(new YellowCard(min, team, player));
        }
    }

    private void handleGoalkeeperExpulsion(int min, Team team, List<Player> pitch, List<Player> subs,
                                           Set<Player> subbedInPlayers, Match match) {
        Player subGoalkeeper = null;
        for (Player p : subs) {
            if (subGoalkeeper == null && p.getPosition() == Position.GOALKEEPER) {
                subGoalkeeper = p;
            }
        }

        // Si hay arquero suplente en el banco, saca a un jugador de campo para que entre el arquero
        if (subGoalkeeper != null) {
            Player sacrificedFieldPlayer = null;
            for (Player p : pitch) {
                if (p.getPosition() == Position.FORWARD) {
                    sacrificedFieldPlayer = p;
                }
            }
            if (sacrificedFieldPlayer == null) {
                for (Player p : pitch) {
                    if (p.getPosition() == Position.MIDFIELDER) {
                        sacrificedFieldPlayer = p;
                    }
                }
            }
            if (sacrificedFieldPlayer == null && !pitch.isEmpty()) {
                sacrificedFieldPlayer = pitch.get(0);
            }

            if (sacrificedFieldPlayer != null) {
                subs.remove(subGoalkeeper);
                pitch.remove(sacrificedFieldPlayer);
                pitch.add(subGoalkeeper);
                subbedInPlayers.add(subGoalkeeper);
                match.addEvent(new Substitution(min, team, sacrificedFieldPlayer, subGoalkeeper));
            }
        }
    }

    private boolean executeFieldSub(int min, Team team, List<Player> pitch, List<Player> subs,
                                    Set<Player> subbedInPlayers, Match match) {
        boolean executed = false;
        // Solo jugadores de campo que NO hayan ingresado desde el banco en este partido
        List<Player> eligibleStartersToExit = new ArrayList<>();
        for (Player p : pitch) {
            if (p.getPosition() != Position.GOALKEEPER && !subbedInPlayers.contains(p)) {
                eligibleStartersToExit.add(p);
            }
        }

        List<Player> fieldPlayersOnSubs = new ArrayList<>();
        for (Player p : subs) {
            if (p.getPosition() != Position.GOALKEEPER) {
                fieldPlayersOnSubs.add(p);
            }
        }

        if (!eligibleStartersToExit.isEmpty() && !fieldPlayersOnSubs.isEmpty()) {
            Player out = eligibleStartersToExit.get(random.nextInt(eligibleStartersToExit.size()));

            // Intentar reemplazar por un suplente de la misma posición preferentemente
            Player in = null;
            for (Player sub : fieldPlayersOnSubs) {
                if (in == null && sub.getPosition() == out.getPosition()) {
                    in = sub;
                }
            }
            // Si no hay de la misma posición, tomar cualquier jugador de campo suplente
            if (in == null) {
                in = fieldPlayersOnSubs.get(random.nextInt(fieldPlayersOnSubs.size()));
            }

            subs.remove(in);
            pitch.remove(out);
            pitch.add(in);
            subbedInPlayers.add(in); // Queda registrado como ingresado
            match.addEvent(new Substitution(min, team, out, in));
            executed = true;
        }

        return executed;
    }

    private Player pickSubstituteFor(Player injuredPlayer, List<Player> subs, List<Player> pitch) {
        Player replacement = null;

        if (injuredPlayer.getPosition() == Position.GOALKEEPER) {
            // Si se lesiona el arquero, debe entrar el arquero suplente obligatoriamente
            for (Player p : subs) {
                if (replacement == null && p.getPosition() == Position.GOALKEEPER) {
                    replacement = p;
                }
            }
        } else {
            // Si se lesiona un jugador de campo, busca reemplazo de campo
            for (Player p : subs) {
                if (replacement == null && p.getPosition() == injuredPlayer.getPosition()) {
                    replacement = p;
                }
            }
            if (replacement == null) {
                for (Player p : subs) {
                    if (replacement == null && p.getPosition() != Position.GOALKEEPER) {
                        replacement = p;
                    }
                }
            }
        }

        if (replacement == null && !subs.isEmpty()) {
            replacement = subs.get(0);
        }

        return replacement;
    }

    private Player pickScorer(List<Player> pitch) {
        List<Player> weightedList = new ArrayList<>();
        for (Player p : pitch) {
            if (p.getPosition() == Position.FORWARD) {
                weightedList.add(p);
                weightedList.add(p);
                weightedList.add(p);
            } else if (p.getPosition() == Position.MIDFIELDER) {
                weightedList.add(p);
                weightedList.add(p);
            } else if (p.getPosition() == Position.DEFENDER) {
                weightedList.add(p);
            }
        }
        if (weightedList.isEmpty()) {
            return pitch.get(0);
        }
        return weightedList.get(random.nextInt(weightedList.size()));
    }

    private Player pickAssister(List<Player> pitch, Player scorer) {
        if (random.nextBoolean() && pitch.size() > 1) {
            Player candidate = pitch.get(random.nextInt(pitch.size()));
            if (candidate != scorer && candidate.getPosition() != Position.GOALKEEPER) {
                return candidate;
            }
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

            if (homeScore) {
                homePens++;
                homeTaker.addGoal();
            }
            if (awayScore) {
                awayPens++;
                awayTaker.addGoal();
            }

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

            if (homeScore) {
                homePens++;
                homeTaker.addGoal();
            }
            if (awayScore) {
                awayPens++;
                awayTaker.addGoal();
            }

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
