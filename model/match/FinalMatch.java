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
    public boolean isTied() {
        return false;
    }


}