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
        return minute + "' Penalty [" + team.getName() + "]: " + player.getName() + " -> " + (scored ? "SCORED" : "MISSED");
    }

    @Override
    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean belongsToShootout() {
        return true;
    }
}
