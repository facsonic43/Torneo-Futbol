package model.match;

import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;

public class GroupMatch extends Match {

    public GroupMatch(Team homeTeam, Team awayTeam, Referee referee, Stadium stadium, LocalDate matchDate) {
        super(homeTeam, awayTeam, referee, stadium, matchDate);
    }

    @Override
    public boolean isKnockout() {
        return false;
    }

    @Override
    public boolean requiresTieBreak() {
        return false;
    }

    @Override
    public Team getWinner() {
        Team winner = null;
        if (played) {
            if (homeGoals > awayGoals) {
                winner = homeTeam;
            } else if (awayGoals > homeGoals) {
                winner = awayTeam;
            }
        }
        return winner;
    }

    @Override
    public String getResolutionCriteria() {
        String criteria = "Not Played";
        if (played) {
            if (homeGoals == awayGoals) {
                criteria = "Draw in Regular Time";
            } else {
                criteria = "Regular Time";
            }
        }
        return criteria;
    }
}
