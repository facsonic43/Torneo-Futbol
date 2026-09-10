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

    public int getHomeTeamPoints() {
        int points = 0;
        if (firstLeg != null && firstLeg.isPlayed()) {
            if (firstLeg.getAwayGoals() > firstLeg.getHomeGoals()) {
                points += 3;
            } else if (firstLeg.getAwayGoals() == firstLeg.getHomeGoals()) {
                points += 1;
            }
        }
        if (played) {
            if (homeGoals > awayGoals) {
                points += 3;
            } else if (homeGoals == awayGoals) {
                points += 1;
            }
        }
        return points;
    }

    public int getAwayTeamPoints() {
        int points = 0;
        if (firstLeg != null && firstLeg.isPlayed()) {
            if (firstLeg.getHomeGoals() > firstLeg.getAwayGoals()) {
                points += 3;
            } else if (firstLeg.getHomeGoals() == firstLeg.getAwayGoals()) {
                points += 1;
            }
        }
        if (played) {
            if (awayGoals > homeGoals) {
                points += 3;
            } else if (awayGoals == homeGoals) {
                points += 1;
            }
        }
        return points;
    }

    public int getHomeTeamWeightedGoals() {
        int firstLegAwayGoals = (firstLeg != null) ? firstLeg.getAwayGoals() : 0;
        return (firstLegAwayGoals * 2) + this.homeGoals;
    }

    public int getAwayTeamWeightedGoals() {
        int firstLegHomeGoals = (firstLeg != null) ? firstLeg.getHomeGoals() : 0;
        return firstLegHomeGoals + (this.awayGoals * 2);
    }

    @Override
    public boolean requiresTieBreak() {
        boolean tieBreakNeeded = false;
        if (played && firstLeg != null && firstLeg.isPlayed()) {
            int homePts = getHomeTeamPoints();
            int awayPts = getAwayTeamPoints();
            if (homePts == awayPts) {
                int homeWeighted = getHomeTeamWeightedGoals();
                int awayWeighted = getAwayTeamWeightedGoals();
                if (homeWeighted == awayWeighted) {
                    if (homePenalties == null || awayPenalties == null || homePenalties.equals(awayPenalties)) {
                        tieBreakNeeded = true;
                    }
                }
            }
        }
        return tieBreakNeeded;
    }

    @Override
    public Team getWinner() {
        Team winner = null;
        if (played && firstLeg != null && firstLeg.isPlayed()) {
            int homePts = getHomeTeamPoints();
            int awayPts = getAwayTeamPoints();

            if (homePts > awayPts) {
                winner = homeTeam;
            } else if (awayPts > homePts) {
                winner = awayTeam;
            } else {
                int homeWeighted = getHomeTeamWeightedGoals();
                int awayWeighted = getAwayTeamWeightedGoals();

                if (homeWeighted > awayWeighted) {
                    winner = homeTeam;
                } else if (awayWeighted > homeWeighted) {
                    winner = awayTeam;
                } else if (homePenalties != null && awayPenalties != null) {
                    if (homePenalties > awayPenalties) {
                        winner = homeTeam;
                    } else if (awayPenalties > homePenalties) {
                        winner = awayTeam;
                    }
                }
            }
        }
        return winner;
    }

    @Override
    public String getResolutionCriteria() {
        String criteria = "Not Played";
        if (played && firstLeg != null && firstLeg.isPlayed()) {
            int homePts = getHomeTeamPoints();
            int awayPts = getAwayTeamPoints();

            if (homePts != awayPts) {
                criteria = "Points Aggregate (" + homePts + " vs " + awayPts + ")";
            } else {
                int homeWeighted = getHomeTeamWeightedGoals();
                int awayWeighted = getAwayTeamWeightedGoals();

                if (homeWeighted != awayWeighted) {
                    criteria = "Away Goals Rule (" + homeWeighted + " vs " + awayWeighted + " weighted)";
                } else if (homePenalties != null && awayPenalties != null) {
                    criteria = "Penalty Shootout (" + homePenalties + " - " + awayPenalties + ")";
                } else {
                    criteria = "Series Tied - Pending Penalties";
                }
            }
        }
        return criteria;
    }
}
