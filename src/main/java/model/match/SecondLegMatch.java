package model.match;

import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Resuelve una serie una vez jugadas la ida y la vuelta, incluidos los resultados administrativos.
 * Compara primero los puntos y luego los goles ponderados, contando doble los de visitante.
 * Sólo una igualdad en ambos criterios requiere penales; una tanda incompleta no define ganador.
 * Si la vuelta se pierde sin jugar y la serie sigue empatada, queda pendiente de resolución.
 */
public class SecondLegMatch extends Match {
    private static final long serialVersionUID = 3746140207111491617L;
    private FirstLegMatch firstLeg;

    public SecondLegMatch(Team homeTeam, Team awayTeam, Referee referee, Stadium stadium,
                          LocalDate matchDate, FirstLegMatch firstLeg) {
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

    @Override
    public boolean requiresTieBreak() {
        return !isForfeited() && getResolutionCriterion() == ResolutionCriterion.UNRESOLVED;
    }

    @Override
    public Team getWinner() {
        return switch (getResolutionCriterion()) {
            case SERIES_POINTS -> getSeriesPoints(homeTeam) > getSeriesPoints(awayTeam) ? homeTeam : awayTeam;
            case WEIGHTED_GOAL_DIFFERENCE -> getWeightedGoalDifference(homeTeam) > 0 ? homeTeam : awayTeam;
            case PENALTIES -> getPenaltyWinner();
            default -> null;
        };
    }

    @Override
    public ResolutionCriterion getResolutionCriterion() {
        if (!played || !firstLeg.isPlayed()) {
            return ResolutionCriterion.NOT_PLAYED;
        }
        if (getSeriesPoints(homeTeam) != getSeriesPoints(awayTeam)) {
            return ResolutionCriterion.SERIES_POINTS;
        }
        if (getWeightedGoalDifference(homeTeam) != 0) {
            return ResolutionCriterion.WEIGHTED_GOAL_DIFFERENCE;
        }
        return getPenaltyWinner() == null ? ResolutionCriterion.UNRESOLVED : ResolutionCriterion.PENALTIES;
    }

    public int getSeriesPoints(Team team) {
        return getMatchPoints(firstLeg, team) + getMatchPoints(this, team);
    }

    public int getWeightedGoals(Team team) {
        int weightedGoals = 0;
        if (firstLeg.isPlayed()) {
            if (firstLeg.getHomeTeam() == team) {
                weightedGoals += firstLeg.getHomeGoals();
            } else if (firstLeg.getAwayTeam() == team) {
                weightedGoals += firstLeg.getAwayGoals() * 2;
            }
        }
        if (played) {
            if (homeTeam == team) {
                weightedGoals += homeGoals;
            } else if (awayTeam == team) {
                weightedGoals += awayGoals * 2;
            }
        }
        return weightedGoals;
    }

    public int getWeightedGoalDifference(Team team) {
        if (team == homeTeam) {
            return getWeightedGoals(homeTeam) - getWeightedGoals(awayTeam);
        }
        if (team == awayTeam) {
            return getWeightedGoals(awayTeam) - getWeightedGoals(homeTeam);
        }
        return 0;
    }

    private int getMatchPoints(Match match, Team team) {
        if (!match.isPlayed() || (team != match.getHomeTeam() && team != match.getAwayTeam())) {
            return 0;
        }
        int teamGoals = team == match.getHomeTeam() ? match.getHomeGoals() : match.getAwayGoals();
        int opponentGoals = team == match.getHomeTeam() ? match.getAwayGoals() : match.getHomeGoals();
        return teamGoals > opponentGoals ? 3 : teamGoals == opponentGoals ? 1 : 0;
    }

    @Override
    public Optional<SecondLegMatch> asSecondLeg() {
        return Optional.of(this);
    }
}
