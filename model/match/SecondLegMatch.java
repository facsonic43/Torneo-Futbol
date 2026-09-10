package model.match;

import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;

public class SecondLegMatch extends Match {
    private FirstLegMatch firstLeg;

    public SecondLegMatch(Team homeTeam, Team awayTeam, Referee referee, Stadium stadium, LocalDate date, FirstLegMatch firstLeg) {
        super(homeTeam, awayTeam, referee, stadium, date);
        this.firstLeg = firstLeg;
    }

    public FirstLegMatch getFirstLeg() {
        return firstLeg;
    }

    // Total aggregate goals scored by homeTeam (Second Leg Home) across both legs
    // Leg 1: played as Away -> firstLeg.getAwayGoals()
    // Leg 2: played as Home -> this.homeGoals
    public int getHomeTeamAggregateGoals() {
        return firstLeg.getAwayGoals() + this.homeGoals;
    }

    // Total aggregate goals scored by awayTeam (Second Leg Away) across both legs
    // Leg 1: played as Home -> firstLeg.getHomeGoals()
    // Leg 2: played as Away -> this.awayGoals
    public int getAwayTeamAggregateGoals() {
        return firstLeg.getHomeGoals() + this.awayGoals;
    }

    // Away goals scored by homeTeam across the tie (scored in First Leg)
    public int getHomeTeamAwayGoals() {
        return firstLeg.getAwayGoals();
    }

    // Away goals scored by awayTeam across the tie (scored in Second Leg)
    public int getAwayTeamAwayGoals() {
        return this.awayGoals;
    }

    @Override
    public boolean requiresTieBreak() {
        // If aggregate goals are tied AND away goals are tied -> Penalty shootout
        return getHomeTeamAggregateGoals() == getAwayTeamAggregateGoals()
                && getHomeTeamAwayGoals() == getAwayTeamAwayGoals();
    }

    @Override
    public Team getWinner() {
        Team winner = null;
        if (this.played) {
            int homeAgg = getHomeTeamAggregateGoals();
            int awayAgg = getAwayTeamAggregateGoals();

            if (homeAgg > awayAgg) {
                winner = this.homeTeam;
            } else if (awayAgg > homeAgg) {
                winner = this.awayTeam;
            } else {
                // Tied on aggregate goals -> Tie-break by Away Goals
                int homeAwayGoals = getHomeTeamAwayGoals();
                int awayAwayGoals = getAwayTeamAwayGoals();

                if (homeAwayGoals > awayAwayGoals) {
                    winner = this.homeTeam;
                } else if (awayAwayGoals > homeAwayGoals) {
                    winner = this.awayTeam;
                } else if (this.homePenalties != null && this.awayPenalties != null) {
                    // Tied on away goals -> Penalty shootout
                    if (this.homePenalties > this.awayPenalties) {
                        winner = this.homeTeam;
                    } else if (this.awayPenalties > this.homePenalties) {
                        winner = this.awayTeam;
                    }
                }
            }
        }
        return winner;
    }

    @Override
    public String getResolutionCriteria() {
        String criteria = "Series not played yet";
        if (this.played) {
            int homeAgg = getHomeTeamAggregateGoals();
            int awayAgg = getAwayTeamAggregateGoals();

            if (homeAgg != awayAgg) {
                criteria = "Decided on Aggregate Score (" + homeAgg + " - " + awayAgg + ")";
            } else {
                int homeAwayGoals = getHomeTeamAwayGoals();
                int awayAwayGoals = getAwayTeamAwayGoals();

                if (homeAwayGoals != awayAwayGoals) {
                    criteria = "Decided by Away Goals Rule (Away Goals: " + homeAwayGoals + " vs " + awayAwayGoals + ", Aggregate: " + homeAgg + "-" + awayAgg + ")";
                } else if (this.homePenalties != null && this.awayPenalties != null) {
                    criteria = "Decided by Penalty Shootout (" + this.homePenalties + " - " + this.awayPenalties + ")";
                } else {
                    criteria = "Tied series";
                }
            }
        }
        return criteria;
    }
}
