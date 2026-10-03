package gui;

import model.participant.Goalkeeper;
import model.participant.Player;
import model.participant.Position;
import model.participant.Team;

public class PlayerDirectoryEntry {
    private final Player player;
    private final Team team;

    public PlayerDirectoryEntry(Player player, Team team) {
        this.player = player;
        this.team = team;
    }

    public Player getPlayer() { return player; }
    public Team getTeam() { return team; }
    public String getPlayerName() { return player.getName(); }
    public Position getPosition() { return player.getPosition(); }
    public String getTeamName() { return team != null ? team.getName() : "-"; }
    public int getMatchesPlayed() { return player.getMatchesPlayed(); }
    public int getMinutesPlayed() { return player.getMinutesPlayed(); }
    public int getGoals() { return player.getGoals(); }

    public Integer getGoalsConceded() {
        if (player instanceof Goalkeeper) {
            return ((Goalkeeper) player).getGoalsConceded();
        }
        return null; // Null indica que no es arquero para formatear con '-'
    }
}