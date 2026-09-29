package model.tournament;

import model.match.GroupMatch;
import model.participant.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

/*
 * Representa uno de los cuatro grupos del campeonato.
 * Guarda sus equipos y partidos y construye la tabla aplicando puntos, diferencia de gol,
 * goles a favor y, si todavía existe empate, el resultado directo entre los equipos.
 */
public class Group implements Serializable{
    private String name;
    private List<Team> teams = new ArrayList<>();
    private List<GroupMatch> matches = new ArrayList<>();

    public Group(String name, List<Team> teams) {
        this.name = name;
        this.teams = teams;
    }

    public String getName() {
        return name;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public List<GroupMatch> getMatches() {
        return matches;
    }

    public void addMatch(GroupMatch match) {
        matches.add(match);
    }

    public List<Standing> getStandings() {

        Map<Team, Standing> table =
                new HashMap<>();

        for (Team team : teams) {

            table.put(
                    team,
                    new Standing(team)
            );
        }

        for (GroupMatch match : matches) {

            if (match.isPlayed()) {

                table.get(
                        match.getHomeTeam()
                ).update(
                        match.getHomeGoals(),
                        match.getAwayGoals()
                );

                table.get(
                        match.getAwayTeam()
                ).update(
                        match.getAwayGoals(),
                        match.getHomeGoals()
                );
            }
        }

        List<Standing> standings =
                new ArrayList<>(
                        table.values()
                );

        standings.sort(
                this::compareStandings
        );

        return standings;
    }

    // Aplica los cuatro criterios de desempate pedidos para la fase de grupos.
    private int compareStandings(
            Standing standing1,
            Standing standing2) {

        if (standing1.getPoints()
                != standing2.getPoints()) {

            return Integer.compare(
                    standing2.getPoints(),
                    standing1.getPoints()
            );
        }

        if (standing1.getGoalDifference()
                != standing2.getGoalDifference()) {

            return Integer.compare(
                    standing2.getGoalDifference(),
                    standing1.getGoalDifference()
            );
        }

        if (standing1.getScored()
                != standing2.getScored()) {

            return Integer.compare(
                    standing2.getScored(),
                    standing1.getScored()
            );
        }

        int directResult =
                compareHeadToHead(
                        standing1.getTeam(),
                        standing2.getTeam()
                );

        if (directResult != 0) {
            return directResult;
        }

        return standing1
                .getTeam()
                .getName()
                .compareToIgnoreCase(
                        standing2
                                .getTeam()
                                .getName()
                );
    }

    // Busca el partido entre los dos equipos para usarlo como último desempate.
    private int compareHeadToHead(
            Team team1,
            Team team2) {

        for (GroupMatch match : matches) {

            if (!match.isPlayed()) {
                continue;
            }

            boolean normalOrder =
                    match.getHomeTeam() == team1
                            && match.getAwayTeam() == team2;

            boolean reverseOrder =
                    match.getHomeTeam() == team2
                            && match.getAwayTeam() == team1;

            if (normalOrder) {

                if (match.getHomeGoals()
                        > match.getAwayGoals()) {

                    return -1;
                }

                if (match.getAwayGoals()
                        > match.getHomeGoals()) {

                    return 1;
                }

                return 0;
            }

            if (reverseOrder) {

                if (match.getAwayGoals()
                        > match.getHomeGoals()) {

                    return -1;
                }

                if (match.getHomeGoals()
                        > match.getAwayGoals()) {

                    return 1;
                }

                return 0;
            }
        }

        return 0;
    }

    public List<Team> getQualifiedTeams() {

        List<Standing> standings =
                getStandings();

        List<Team> qualified =
                new ArrayList<>();

        if (standings.size() >= 2) {

            qualified.add(
                    standings.get(0).getTeam()
            );

            qualified.add(
                    standings.get(1).getTeam()
            );
        }

        return qualified;
    }
}