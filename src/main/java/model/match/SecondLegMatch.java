package model.match;

import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;

/*
 * Representa el partido de vuelta de una serie eliminatoria.
 * Primero compara los puntos obtenidos en los dos partidos y, si existe igualdad,
 * utiliza la diferencia de gol ponderando doble los goles convertidos como visitante.
 * Si la igualdad continúa, la serie se define mediante penales.
 */
public class SecondLegMatch extends Match {
    private FirstLegMatch firstLeg;

    public SecondLegMatch(
            Team homeTeam,
            Team awayTeam,
            Referee referee,
            Stadium stadium,
            LocalDate matchDate,
            FirstLegMatch firstLeg) {

        super(
                homeTeam,
                awayTeam,
                referee,
                stadium,
                matchDate
        );

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
        return getSeriesPoints(homeTeam)
                == getSeriesPoints(awayTeam)

                && getWeightedGoalDifference(
                homeTeam
        ) == 0

                && homePenalties == null
                && awayPenalties == null;
    }

    @Override
    public Team getWinner() {
        if (!isPlayed()) {
            return null;
        }

        int homePoints =
                getSeriesPoints(
                        homeTeam
                );

        int awayPoints =
                getSeriesPoints(
                        awayTeam
                );

        if (homePoints > awayPoints) {
            return homeTeam;
        }

        if (awayPoints > homePoints) {
            return awayTeam;
        }

        int weightedDifference =
                getWeightedGoalDifference(
                        homeTeam
                );

        if (weightedDifference > 0) {
            return homeTeam;
        }

        if (weightedDifference < 0) {
            return awayTeam;
        }

        if (homePenalties != null
                && awayPenalties != null) {

            if (homePenalties
                    > awayPenalties) {

                return homeTeam;
            }

            if (awayPenalties
                    > homePenalties) {

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

        if (homePenalties != null
                && awayPenalties != null) {

            return "PENALTIES";
        }

        if (getSeriesPoints(homeTeam)
                != getSeriesPoints(awayTeam)) {

            return "SERIES_POINTS";
        }

        if (getWeightedGoalDifference(
                homeTeam
        ) != 0) {

            return "WEIGHTED_GOAL_DIFFERENCE";
        }

        return "UNRESOLVED";
    }

    // Calcula los puntos obtenidos por un equipo entre la ida y la vuelta.
    public int getSeriesPoints(
            Team team) {

        return getMatchPoints(
                firstLeg,
                team
        ) + getMatchPoints(
                this,
                team
        );
    }

    // Calcula los goles ponderados, contando doble los goles de visitante.
    public int getWeightedGoals(
            Team team) {

        int weightedGoals = 0;

        if (firstLeg.getHomeTeam()
                == team) {

            weightedGoals +=
                    firstLeg.getHomeGoals();

        } else if (firstLeg.getAwayTeam()
                == team) {

            weightedGoals +=
                    firstLeg.getAwayGoals()
                            * 2;
        }

        if (homeTeam == team) {

            weightedGoals +=
                    homeGoals;

        } else if (awayTeam == team) {

            weightedGoals +=
                    awayGoals * 2;
        }

        return weightedGoals;
    }

    // Obtiene la diferencia entre los goles ponderados propios y los del rival.
    public int getWeightedGoalDifference(
            Team team) {

        Team opponent;

        if (team == homeTeam) {

            opponent =
                    awayTeam;

        } else if (team == awayTeam) {

            opponent =
                    homeTeam;

        } else {

            return 0;
        }

        return getWeightedGoals(team)
                - getWeightedGoals(opponent);
    }

    public int getGlobalGoals(
            Team team) {

        int goals = 0;

        if (firstLeg.getHomeTeam()
                == team) {

            goals +=
                    firstLeg.getHomeGoals();

        } else if (firstLeg.getAwayTeam()
                == team) {

            goals +=
                    firstLeg.getAwayGoals();
        }

        if (homeTeam == team) {

            goals +=
                    homeGoals;

        } else if (awayTeam == team) {

            goals +=
                    awayGoals;
        }

        return goals;
    }

    private int getMatchPoints(
            Match match,
            Team team) {

        int teamGoals;
        int opponentGoals;

        if (match.getHomeTeam()
                == team) {

            teamGoals =
                    match.getHomeGoals();

            opponentGoals =
                    match.getAwayGoals();

        } else if (match.getAwayTeam()
                == team) {

            teamGoals =
                    match.getAwayGoals();

            opponentGoals =
                    match.getHomeGoals();

        } else {

            return 0;
        }

        if (teamGoals > opponentGoals) {
            return 3;
        }

        if (teamGoals == opponentGoals) {
            return 1;
        }

        return 0;
    }
}