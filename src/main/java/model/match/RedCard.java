package model.match;

import model.participant.Player;
import model.participant.Team;

public class RedCard extends Event {
    private boolean directRed; // true = roja directa, false = doble amarilla

    public RedCard(int minute, Team team, Player player, boolean directRed) {
        super(minute, team, player);
        this.directRed = directRed;
    }

    public boolean isDirectRed() {
        return directRed;
    }

    @Override
    public String getDescription() {
        String motive = directRed ? "Roja Directa" : "Doble Amarilla / Roja";
        return minute + "' Tarjeta Roja (" + motive + ") - " + player.getName() + " [" + team.getName() + "]";
    }
}