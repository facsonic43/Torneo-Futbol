package model.match;

import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;

public class FinalMatch extends Match {

    public FinalMatch(Team homeTeam, Team awayTeam, Referee referee, Stadium stadium, LocalDate date) {
        super(homeTeam, awayTeam, referee, stadium, date);
    }

    @Override
    public boolean requiresTieBreak() {
        // En una Final a partido único, si hay empate en los 90', requiere alargue / penales
        return this.homeGoals == this.awayGoals;
    }

    @Override
    public Team getWinner() {
        Team winner = null;
        if (this.played) {
            if (this.homeGoals > this.awayGoals) {
                winner = this.homeTeam;
            } else if (this.awayGoals > this.homeGoals) {
                winner = this.awayTeam;
            } else if (this.homePenalties != null && this.awayPenalties != null) {
                if (this.homePenalties > this.awayPenalties) {
                    winner = this.homeTeam;
                } else if (this.awayPenalties > this.homePenalties) {
                    winner = this.awayTeam;
                }
            }
        }
        return winner;
    }

    @Override
    public String getResolutionCriteria() {
        String criteria = "Match not played yet";
        if (this.played) {
            if (this.homePenalties != null && this.awayPenalties != null) {
                criteria = "Decided by Penalty Shootout (" + this.homePenalties + " - " + this.awayPenalties + ")";
            } else if (this.extraTimePlayed) {
                criteria = "Decided in Extra Time (" + this.homeGoals + " - " + this.awayGoals + ")";
            } else {
                criteria = "Decided in Regular Time (" + this.homeGoals + " - " + this.awayGoals + ")";
            }
        }
        return criteria;
    }
}
