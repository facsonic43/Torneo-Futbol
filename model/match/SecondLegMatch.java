package model.match;

import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;

public class SecondLegMatch extends Match {
    private FirstLegMatch firstLeg;

    public SecondLegMatch(Team homeTeam, Team awayTeam, Referee referee, Stadium stadium, LocalDate matchDate, FirstLegMatch firstLeg) {
        super(homeTeam, awayTeam, referee, stadium, matchDate);
        this.firstLeg = firstLeg;
    }

    public FirstLegMatch getFirstLeg() {
        return firstLeg;
    }

    @Override
    public boolean isKnockout() {
        return true;
    }

    @Override
    public boolean isTied() {
        return false;
    }

    public int getGlobalHomeGoals() {
        int awayGoalsInFirstLeg = firstLeg.getAwayGoals();
        return this.homeGoals + awayGoalsInFirstLeg;
    }

    public int getGlobalAwayGoals() {
        int homeGoalsInFirstLeg = firstLeg.getHomeGoals();
        return this.awayGoals + homeGoalsInFirstLeg;
    }
}