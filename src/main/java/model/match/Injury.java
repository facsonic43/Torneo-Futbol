package model.match;

import model.participant.Player;
import model.participant.Team;

// Representa una lesión ocurrida durante el partido y permite describirla
// y procesarla como incidencia.

public class Injury extends Event {
    private static final long serialVersionUID = 6087856104520587795L;
    private int matchesOut;

    public Injury(int minute, Team team, Player player, int matchesOut) {
        super(minute, team, player);
        this.matchesOut = matchesOut;
    }

    public int getMatchesOut() {
        return matchesOut;
    }

    @Override
    public String getDescription() {
        return getMinute() + "' Injury - " + getPlayer().getName() + " [" + getTeam().getName() + "] (Out for " + matchesOut + " match/es)";
    }

    @Override
    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }
}
