package model.match;

import model.participant.Player;
import model.participant.Team;

public class YellowCard extends Event {

    public YellowCard(int minute, Team team, Player player) {
        super(minute, team, player);
    }

    @Override
    public String getDescription() {
        return minute + "' Yellow Card - " + player.getName() + " [" + team.getName() + "]";
    }
}
