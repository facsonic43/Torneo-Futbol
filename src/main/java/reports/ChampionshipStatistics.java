package reports;

import main.loader.TournamentData;
import model.match.*;
import model.participant.*;

import java.util.*;

// Construye una fotografía de las estadísticas de esta edición a partir de los partidos jugados,
// sin repetir encuentros ni mezclar los acumulados históricos del JSON con los rankings actuales.
// Los resultados administrativos cuentan para los equipos, sin sumar actividad a jugadores o árbitros.
// Reconstruye participaciones e incidencias y señala los partidos con formaciones o goles incompletos.
// Los eventos permiten identificar participantes sin formación, aunque no reconstruir sus minutos.
// Incluye eficiencia por puntos, juego limpio (una amarilla vale uno y una roja tres) y experiencia
// arbitral conocida. Los penales de desempate no suman goles y se contemplan registros antiguos
// de segunda amarilla. Los filtros de posición o jugador nulos incluyen todos los participantes.

public class ChampionshipStatistics {
    public record PlayerStats(Player player, Team team, int matches, int minutes, int goals, int penaltyGoals,
            int assists, int yellowCards, int redCards, int goalsConceded) {
        double averageGoalsConceded() {
            return matches == 0 ? 0.0 : (double) goalsConceded / matches;
        }
    }

    public record TeamStats(Team team, int played, int won, int drawn, int lost, int goalsFor, int goalsAgainst,
            int yellowCards, int redCards) {
        public int points() { return 3 * won + drawn; }

        double efficiency() { return played == 0 ? 0.0 : points() * 100.0 / (3 * played); }

        public int fairPlayScore() { return yellowCards + 3 * redCards; }

        double averageAge() {
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
            if (match.isForfeited()) continue;
            if (match.getReferee() != null) refereeTotals.merge(match.getReferee(), 1, Integer::sum);
            ReportMatchDetails details = data.getMatchDetails(match);
            addPlayerParticipations(match, details, playerTotals);
            boolean missingGoalInformation = addEvents(match, details, teamTotals, playerTotals);
            long recordedGoals = match.getGoals().size();
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

    public int getIncompleteMatchCount() { return incompleteMatchCount; }

    public boolean hasCompleteStartingLineups() { return completeStartingLineups; }

    public boolean hasCompleteGoalInformation() { return completeGoalInformation; }

    public List<PlayerStats> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public List<TeamStats> getTeams() {
        return Collections.unmodifiableList(teams);
    }

    public List<RefereeStats> getReferees() {
        return Collections.unmodifiableList(referees);
    }

    public List<PlayerStats> getTopScorers() {
        return players.stream().filter(stats -> stats.goals() > 0)
                .sorted(Comparator.comparingInt(PlayerStats::goals).reversed() .thenComparing(playerName()).thenComparing(stats -> stats.team().getName()))
                .toList();
    }

    public List<PlayerStats> getMinutesRanking() {
        return players.stream().sorted(Comparator.comparingInt(PlayerStats::minutes).reversed() .thenComparing(Comparator.comparingInt(PlayerStats::matches).reversed()) .thenComparing(playerName()).thenComparing(stats -> stats.team().getName())).toList();
    }

    public List<TeamStats> getFairPlayRanking() {
        return teams.stream().sorted(Comparator.comparingInt(TeamStats::fairPlayScore).thenComparingInt(TeamStats::redCards).thenComparing(stats -> stats.team().getName(),
                String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public List<TeamStats> getTeamsAlphabetically() {
        return teams.stream().sorted(Comparator.comparing(stats -> stats.team().getName(),
                String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public List<RefereeStats> getRefereeRanking() {
        return referees.stream().sorted(Comparator.comparingInt(RefereeStats::matches).reversed().thenComparing(stats -> stats.referee().getName(),
                String.CASE_INSENSITIVE_ORDER)).toList();
    }

    double getAverageRefereeExperience() {
        return referees.stream().filter(stats -> stats.yearsOfExperience() != null)
                .mapToInt(RefereeStats::yearsOfExperience).average().orElse(0.0);
    }

    int getRefereesWithKnownExperienceCount() {
        return (int) referees.stream().filter(stats -> stats.yearsOfExperience() != null).count();
    }

    List<PlayerStats> getPlayers(Position position, Player selectedPlayer) {
        return players.stream().filter(stats -> position == null || stats.player().getPosition() == position)
                .filter(stats -> selectedPlayer == null || stats.player() == selectedPlayer)
                .sorted(playerName().thenComparing(stats -> stats.team().getName())).toList();
    }

    PlayerStats getPlayerStats(Player player) {
        return players.stream().filter(stats -> stats.player() == player).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El jugador no pertenece al campeonato"));
    }

    private static Comparator<PlayerStats> playerName() {
        return Comparator.comparing(stats -> stats.player().getName(), String.CASE_INSENSITIVE_ORDER);
    }

    private static final class EventProcessor implements EventVisitor {
        private final Match match;
        private final ReportMatchDetails details;
        private final Map<Team, TeamAccumulator> teams;
        private final Map<Player, PlayerAccumulator> players;
        private final List<Event> events;
        private final List<YellowCard> yellowCards;
        private final Map<Team, Set<Player>> onPitch = new HashMap<>();
        private boolean incomplete;
        private PlayerAccumulator player;
        private TeamAccumulator team;

        EventProcessor(Match match, ReportMatchDetails details, Map<Team, TeamAccumulator> teams, Map<Player,
                PlayerAccumulator> players) {
            this.match = match;
            this.details = details;
            this.teams = teams;
            this.players = players;
            onPitch.put(match.getHomeTeam(), new LinkedHashSet<>(details.getHomeStarters()));
            onPitch.put(match.getAwayTeam(), new LinkedHashSet<>(details.getAwayStarters()));
            events = match.getEvents().stream().sorted(Comparator.comparingInt(Event::getMinute)).toList();
            yellowCards = match.getYellowCards();
        }

        public Match getMatch() {
            return match;
        }

        public ReportMatchDetails getDetails() {
            return details;
        }

        public Map<Team, TeamAccumulator> getTeams() {
            return Collections.unmodifiableMap(teams);
        }

        public Map<Player, PlayerAccumulator> getPlayers() {
            return Collections.unmodifiableMap(players);
        }

        public List<Event> getEvents() {
            return Collections.unmodifiableList(events);
        }

        public List<YellowCard> getYellowCards() {
            return Collections.unmodifiableList(yellowCards);
        }

        public Map<Team, Set<Player>> getOnPitch() {
            return Collections.unmodifiableMap(onPitch);
        }

        public boolean isIncomplete() {
            return incomplete;
        }

        public PlayerAccumulator getPlayer() {
            return player;
        }

        public TeamAccumulator getTeam() {
            return team;
        }

        boolean process() {
            for (Event event : events) {
                player = players.get(event.getPlayer());
                team = teams.get(event.getTeam());
                event.accept(this);
            }
            return incomplete;
        }

        @Override
        public void visit(Goal goal) {
            ReportMatchDetails.GoalDetails goalDetails = details.getGoalDetails(goal);
            boolean matchHasRecordedStarters = match.hasRecordedStartingPlayers() || details.hasStartingPlayers();
            boolean canInferGoalContext = matchHasRecordedStarters && !events.isEmpty();
            if (goalDetails == null && !canInferGoalContext) incomplete = true;
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
                Team concedingTeam = goal.getTeam() == match.getHomeTeam()
                        ? match.getAwayTeam() : match.getHomeTeam();
                receivingKeeper = onPitch.get(concedingTeam).stream()
                        .filter(Player::isGoalkeeper)
                        .findFirst().orElse(null);
            }
            PlayerAccumulator keeper = players.get(receivingKeeper);
            if (keeper != null) keeper.goalsConceded++;
            else if (!canInferGoalContext) incomplete = true;
        }

        @Override
        public void visit(YellowCard yellowCard) {
            if (player != null) player.yellowCards++;
            if (team != null) team.yellowCards++;
        }

        @Override
        public void visit(RedCard redCard) {
            if (player != null) player.redCards++;
            if (team != null) team.redCards++;
            boolean secondYellowRecorded = yellowCards.stream().anyMatch(candidate -> candidate.getPlayer() == redCard.getPlayer()
                    && candidate.getMinute() == redCard.getMinute());
            if (!redCard.isDirectRed() && !secondYellowRecorded) {
                if (player != null) player.yellowCards++;
                if (team != null) team.yellowCards++;
            }
            if (onPitch.containsKey(redCard.getTeam())) onPitch.get(redCard.getTeam()).remove(redCard.getPlayer());
        }

        @Override
        public void visit(Substitution substitution) {
            if (onPitch.containsKey(substitution.getTeam())) {
                onPitch.get(substitution.getTeam()).remove(substitution.getPlayerOut());
                onPitch.get(substitution.getTeam()).add(substitution.getPlayerIn());
            }
        }
    }

    private static class PlayerAccumulator {
        private final Player player;
        private final Team team;
        private int matches, minutes, goals, penaltyGoals, assists, yellowCards, redCards, goalsConceded;

        PlayerAccumulator(Player player, Team team) { this.player = player; this.team = team; }

        public Player getPlayer() {
            return player;
        }

        public Team getTeam() {
            return team;
        }

        public int getMatches() {
            return matches;
        }

        public int getMinutes() {
            return minutes;
        }

        public int getGoals() {
            return goals;
        }

        public int getPenaltyGoals() {
            return penaltyGoals;
        }

        public int getAssists() {
            return assists;
        }

        public int getYellowCards() {
            return yellowCards;
        }

        public int getRedCards() {
            return redCards;
        }

        public int getGoalsConceded() {
            return goalsConceded;
        }

        PlayerStats snapshot() {
            return new PlayerStats(player, team, matches, minutes, goals, penaltyGoals, assists, yellowCards, redCards,
                    goalsConceded);
        }
    }

    private static class TeamAccumulator {
        private final Team team;
        private int played, won, drawn, lost, goalsFor, goalsAgainst, yellowCards, redCards;

        TeamAccumulator(Team team) { this.team = team; }

        public Team getTeam() {
            return team;
        }

        public int getPlayed() {
            return played;
        }

        public int getWon() {
            return won;
        }

        public int getDrawn() {
            return drawn;
        }

        public int getLost() {
            return lost;
        }

        public int getGoalsFor() {
            return goalsFor;
        }

        public int getGoalsAgainst() {
            return goalsAgainst;
        }

        public int getYellowCards() {
            return yellowCards;
        }

        public int getRedCards() {
            return redCards;
        }

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

    private static void registerTeam(Team team, Map<Team, TeamAccumulator> teams, Map<Player,
            PlayerAccumulator> players) {
        teams.computeIfAbsent(team, TeamAccumulator::new);
        team.getSquad().forEach(player -> players.putIfAbsent(player, new PlayerAccumulator(player, team)));
    }

    private static void addPlayerParticipations(Match match, ReportMatchDetails details, Map<Player,
            PlayerAccumulator> totals) {
        Map<Player, Integer> minutes = details.getPlayerMinutes(match);
        Set<Player> participants = new LinkedHashSet<>(minutes.keySet());
        for (Event event : match.getEvents()) {
            participants.add(event.getPlayer());
            event.accept(new EventVisitor() {
                @Override
                public void visit(Substitution substitution) {
                    participants.add(substitution.getPlayerIn());
                }
                @Override
                public void visit(Goal goal) {
                    if (goal.getAssistPlayer() != null) participants.add(goal.getAssistPlayer());
                    ReportMatchDetails.GoalDetails goalDetails = details.getGoalDetails(goal);
                    if (goalDetails != null && goalDetails.receivingGoalkeeper() != null) {
                        participants.add(goalDetails.receivingGoalkeeper());
                    }
                }
            });
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
        return new EventProcessor(match, details, teams, players).process();
    }
}
