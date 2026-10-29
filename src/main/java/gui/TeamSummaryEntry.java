package gui;

import javafx.scene.control.*;
import model.participant.Team;
import model.tournament.Group;
import model.tournament.Standing;
import java.util.List;

public class TeamSummaryEntry {
    private final Team team;
    private final double squadAverageAge;
    private final int totalGoalsScored;
    private final int totalGoalsConceded;
    private final List<Group> tournamentGroups;

    public TeamSummaryEntry(Team team, double avgAge, int gf, int ga, List<Group> groups) {
        this.team = team;
        this.squadAverageAge = avgAge;
        this.totalGoalsScored = gf;
        this.totalGoalsConceded = ga;
        this.tournamentGroups = groups;
    }

    public String getTeamName() { return team.getName(); }
    public String getCountryName() { return team.getCountry() != null ? team.getCountry().getName() : "-"; }
    public int getRanking() { return team.getRanking(); }
    public double getOverall() { return team.getOverall(); }
    public String getCoachName() { return team.getCoach() != null ? team.getCoach().getName() : "-"; }

    public String getCoachAge() {
        return team.getCoach() != null ? String.valueOf(team.getCoach().getAge()) : "-";
    }

    public String getCoachNationality() {
        return (team.getCoach() != null && team.getCoach().getNationality() != null)
                ? team.getCoach().getNationality().getName()
                : "-";
    }

    public double getEffectiveness() {
        int points = 0;
        int matchesPlayed = 0;

        if (tournamentGroups != null) {
            for (Group group : tournamentGroups) {
                for (Standing s : group.getStandings()) {
                    if (s.getTeam().equals(team)) {
                        points = s.getPoints();
                        matchesPlayed = s.getPlayed();
                        break;
                    }
                }
            }
        }

        if (matchesPlayed == 0) return 0.0;
        return ((double) points / (matchesPlayed * 3)) * 100.0;
    }

    public double getSquadAverageAge() { return squadAverageAge; }
    public int getTotalGoalsScored() { return totalGoalsScored; }
    public int getTotalGoalsConceded() { return totalGoalsConceded; }
}
