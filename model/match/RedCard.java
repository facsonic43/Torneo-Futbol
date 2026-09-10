package model.match;

import model.participant.Player;
import model.participant.Team;

public class RedCard extends Event {
    private boolean directRed; // true = straight red, false = double yellow

    public RedCard(int minute, Team team, Player player, boolean directRed) {
        super(minute, team, player);
        this.directRed = directRed;
    }

    public boolean isDirectRed() {
        return directRed;
    }

    @Override
    public String getDescription() {
        String motive = directRed ? "Straight Red Card" : "Second Yellow / Red Card";
        return minute + "' Red Card (" + motive + ") - " + player.getName() + " [" + team.getName() + "]";
    }
}
