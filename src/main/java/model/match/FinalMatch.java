package model.match;

import java.util.Optional;

import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;

/**
 * Final a partido único: decide por el marcador o, si hay empate, por una tanda de penales.
 */
public class FinalMatch extends Match {
    private static final long serialVersionUID = -5476613621063167277L;

    public FinalMatch(Team homeTeam, Team awayTeam, Referee referee, Stadium stadium, LocalDate matchDate) {
        super(homeTeam, awayTeam, referee, stadium, matchDate);
    }

    @Override
    public boolean isKnockout() {
        return true;
    }

    @Override
    public boolean requiresTieBreak() {
        return getResolutionCriterion() == ResolutionCriterion.UNRESOLVED;
    }

    @Override
    public Team getWinner() {
        return switch (getResolutionCriterion()) {
            case REGULAR_TIME, FORFEIT -> super.getWinner();
            case PENALTIES -> getPenaltyWinner();
            default -> null;
        };
    }

    @Override
    public ResolutionCriterion getResolutionCriterion() {
        ResolutionCriterion criterion = super.getResolutionCriterion();
        if (criterion != ResolutionCriterion.DRAW) {
            return criterion;
        }
        return getPenaltyWinner() == null ? ResolutionCriterion.UNRESOLVED : ResolutionCriterion.PENALTIES;
    }

    @Override
    public Optional<FinalMatch> asFinal() {
        return Optional.of(this);
    }
}
