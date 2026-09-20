package model.match;

import model.participant.Player;
import model.participant.Team;
import java.io.Serializable;

public abstract class Event implements Serializable{
    protected int minute;
    protected Team team;
    protected Player player;

    public Event(int minute, Team team, Player player) {
        this.minute = minute;
        this.team = team;
        this.player = player;
    }

    public int getMinute() {
        return minute;
    }

    public Team getTeam() {
        return team;
    }

    public Player getPlayer() {
        return player;
    }

    public abstract String getDescription();
}