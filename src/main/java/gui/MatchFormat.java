package gui;

import javafx.scene.control.*;
import model.match.Match;
import model.match.ResolutionCriterion;
import model.match.FirstLegMatch;
import model.match.SecondLegMatch;

// Centraliza los textos de resultados, fechas, sedes, árbitros y contexto de penales.
// Comparte el mismo formato entre las pantallas de partidos y los reportes.

public final class MatchFormat {
    private MatchFormat() {
    }

    public static String scoreText(Match match) {
        if (!match.isPlayed()) {
            return "Pending";
        }
        String result = match.getHomeGoals() + " - " + match.getAwayGoals();
        if (match.getResolutionCriterion() == ResolutionCriterion.PENALTIES) {
            result += " (" + match.getHomePenalties() + " - " + match.getAwayPenalties() + " pens)";
        }
        return result;
    }

    static String matchDate(Match match) {
        return match.getMatchDate() == null ? "TBD" : String.valueOf(match.getMatchDate());
    }

    static String matchStadium(Match match) {
        return match.getStadium() == null ? "TBD" : match.getStadium().getName();
    }

    static String matchReferee(Match match) {
        return match.getReferee() == null ? "TBD" : match.getReferee().getName();
    }

    public static String penaltyContext(Match match) {
        SecondLegMatch secondLeg = match.asSecondLeg().orElse(null);
        if (secondLeg != null) {
            FirstLegMatch firstLeg = secondLeg.getFirstLeg();
            return "First leg: " + firstLeg.getHomeTeam().getName() + " " + firstLeg.getHomeGoals() + " - "
                    + firstLeg.getAwayGoals() + " " + firstLeg.getAwayTeam().getName() + "   ·   Second leg: "
                    + secondLeg.getHomeTeam().getName() + " " + secondLeg.getHomeGoals() + " - "
                    + secondLeg.getAwayGoals() + " " + secondLeg.getAwayTeam().getName()
                    + "   ·   Series remains tied after tournament criteria.";
        }
        return match.getHomeTeam().getName() + " " + match.getHomeGoals() + " - " + match.getAwayGoals() + " "
                + match.getAwayTeam().getName() + "   ·   The final is level after 90 minutes.";
    }
}
