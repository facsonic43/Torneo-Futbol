package model.match;

import model.participant.Player;
import model.participant.Team;

public class Goal extends Event {
    private Player assistPlayer;

    public Goal(int minute, Team team, Player scorer, Player assistPlayer) {
        super(minute, team, scorer);
        this.assistPlayer = assistPlayer;
    }

    public Player getAssistPlayer() {
        return assistPlayer;
    }

    @Override
    public String getDescription() {
        if (assistPlayer != null) {
            return minute + "' GOAL - " + player.getName() + " (Assist: " + assistPlayer.getName() + ") [" + team.getName() + "]";
        }
        return minute + "' GOAL - " + player.getName() + " [" + team.getName() + "]";
    }
}
