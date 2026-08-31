package model.tournament;

import model.match.GroupMatch;
import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Group {
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
        this.matches.add(match);
    }


    //tabla de posiciones
    public List<Standing> getStandings() {
        Map<Team, Standing> table = new HashMap<>();

        for (Team t : teams) {
            table.put(t, new Standing(t));
        }

        for (GroupMatch match : matches) {
            if (match.isPlayed()) {
                table.get(match.getHomeTeam()).update(match.getHomeGoals(), match.getAwayGoals());
                table.get(match.getAwayTeam()).update(match.getAwayGoals(), match.getHomeGoals());
            }
        }

        List<Standing> standingsList = new ArrayList<>(table.values());
        Collections.sort(standingsList);
        return standingsList;
    }

    public List<Team> getQualifiedTeams() {
        List<Standing> standings = getStandings();
        List<Team> qualified = new ArrayList<>();
        if (standings.size() >= 2) {
            qualified.add(standings.get(0).getTeam());
            qualified.add(standings.get(1).getTeam());
        }
        return qualified;
    }

}