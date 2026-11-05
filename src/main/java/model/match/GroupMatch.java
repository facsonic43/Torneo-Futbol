package model.match;

import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;

// Partido de la fase de grupos: reparte puntos por el marcador y admite empates sin desempate.

public class GroupMatch extends Match {
    private static final long serialVersionUID = 90508628634392140L;

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

}
