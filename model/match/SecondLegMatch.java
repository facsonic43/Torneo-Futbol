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
    public boolean requiresTieBreak() {
        return getGlobalHomeGoals() == getGlobalAwayGoals()
                && homePenalties == null
                && awayPenalties == null;
    }

    @Override
    public Team getWinner() {
        if (getGlobalHomeGoals() > getGlobalAwayGoals()) {
            return homeTeam;
        }

        if (getGlobalAwayGoals() > getGlobalHomeGoals()) {
            return awayTeam;
        }

        if (homePenalties != null && awayPenalties != null) {
            if (homePenalties > awayPenalties) {
                return homeTeam;
            }

            if (awayPenalties > homePenalties) {
                return awayTeam;
            }
        }

        return null;
    }

    @Override
    public String getResolutionCriteria() {
        if (!isPlayed()) {
            return "NOT_PLAYED";
        }

        if (homePenalties != null && awayPenalties != null) {
            return "PENALTIES";
        }

        if (getGlobalHomeGoals() != getGlobalAwayGoals()) {
            return "AGGREGATE_SCORE";
        }

        return "UNRESOLVED";
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