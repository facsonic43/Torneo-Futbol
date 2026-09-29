package model.participant;

import java.time.LocalDate;

/*
 * Representa a un arquero con sus habilidades específicas.
 * Además guarda la cantidad de goles recibidos durante el torneo para utilizarla
 * posteriormente en los reportes estadísticos de jugadores por posición.
 */
public class Goalkeeper extends Player {
    private int speed;
    private int jumping;
    private int passing;
    private int reflexes;
    private int oneOnOne;
    private int kicking;
    private int goalsConceded;

    public Goalkeeper(String name, int idNumber, String idType, LocalDate birthDate, Country nationality,
                      int matchesPlayed, int minutesPlayed, int yellowCards, int redCards, int goals, int assists,
                      int speed, int jumping, int passing, int reflexes, int oneOnOne, int kicking) {
        super(name, idNumber, idType, birthDate, nationality, matchesPlayed, minutesPlayed,
                yellowCards, redCards, goals, assists);
        this.speed = speed;
        this.jumping = jumping;
        this.passing = passing;
        this.reflexes = reflexes;
        this.oneOnOne = oneOnOne;
        this.kicking = kicking;
        this.goalsConceded = 0;
    }

    @Override
    public double getOverall() {
        return (speed + jumping + passing + reflexes + oneOnOne + kicking) / 6.0;
    }

    @Override
    public Position getPosition() {
        return Position.GOALKEEPER;
    }

    public void addGoalConceded() {
        goalsConceded++;
    }

    public int getGoalsConceded() {
        return goalsConceded;
    }
}