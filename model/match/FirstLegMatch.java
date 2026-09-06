package model.match;

import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;

public class FirstLegMatch extends Match {

    public FirstLegMatch(Team homeTeam, Team awayTeam, Referee referee, Stadium stadium, LocalDate matchDate) {
        super(homeTeam, awayTeam, referee, stadium, matchDate);
    }

    @Override
    public boolean isKnockout() {
        return true;
    }

    @Override
    public boolean requiresTieBreak() {
        return false;
    }

    @Override
    public Team getWinner() {
        if (!isPlayed() || isTied()) {
            return null;
        }

        if (homeGoals > awayGoals) {
            return homeTeam;
        }

        return awayTeam;
    }

    @Override
    public String getResolutionCriteria() {
        if (!isPlayed()) {
            return "NOT_PLAYED";
        }

        if (isTied()) {
            return "DRAW";
        }

        return "REGULAR_TIME";
    }
}