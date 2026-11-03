package model.match;

import model.participant.Player;
import model.participant.Team;

// Representa un cambio de futbolistas y conserva al jugador que sale y al que ingresa
// para describir el cambio y reconstruir las participaciones.

public class Substitution extends Event {
    private Player playerIn;

    public Substitution(int minute, Team team, Player playerOut, Player playerIn) {
        super(minute, team, playerOut);
        this.playerIn = playerIn;
    }

    public Player getPlayerOut() {
        return getPlayer();
    }

    public Player getPlayerIn() {
        return playerIn;
    }

    @Override
    public String getDescription() {
        return getMinute() + "' Substitution [" + getTeam().getName() + "]: " + playerIn.getName() + " replaces " + getPlayer().getName();
    }

    @Override
    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }
}
