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
        return isTied() && homePenalties == null && awayPenalties == null;
    }

    @Override
    public Team getWinner() {
        if (homeGoals > awayGoals) {
            return homeTeam;
        }

        if (awayGoals > homeGoals) {
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

        if (!isTied()) {
            return "REGULAR_TIME";
        }

        return "UNRESOLVED";
    }
}