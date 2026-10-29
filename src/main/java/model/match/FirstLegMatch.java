package model.match;

import java.util.Optional;
import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;

public class FirstLegMatch extends Match {

    public FirstLegMatch(Team homeTeam, Team awayTeam, Referee referee, Stadium stadium, LocalDate matchDate) {
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
    public String getResolutionCriteria() {
        String criteria = "Not Played";
        if (played) {
            if (homeGoals == awayGoals) {
                criteria = "First Leg Draw";
            } else {
                criteria = "First Leg Advantage";
            }
        }
        return criteria;
    }

    @Override
    public Optional<FirstLegMatch> asFirstLeg() {
        return Optional.of(this);
    }
}
