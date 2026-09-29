package model.match;

import model.participant.Player;
import model.participant.Team;

/*
 * Representa un gol ocurrido durante los 90 minutos de un partido.
 * Guarda si fue de penal, si fue en contra, quién asistió y qué jugador
 * estaba ocupando el arco cuando se produjo el gol.
 */
public class Goal extends Event {
    private Player assistPlayer;
    private boolean penalty;
    private boolean ownGoal;
    private Player goalkeeperConceded;

    public Goal(int minute, Team team, Player scorer, Player assistPlayer,
                boolean penalty, boolean ownGoal, Player goalkeeperConceded) {
        super(minute, team, scorer);
        this.assistPlayer = assistPlayer;
        this.penalty = penalty;
        this.ownGoal = ownGoal;
        this.goalkeeperConceded = goalkeeperConceded;
    }

    public Player getScorer() {
        return player;
    }

    public Player getAssistPlayer() {
        return assistPlayer;
    }

    public boolean isPenalty() {
        return penalty;
    }

    public boolean isOwnGoal() {
        return ownGoal;
    }

    public Player getGoalkeeperConceded() {
        return goalkeeperConceded;
    }

    @Override
    public String getDescription() {
        String description =
                minute + "' GOL - " + player.getName();

        if (ownGoal) {
            description += " (Gol en contra)";
        } else if (penalty) {
            description += " (Penal)";
        } else if (assistPlayer != null) {
            description +=
                    " (Asistencia: "
                            + assistPlayer.getName()
                            + ")";
        }

        description +=
                " [" + team.getName() + "]";

        return description;
    }
}