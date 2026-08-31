package model.match;

import model.participant.Player;
import model.participant.Team;

public class PenaltyTaken extends Event {
    private boolean scored;

    public PenaltyTaken(int minute, Team team, Player player, boolean scored) {
        super(minute, team, player);
        this.scored = scored;
    }

    public boolean isScored() {
        return scored;
    }

    @Override
    public String getDescription() {
        return minute + "' Penal [" + team.getName() + "]: " + player.getName() + " -> " + (scored ? "CONVERTIDO" : "FALLADO");
    }
}