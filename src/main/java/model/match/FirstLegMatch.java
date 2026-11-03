package model.match;

import java.util.Optional;

import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;

/**
 * Partido de ida de una eliminatoria; aporta puntos y goles a la serie sin exigir un ganador.
 */
public class FirstLegMatch extends Match {
    private static final long serialVersionUID = 1690642534697006805L;

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
    public Optional<FirstLegMatch> asFirstLeg() {
        return Optional.of(this);
    }
}
