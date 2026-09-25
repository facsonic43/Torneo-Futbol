package model.participant;

import java.io.Serializable;

/*
 * Guarda la participación real de un jugador dentro de un partido.
 * Permite saber si fue titular, cuándo ingresó, cuándo salió y cuántos minutos jugó,
 * para registrar correctamente las estadísticas aunque haya cambios, lesiones o expulsiones.
 */
public class PlayerParticipation implements Serializable{
    private Player player;
    private Team team;
    private boolean starting;
    private int enterMinute;
    private int exitMinute;

    public PlayerParticipation(Player player, Team team, boolean starting, int enterMinute) {
        this.player = player;
        this.team = team;
        this.starting = starting;
        this.enterMinute = enterMinute;
        this.exitMinute = 90;
    }

    public Player getPlayer() {
        return player;
    }

    public Team getTeam() {
        return team;
    }

    public boolean isStarting() {
        return starting;
    }

    public int getEnterMinute() {
        return enterMinute;
    }

    public int getExitMinute() {
        return exitMinute;
    }

    public void setExitMinute(int exitMinute) {
        this.exitMinute = exitMinute;
    }

    public int getMinutesPlayed() {
        return Math.max(0, exitMinute - enterMinute);
    }
}