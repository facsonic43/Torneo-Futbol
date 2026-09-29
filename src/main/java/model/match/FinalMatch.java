package model.match;

import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;

public class FinalMatch extends Match {

    public FinalMatch(Team homeTeam, Team awayTeam, Referee referee, Stadium stadium, LocalDate matchDate) {
        super(homeTeam, awayTeam, referee, stadium, matchDate);
    }

    @Override
    public boolean isKnockout() {
        return true;
    }

    @Override
    public boolean requiresTieBreak() {
        boolean tieBreakNeeded = false;
        if (played && homeGoals == awayGoals) {
            if (homePenalties == null || awayPenalties == null || homePenalties.equals(awayPenalties)) {
                tieBreakNeeded = true;
            }
        }
        return tieBreakNeeded;
    }

    @Override
    public Team getWinner() {
        Team winner = null;
        if (played) {
            if (homeGoals > awayGoals) {
                winner = homeTeam;
            } else if (awayGoals > homeGoals) {
                winner = awayTeam;
            } else if (homePenalties != null && awayPenalties != null) {
                if (homePenalties > awayPenalties) {
                    winner = homeTeam;
                } else if (awayPenalties > homePenalties) {
                    winner = awayTeam;
                }
            }
        }
        return winner;
    }

    @Override
    public String getResolutionCriteria() {
        String criteria = "Not Played";
        if (played) {
            if (homeGoals != awayGoals) {
                criteria = "Regular Time (" + homeGoals + " - " + awayGoals + ")";
            } else if (homePenalties != null && awayPenalties != null) {
                criteria = "Penalty Shootout (" + homePenalties + " - " + awayPenalties + ")";
            } else {
                criteria = "Tied - Pending Penalties";
            }
        }
        return criteria;
    }
}
