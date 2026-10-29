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

    /** Double dispatch: the incident tells the visitor which concrete type it is. */
    public abstract void accept(EventVisitor visitor);

    /** True only for the kicks of a penalty shootout, which are not incidents of the 90 minutes. */
    public boolean belongsToShootout() {
        return false;
    }
}
