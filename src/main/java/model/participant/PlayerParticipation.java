package model.participant;

import java.io.Serializable;

// Registra la participación de un futbolista en un equipo durante un partido,
// incluidos los minutos de entrada y salida que permiten calcular el tiempo jugado.

public class PlayerParticipation implements Serializable{
    private static final long serialVersionUID = 5740780584945396040L;
    private Player player;
    private Team team;
    private int enterMinute;
    private int exitMinute;

    public PlayerParticipation(Player player, Team team, int enterMinute) {
        this.player = player;
        this.team = team;
        this.enterMinute = enterMinute;
        this.exitMinute = 90;
    }

    public Player getPlayer() {
        return player;
    }

    public Team getTeam() {
        return team;
    }

    public int getExitMinute() {
        return exitMinute;
    }

    public void setExitMinute(int exitMinute) {
        this.exitMinute = exitMinute;
    }

    public int getEnterMinute() {
        return enterMinute;
    }

    public int getMinutesPlayed() {
        return Math.max(0, exitMinute - enterMinute);
    }
}
