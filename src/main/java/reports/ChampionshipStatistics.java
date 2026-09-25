package reports;

import main.loader.TournamentData;
import model.match.*;
import model.participant.*;

import java.util.*;

/**
 * Fotografía estadística del campeonato al crear este servicio. Se utilizan
 * únicamente los partidos jugados; los acumulados históricos del JSON no se
 * mezclan con los rankings de esta edición.
 */
public class ChampionshipStatistics {
    public record PlayerStats(Player player, Team team, int matches, int minutes,
                              int goals, int penaltyGoals, int assists,
                              int yellowCards, int redCards, int goalsConceded) {
        public double averageGoalsConceded() {
            return matches == 0 ? 0.0 : (double) goalsConceded / matches;
        }
    }

    public record TeamStats(Team team, int played, int won, int drawn, int lost,
                            int goalsFor, int goalsAgainst, int yellowCards, int redCards) {
        public int points() { return 3 * won + drawn; }

        /** Porcentaje de puntos obtenidos sobre los posibles (tandas excluidas). */
        public double efficiency() { return played == 0 ? 0.0 : points() * 100.0 / (3 * played); }

        /** Criterio del reporte: una amarilla = 1, una roja = 3; menor es mejor. */
        public int fairPlayScore() { return yellowCards + 3 * redCards; }

        public double averageAge() {
            return team.getSquad().stream().mapToInt(Player::getAge).average().orElse(0.0);
        }
    }

    public record RefereeStats(Referee referee, int matches, Integer yearsOfExperience) { }

    private final List<PlayerStats> players;
    private final List<TeamStats> teams;
    private final List<RefereeStats> referees;
    private final int playedMatchCount;
    private final int incompleteMatchCount;
    private final boolean completeStartingLineups;
    private final boolean completeGoalInformation;

    public ChampionshipStatistics(TournamentData data) {
        this(new ReportData(data));
    }

    public ChampionshipStatistics(ReportData data) {
        Objects.requireNonNull(data, "El torneo es obligatorio");
        Map<Team, TeamAccumulator> teamTotals = new LinkedHashMap<>();
        Map<Player, PlayerAccumulator> playerTotals = new LinkedHashMap<>();
        Map<Referee, Integer> refereeTotals = new LinkedHashMap<>();
        for (Team team : data.getTeams()) {
            registerTeam(team, teamTotals, playerTotals);
        }
        data.getReferees().forEach(referee -> refereeTotals.put(referee, 0));

        // El set evita duplicar un encuentro registrado en más de un lugar.
        Set<Match> matches = new LinkedHashSet<>(data.getMatches());
        data.getGroups().forEach(group -> matches.addAll(group.getMatches()));
        int played = 0;
        int incomplete = 0;
        boolean lineupsComplete = true;
        boolean goalInformationComplete = true;
        for (Match match : matches) {
            if (!match.isPlayed()) continue;
            played++;
            registerTeam(match.getHomeTeam(), teamTotals, playerTotals);
            registerTeam(match.getAwayTeam(), teamTotals, playerTotals);
            teamTotals.get(match.getHomeTeam()).addResult(match.getHomeGoals(), match.getAwayGoals());
            teamTotals.get(match.getAwayTeam()).addResult(match.getAwayGoals(), match.getHomeGoals());
            if (match.getReferee() != null) refereeTotals.merge(match.getReferee(), 1, Integer::sum);
            ReportMatchDetails details = data.getMatchDetails(match);
            addPlayerParticipations(match, details, playerTotals);
            boolean missingGoalInformation = addEvents(match, details, teamTotals, playerTotals);
            long recordedGoals = match.getEvents().stream().filter(event -> event instanceof Goal).count();
            boolean missingLineup = !details.hasStartingPlayers();
            boolean scoreMismatch = recordedGoals != match.getHomeGoals() + match.getAwayGoals();
            if (missingLineup) lineupsComplete = false;
            if (missingGoalInformation || scoreMismatch) goalInformationComplete = false;
            if (missingLineup || missingGoalInformation || scoreMismatch) incomplete++;
        }
        playedMatchCount = played;
        incompleteMatchCount = incomplete;
        completeStartingLineups = lineupsComplete;
        completeGoalInformation = goalInformationComplete;
        players = playerTotals.values().stream().map(PlayerAccumulator::snapshot).toList();
        teams = teamTotals.values().stream().map(TeamAccumulator::snapshot).toList();
        referees = refereeTotals.entrySet().stream()
                .map(entry -> new RefereeStats(entry.getKey(), entry.getValue(),
                        data.getRefereeExperience(entry.getKey()))).toList();
    }

    public int getPlayedMatchCount() { return playedMatchCount; }

    /** Encuentros cuyos datos no permiten reconstruir todas las estadísticas. */
    public int getIncompleteMatchCount() { return incompleteMatchCount; }

    /** True when every completed match includes its starting lineups. */
    public boolean hasCompleteStartingLineups() { return completeStartingLineups; }

    /** True when every completed match has reconciled scores and goal details. */
    public boolean hasCompleteGoalInformation() { return completeGoalInformation; }

    public List<PlayerStats> getTopScorers() {
        return players.stream().filter(stats -> stats.goals() > 0)
                .sorted(Comparator.comparingInt(PlayerStats::goals).reversed()
                        .thenComparing(playerName()).thenComparing(stats -> stats.team().getName()))
                .toList();
    }

    public List<PlayerStats> getMinutesRanking() {
        return players.stream().sorted(Comparator.comparingInt(PlayerStats::minutes).reversed()
                .thenComparing(Comparator.comparingInt(PlayerStats::matches).reversed())
                .thenComparing(playerName()).thenComparing(stats -> stats.team().getName())).toList();
    }

    public List<TeamStats> getFairPlayRanking() {
        return teams.stream().sorted(Comparator.comparingInt(TeamStats::fairPlayScore)
                .thenComparingInt(TeamStats::redCards)
                .thenComparing(stats -> stats.team().getName(), String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public List<TeamStats> getTeamsAlphabetically() {
        return teams.stream().sorted(Comparator.comparing(stats -> stats.team().getName(),
                String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public List<RefereeStats> getRefereeRanking() {
        return referees.stream().sorted(Comparator.comparingInt(RefereeStats::matches).reversed()
                .thenComparing(stats -> stats.referee().getName(), String.CASE_INSENSITIVE_ORDER)).toList();
    }

    /** Promedio de años de experiencia de los árbitros con ese dato disponible. */
    public double getAverageRefereeExperience() {
        return referees.stream().filter(stats -> stats.yearsOfExperience() != null)
                .mapToInt(RefereeStats::yearsOfExperience).average().orElse(0.0);
    }

    public int getRefereesWithKnownExperienceCount() {
        return (int) referees.stream().filter(stats -> stats.yearsOfExperience() != null).count();
    }

    /** Un filtro null significa todas las posiciones o todos los jugadores. */
    public List<PlayerStats> getPlayers(Position position, Player selectedPlayer) {
        return players.stream().filter(stats -> position == null || stats.player().getPosition() == position)
                .filter(stats -> selectedPlayer == null || stats.player() == selectedPlayer)
                .sorted(playerName().thenComparing(stats -> stats.team().getName())).toList();
    }

    public PlayerStats getPlayerStats(Player player) {
        return players.stream().filter(stats -> stats.player() == player).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El jugador no pertenece al campeonato"));
    }

    private static Comparator<PlayerStats> playerName() {
        return Comparator.comparing(stats -> stats.player().getName(), String.CASE_INSENSITIVE_ORDER);
    }

    private static void registerTeam(Team team, Map<Team, TeamAccumulator> teams,
                                     Map<Player, PlayerAccumulator> players) {
        teams.computeIfAbsent(team, TeamAccumulator::new);
        team.getSquad().forEach(player -> players.putIfAbsent(player, new PlayerAccumulator(player, team)));
    }

    private static void addPlayerParticipations(Match match, ReportMatchDetails details,
                                                Map<Player, PlayerAccumulator> totals) {
        Map<Player, Integer> minutes = details.getPlayerMinutes(match);
        Set<Player> participants = new LinkedHashSet<>(minutes.keySet());
        // Los eventos también prueban participación si se cargó un partido sin formación.
        // En ese caso no permiten reconstruir los minutos de un titular desconocido.
        for (Event event : match.getEvents()) {
            participants.add(event.getPlayer());
            if (event instanceof Substitution substitution) participants.add(substitution.getPlayerIn());
            if (event instanceof Goal goal) {
                if (goal.getAssistPlayer() != null) participants.add(goal.getAssistPlayer());
                ReportMatchDetails.GoalDetails goalDetails = details.getGoalDetails(goal);
                if (goalDetails != null && goalDetails.receivingGoalkeeper() != null) {
                    participants.add(goalDetails.receivingGoalkeeper());
                }
            }
        }
        for (Player player : participants) {
            PlayerAccumulator total = totals.get(player);
            if (total != null) {
                total.matches++;
                total.minutes += minutes.getOrDefault(player, 0);
            }
        }
    }

    private static boolean addEvents(Match match, ReportMatchDetails details, Map<Team, TeamAccumulator> teams,
                                     Map<Player, PlayerAccumulator> players) {
        boolean incomplete = false;
        Map<Team, Set<Player>> onPitch = new HashMap<>();
        onPitch.put(match.getHomeTeam(), new LinkedHashSet<>(details.getHomeStarters()));
        onPitch.put(match.getAwayTeam(), new LinkedHashSet<>(details.getAwayStarters()));
        List<Event> events = match.getEvents().stream()
                .sorted(Comparator.comparingInt(Event::getMinute)).toList();
        for (Event event : events) {
            PlayerAccumulator player = players.get(event.getPlayer());
            TeamAccumulator team = teams.get(event.getTeam());
            if (event instanceof Goal goal) {
                ReportMatchDetails.GoalDetails goalDetails = details.getGoalDetails(goal);
                if (goalDetails == null) incomplete = true;
                boolean ownGoal = goalDetails != null ? goalDetails.ownGoal()
                        : player != null && player.team != goal.getTeam();
                boolean penalty = goalDetails != null && goalDetails.penalty();
                if (!ownGoal && player != null) {
                    player.goals++;
                    if (penalty) player.penaltyGoals++;
                    PlayerAccumulator assist = players.get(goal.getAssistPlayer());
                    if (!penalty && assist != null) assist.assists++;
                }
                Player receivingKeeper = goalDetails == null ? null : goalDetails.receivingGoalkeeper();
                if (receivingKeeper == null) {
                    Team concedingTeam = event.getTeam() == match.getHomeTeam()
                            ? match.getAwayTeam() : match.getHomeTeam();
                    receivingKeeper = onPitch.get(concedingTeam).stream()
                            .filter(candidate -> candidate.getPosition() == Position.GOALKEEPER)
                            .findFirst().orElse(null);
                }
                PlayerAccumulator keeper = players.get(receivingKeeper);
                if (keeper != null) keeper.goalsConceded++;
                else incomplete = true;
            } else if (event instanceof YellowCard) {
                if (player != null) player.yellowCards++;
                if (team != null) team.yellowCards++;
            } else if (event instanceof RedCard redCard) {
                if (player != null) player.redCards++;
                if (team != null) team.redCards++;
                // Compatibilidad con partidos previos: la segunda amarilla podía
                // representarse únicamente por una RedCard(false).
                boolean secondYellowRecorded = events.stream().anyMatch(candidate ->
                        candidate instanceof YellowCard && candidate.getPlayer() == event.getPlayer()
                                && candidate.getMinute() == event.getMinute());
                if (!redCard.isDirectRed() && !secondYellowRecorded) {
                    if (player != null) player.yellowCards++;
                    if (team != null) team.yellowCards++;
                }
                if (onPitch.containsKey(event.getTeam())) onPitch.get(event.getTeam()).remove(event.getPlayer());
            } else if (event instanceof Substitution substitution) {
                if (onPitch.containsKey(event.getTeam())) {
                    onPitch.get(event.getTeam()).remove(substitution.getPlayerOut());
                    onPitch.get(event.getTeam()).add(substitution.getPlayerIn());
                }
            }
            // PenaltyTaken pertenece a la tanda: no aporta goles, asistencias ni goles recibidos.
        }
        return incomplete;
    }

    private static class PlayerAccumulator {
        final Player player;
        final Team team;
        int matches, minutes, goals, penaltyGoals, assists, yellowCards, redCards, goalsConceded;

        PlayerAccumulator(Player player, Team team) { this.player = player; this.team = team; }

        PlayerStats snapshot() {
            return new PlayerStats(player, team, matches, minutes, goals, penaltyGoals,
                    assists, yellowCards, redCards, goalsConceded);
        }
    }

    private static class TeamAccumulator {
        final Team team;
        int played, won, drawn, lost, goalsFor, goalsAgainst, yellowCards, redCards;

        TeamAccumulator(Team team) { this.team = team; }

        void addResult(int scored, int conceded) {
            played++;
            goalsFor += scored;
            goalsAgainst += conceded;
            if (scored > conceded) won++;
            else if (scored == conceded) drawn++;
            else lost++;
        }

        TeamStats snapshot() {
            return new TeamStats(team, played, won, drawn, lost, goalsFor, goalsAgainst, yellowCards, redCards);
        }
    }
}
