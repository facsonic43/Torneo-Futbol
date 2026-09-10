package model.match;

import model.participant.Player;
import model.participant.Team;

public class Injury extends Event {
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
        return minute + "' Injury - " + player.getName() + " [" + team.getName() + "] (Out for " + matchesOut + " fixture/s)";
    }
}
