package model.match;

import model.participant.Player;
import model.participant.Team;

// Representa una expulsión e indica si corresponde a roja directa o doble amarilla.
// Permite describirla y procesarla como incidencia del partido.

public class RedCard extends Event {
    private boolean directRed;

    public RedCard(int minute, Team team, Player player, boolean directRed) {
        super(minute, team, player);
        this.directRed = directRed;
    }

    public boolean isDirectRed() {
        return directRed;
    }

    @Override
    public String getDescription() {
        String motive = directRed ? "Straight Red card" : "Double Yellow Card / Red Card";
        return minute + "' Red Card (" + motive + ") - " + player.getName() + " [" + team.getName() + "]";
    }

    @Override
    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }
}
