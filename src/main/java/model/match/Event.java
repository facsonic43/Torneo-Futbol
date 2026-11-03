package model.match;

import model.participant.Player;
import model.participant.Team;
import java.io.Serializable;

// Define los datos comunes de una incidencia: minuto, equipo y futbolista involucrados.
// Cada tipo aporta su descripción y se procesa mediante un visitante; las ejecuciones
// de una tanda se distinguen de las incidencias del tiempo de juego.

public abstract class Event implements Serializable{
    private int minute;
    private Team team;
    private Player player;

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

    public abstract void accept(EventVisitor visitor);

    public boolean belongsToShootout() {
        return false;
    }
}
