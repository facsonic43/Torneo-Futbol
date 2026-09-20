package report;

import model.match.Event;
import model.match.Match;
import model.match.RedCard;
import model.match.YellowCard;
import model.participant.Goalkeeper;
import model.participant.Player;
import model.participant.Position;
import model.participant.Referee;
import model.participant.Team;
import model.tournament.Tournament;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Genera los reportes estadísticos del campeonato.
 * Incluye goleadores, Fair Play, minutos, equipos,
 * árbitros y jugadores. report 2,3,4,6,7,8
 */
public class StatisticsReportService {

    public String generateScorersRanking(
            List<Team> teams) {

        List<Player> players =
                getAllPlayers(
                        teams
                );

        players.removeIf(
                player ->
                        player.getGoals() == 0
        );

        players.sort(
                Comparator
                        .comparingInt(
                                Player::getGoals
                        )
                        .reversed()
                        .thenComparing(
                                Player::getName
                        )
        );

        StringBuilder report =
                new StringBuilder();

        report.append(
                "\n====================================\n"
        );

        report.append(
                "REPORT II - TOP SCORERS\n"
        );

        report.append(
                "====================================\n"
        );

        report.append(
                String.format(
                        "%-4s | %-25s | %-25s | %-6s | %-8s%n",
                        "POS",
                        "PLAYER",
                        "TEAM",
                        "GOALS",
                        "PENALTY"
                )
        );

        for (int i = 0;
             i < players.size();
             i++) {

            Player player =
                    players.get(i);

            Team team =
                    findPlayerTeam(
                            teams,
                            player
                    );

            report.append(
                    String.format(
                            "%-4d | %-25s | %-25s | %-6d | %-8d%n",
                            i + 1,
                            player.getName(),
                            team == null
                                    ? "-"
                                    : team.getName(),
                            player.getGoals(),
                            player.getPenaltyGoals()
                    )
            );
        }

        return report.toString();
    }

    public String generateFairPlayRanking(
            List<Team> teams,
            List<Match> matches) {

        Map<Team, Integer> yellowCards =
                new HashMap<>();

        Map<Team, Integer> directReds =
                new HashMap<>();

        Map<Team, Integer> secondYellowReds =
                new HashMap<>();

        initializeFairPlayMaps(
                teams,
                yellowCards,
                directReds,
                secondYellowReds
        );

        countCards(
                matches,
                yellowCards,
                directReds,
                secondYellowReds
        );

        List<Team> orderedTeams =
                new ArrayList<>(
                        teams
                );

        orderedTeams.sort(
                (team1, team2) ->
                        compareFairPlayTeams(
                                team1,
                                team2,
                                yellowCards,
                                directReds,
                                secondYellowReds
                        )
        );

        StringBuilder report =
                new StringBuilder();

        report.append(
                "\n====================================\n"
        );

        report.append(
                "REPORT III - FAIR PLAY RANKING\n"
        );

        report.append(
                "====================================\n"
        );

        report.append(
                "Criterion: Yellow Card = 1 point, Red Card = 3 points. "
                        + "Lower score is better.\n\n"
        );

        report.append(
                String.format(
                        "%-4s | %-27s | %-7s | %-10s | %-12s | %-7s%n",
                        "POS",
                        "TEAM",
                        "YELLOW",
                        "DIRECT RED",
                        "2ND YELLOW",
                        "POINTS"
                )
        );

        for (int i = 0;
             i < orderedTeams.size();
             i++) {

            Team team =
                    orderedTeams.get(i);

            int points =
                    getFairPlayPoints(
                            team,
                            yellowCards,
                            directReds,
                            secondYellowReds
                    );

            report.append(
                    String.format(
                            "%-4d | %-27s | %-7d | %-10d | %-12d | %-7d%n",
                            i + 1,
                            team.getName(),
                            yellowCards.get(team),
                            directReds.get(team),
                            secondYellowReds.get(team),
                            points
                    )
            );
        }

        return report.toString();
    }

    public String generateMinutesRanking(
            List<Team> teams) {

        List<Player> players =
                getAllPlayers(
                        teams
                );

        players.removeIf(
                player ->
                        player.getMatchesPlayed()
                                == 0
        );

        players.sort(
                Comparator
                        .comparingInt(
                                Player::getMinutesPlayed
                        )
                        .reversed()
                        .thenComparing(
                                Comparator
                                        .comparingInt(
                                                Player::getMatchesPlayed
                                        )
                                        .reversed()
                        )
                        .thenComparing(
                                Player::getName
                        )
        );

        StringBuilder report =
                new StringBuilder();

        report.append(
                "\n====================================\n"
        );

        report.append(
                "REPORT IV - MOST MINUTES PLAYED\n"
        );

        report.append(
                "====================================\n"
        );

        report.append(
                String.format(
                        "%-4s | %-25s | %-25s | %-8s | %-8s%n",
                        "POS",
                        "PLAYER",
                        "TEAM",
                        "MATCHES",
                        "MINUTES"
                )
        );

        for (int i = 0;
             i < players.size();
             i++) {

            Player player =
                    players.get(i);

            Team team =
                    findPlayerTeam(
                            teams,
                            player
                    );

            report.append(
                    String.format(
                            "%-4d | %-25s | %-25s | %-8d | %-8d%n",
                            i + 1,
                            player.getName(),
                            team == null
                                    ? "-"
                                    : team.getName(),
                            player.getMatchesPlayed(),
                            player.getMinutesPlayed()
                    )
            );
        }

        return report.toString();
    }

    public String generateTeamsReport(
            Tournament tournament) {

        List<Team> teams =
                new ArrayList<>(
                        tournament.getTeams()
                );

        teams.sort(
                Comparator.comparing(
                        Team::getName,
                        String.CASE_INSENSITIVE_ORDER
                )
        );

        List<Match> matches =
                tournament.getAllMatches();

        StringBuilder report =
                new StringBuilder();

        report.append(
                "\n====================================\n"
        );

        report.append(
                "REPORT VI - TEAMS\n"
        );

        report.append(
                "====================================\n"
        );

        report.append(
                String.format(
                        "%-27s | %-8s | %-8s | %-16s | %-4s | %-4s | %-7s%n",
                        "TEAM",
                        "AVG AGE",
                        "DT AGE",
                        "DT NATIONALITY",
                        "GF",
                        "GA",
                        "EFFECT."
                )
        );

        for (Team team :
                teams) {

            appendTeamStatistics(
                    report,
                    team,
                    matches
            );
        }

        return report.toString();
    }

    public String generateRefereesRanking(
            List<Referee> referees,
            List<Match> matches) {

        Map<Referee, Integer> matchesByReferee =
                new HashMap<>();

        for (Referee referee :
                referees) {

            matchesByReferee.put(
                    referee,
                    0
            );
        }

        for (Match match :
                matches) {

            if (match.isPlayed()
                    && match.getReferee()
                    != null) {

                Referee referee =
                        match.getReferee();

                matchesByReferee.put(
                        referee,
                        matchesByReferee
                                .getOrDefault(
                                        referee,
                                        0
                                )
                                + 1
                );
            }
        }

        List<Referee> orderedReferees =
                new ArrayList<>(
                        referees
                );

        orderedReferees.sort(
                Comparator
                        .comparingInt(
                                (Referee referee) ->
                                        matchesByReferee
                                                .getOrDefault(
                                                        referee,
                                                        0
                                                )
                        )
                        .reversed()
                        .thenComparing(
                                Referee::getName
                        )
        );

        return buildRefereeReport(
                orderedReferees,
                matchesByReferee
        );
    }

    public String generatePlayersReport(
            List<Team> teams) {

        return generatePlayersReport(
                teams,
                null
        );
    }

    public String generatePlayersReport(
            List<Team> teams,
            Position position) {

        List<Player> players =
                getAllPlayers(
                        teams
                );

        if (position != null) {

            players.removeIf(
                    player ->
                            player.getPosition()
                                    != position
            );
        }

        players.sort(
                Comparator.comparing(
                        Player::getName,
                        String.CASE_INSENSITIVE_ORDER
                )
        );

        StringBuilder report =
                new StringBuilder();

        report.append(
                "\n====================================\n"
        );

        report.append(
                "REPORT VIII - PLAYERS\n"
        );

        report.append(
                "====================================\n"
        );

        report.append(
                "Position: "
                        + (position == null
                        ? "ALL"
                        : position)
                        + "\n"
        );

        for (Player player :
                players) {

            Team team =
                    findPlayerTeam(
                            teams,
                            player
                    );

            appendPlayer(
                    report,
                    player,
                    team
            );
        }

        return report.toString();
    }

    private void initializeFairPlayMaps(
            List<Team> teams,
            Map<Team, Integer> yellowCards,
            Map<Team, Integer> directReds,
            Map<Team, Integer> secondYellowReds) {

        for (Team team :
                teams) {

            yellowCards.put(
                    team,
                    0
            );

            directReds.put(
                    team,
                    0
            );

            secondYellowReds.put(
                    team,
                    0
            );
        }
    }

    private void countCards(
            List<Match> matches,
            Map<Team, Integer> yellowCards,
            Map<Team, Integer> directReds,
            Map<Team, Integer> secondYellowReds) {

        for (Match match :
                matches) {

            for (Event event :
                    match.getEvents()) {

                Team team =
                        event.getTeam();

                if (event
                        instanceof YellowCard) {

                    yellowCards.put(
                            team,
                            yellowCards
                                    .getOrDefault(
                                            team,
                                            0
                                    )
                                    + 1
                    );
                }

                if (event
                        instanceof RedCard) {

                    RedCard redCard =
                            (RedCard) event;

                    if (redCard.isDirectRed()) {

                        directReds.put(
                                team,
                                directReds
                                        .getOrDefault(
                                                team,
                                                0
                                        )
                                        + 1
                        );

                    } else {

                        secondYellowReds.put(
                                team,
                                secondYellowReds
                                        .getOrDefault(
                                                team,
                                                0
                                        )
                                        + 1
                        );
                    }
                }
            }
        }
    }

    private int compareFairPlayTeams(
            Team team1,
            Team team2,
            Map<Team, Integer> yellowCards,
            Map<Team, Integer> directReds,
            Map<Team, Integer> secondYellowReds) {

        int points1 =
                getFairPlayPoints(
                        team1,
                        yellowCards,
                        directReds,
                        secondYellowReds
                );

        int points2 =
                getFairPlayPoints(
                        team2,
                        yellowCards,
                        directReds,
                        secondYellowReds
                );

        if (points1 != points2) {

            return Integer.compare(
                    points1,
                    points2
            );
        }

        return team1.getName()
                .compareToIgnoreCase(
                        team2.getName()
                );
    }

    private int getFairPlayPoints(
            Team team,
            Map<Team, Integer> yellowCards,
            Map<Team, Integer> directReds,
            Map<Team, Integer> secondYellowReds) {

        return yellowCards
                .getOrDefault(
                        team,
                        0
                )
                + directReds
                .getOrDefault(
                        team,
                        0
                ) * 3
                + secondYellowReds
                .getOrDefault(
                        team,
                        0
                ) * 3;
    }

    private void appendTeamStatistics(
            StringBuilder report,
            Team team,
            List<Match> matches) {

        double averageAge =
                getAveragePlayerAge(
                        team
                );

        int goalsFor = 0;
        int goalsAgainst = 0;
        int matchesPlayed = 0;
        int pointsObtained = 0;

        for (Match match :
                matches) {

            if (!match.isPlayed()) {
                continue;
            }

            if (match.getHomeTeam()
                    == team) {

                matchesPlayed++;

                goalsFor +=
                        match.getHomeGoals();

                goalsAgainst +=
                        match.getAwayGoals();

                pointsObtained +=
                        getPoints(
                                match.getHomeGoals(),
                                match.getAwayGoals()
                        );

            } else if (match.getAwayTeam()
                    == team) {

                matchesPlayed++;

                goalsFor +=
                        match.getAwayGoals();

                goalsAgainst +=
                        match.getHomeGoals();

                pointsObtained +=
                        getPoints(
                                match.getAwayGoals(),
                                match.getHomeGoals()
                        );
            }
        }

        double effectiveness =
                0.0;

        if (matchesPlayed > 0) {

            effectiveness =
                    pointsObtained
                            * 100.0
                            / (matchesPlayed * 3);
        }

        report.append(
                String.format(
                        "%-27s | %-8.2f | %-8d | %-16s | %-4d | %-4d | %6.2f%%%n",
                        team.getName(),
                        averageAge,
                        team.getCoach()
                                .getAge(),
                        team.getCoach()
                                .getNationality()
                                .getName(),
                        goalsFor,
                        goalsAgainst,
                        effectiveness
                )
        );
    }

    private String buildRefereeReport(
            List<Referee> referees,
            Map<Referee, Integer> matchesByReferee) {

        StringBuilder report =
                new StringBuilder();

        report.append(
                "\n====================================\n"
        );

        report.append(
                "REPORT VII - REFEREES\n"
        );

        report.append(
                "====================================\n"
        );

        report.append(
                String.format(
                        "%-4s | %-28s | %-18s | %-8s | %-10s%n",
                        "POS",
                        "REFEREE",
                        "NATIONALITY",
                        "MATCHES",
                        "YEARS"
                )
        );

        double totalYears = 0;

        for (int i = 0;
             i < referees.size();
             i++) {

            Referee referee =
                    referees.get(i);

            totalYears +=
                    referee.getYearsOfficiated();

            report.append(
                    String.format(
                            "%-4d | %-28s | %-18s | %-8d | %-10d%n",
                            i + 1,
                            referee.getName(),
                            referee.getNationality()
                                    .getName(),
                            matchesByReferee
                                    .getOrDefault(
                                            referee,
                                            0
                                    ),
                            referee.getYearsOfficiated()
                    )
            );
        }

        double averageYears =
                referees.isEmpty()
                        ? 0
                        : totalYears
                        / referees.size();

        report.append(
                String.format(
                        "%nAverage years officiating: %.2f%n",
                        averageYears
                )
        );

        return report.toString();
    }

    private void appendPlayer(
            StringBuilder report,
            Player player,
            Team team) {

        report.append(
                "\n------------------------------------\n"
        );

        report.append(
                "Name: "
                        + player.getName()
                        + "\n"
        );

        report.append(
                "Team: "
                        + (team == null
                        ? "-"
                        : team.getName())
                        + "\n"
        );

        report.append(
                "Position: "
                        + player.getPosition()
                        + "\n"
        );

        report.append(
                "Document: "
                        + player.getIdType()
                        + " "
                        + player.getIdNumber()
                        + "\n"
        );

        report.append(
                "Birth date: "
                        + player.getBirthDate()
                        + "\n"
        );

        report.append(
                "Age: "
                        + player.getAge()
                        + "\n"
        );

        report.append(
                "Nationality: "
                        + player.getNationality()
                        .getName()
                        + "\n"
        );

        report.append(
                String.format(
                        "Overall: %.2f%n",
                        player.getOverall()
                )
        );

        report.append(
                "Matches played: "
                        + player.getMatchesPlayed()
                        + "\n"
        );

        report.append(
                "Minutes played: "
                        + player.getMinutesPlayed()
                        + "\n"
        );

        report.append(
                "Goals: "
                        + player.getGoals()
                        + "\n"
        );

        report.append(
                "Penalty goals: "
                        + player.getPenaltyGoals()
                        + "\n"
        );

        report.append(
                "Assists: "
                        + player.getAssists()
                        + "\n"
        );

        report.append(
                "Yellow cards: "
                        + player.getYellowCards()
                        + "\n"
        );

        report.append(
                "Red cards: "
                        + player.getRedCards()
                        + "\n"
        );

        if (player
                instanceof Goalkeeper) {

            appendGoalkeeperStatistics(
                    report,
                    (Goalkeeper) player
            );
        }
    }

    private void appendGoalkeeperStatistics(
            StringBuilder report,
            Goalkeeper goalkeeper) {

        double averageGoalsConceded =
                0.0;

        if (goalkeeper.getMatchesPlayed()
                > 0) {

            averageGoalsConceded =
                    goalkeeper.getGoalsConceded()
                            * 1.0
                            / goalkeeper
                            .getMatchesPlayed();
        }

        report.append(
                "Goals conceded: "
                        + goalkeeper
                        .getGoalsConceded()
                        + "\n"
        );

        report.append(
                String.format(
                        "Goals conceded per match: %.2f%n",
                        averageGoalsConceded
                )
        );
    }

    private double getAveragePlayerAge(
            Team team) {

        if (team.getSquad()
                .isEmpty()) {

            return 0.0;
        }

        int totalAge = 0;

        for (Player player :
                team.getSquad()) {

            totalAge +=
                    player.getAge();
        }

        return totalAge
                * 1.0
                / team.getSquad()
                .size();
    }

    private int getPoints(
            int goalsFor,
            int goalsAgainst) {

        if (goalsFor > goalsAgainst) {
            return 3;
        }

        if (goalsFor == goalsAgainst) {
            return 1;
        }

        return 0;
    }

    private List<Player> getAllPlayers(
            List<Team> teams) {

        List<Player> players =
                new ArrayList<>();

        for (Team team :
                teams) {

            players.addAll(
                    team.getSquad()
            );
        }

        return players;
    }

    private Team findPlayerTeam(
            List<Team> teams,
            Player player) {

        for (Team team :
                teams) {

            if (team.getSquad()
                    .contains(
                            player
                    )) {

                return team;
            }
        }

        return null;
    }
}