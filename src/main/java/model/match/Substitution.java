package model.match;

import model.participant.Player;
import model.participant.Team;

public class Substitution extends Event {
    private Player playerIn;

    public Substitution(int minute, Team team, Player playerOut, Player playerIn) {
        super(minute, team, playerOut);
        this.playerIn = playerIn;
    }

    public Player getPlayerOut() {
        return player;
    }

    public Player getPlayerIn() {
        return playerIn;
    }

    @Override
    public String getDescription() {
        return minute + "' Cambio [" + team.getName() + "]: " + playerIn.getName() + " entra por " + player.getName();
    }
}