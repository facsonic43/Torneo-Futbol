package model.participant;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

// Representa al arquero con sus habilidades específicas y calcula su valoración.
// Registra los goles recibidos para las estadísticas y reportes del campeonato.

public class Goalkeeper extends Player {
    private static final long serialVersionUID = -3494408344082799670L;
    private int speed;
    private int jumping;
    private int passing;
    private int reflexes;
    private int oneOnOne;
    private int kicking;
    private int goalsConceded;

    public Goalkeeper(String name, int idNumber, String idType, LocalDate birthDate, Country nationality,
            int matchesPlayed, int minutesPlayed, int yellowCards, int redCards, int goals, int assists, int speed,
            int jumping, int passing, int reflexes, int oneOnOne, int kicking) {
        super(name, idNumber, idType, birthDate, nationality, matchesPlayed, minutesPlayed, yellowCards, redCards,
                goals, assists);
        this.speed = speed;
        this.jumping = jumping;
        this.passing = passing;
        this.reflexes = reflexes;
        this.oneOnOne = oneOnOne;
        this.kicking = kicking;
        this.goalsConceded = 0;
    }

    @Override
    public int getGoalsConceded() {
        return goalsConceded;
    }

    public int getSpeed() {
        return speed;
    }

    public int getJumping() {
        return jumping;
    }

    public int getPassing() {
        return passing;
    }

    public int getReflexes() {
        return reflexes;
    }

    public int getOneOnOne() {
        return oneOnOne;
    }

    public int getKicking() {
        return kicking;
    }

    @Override
    public double getOverall() {
        return (speed + jumping + passing + reflexes + oneOnOne + kicking) / 6.0;
    }

    @Override
    public Position getPosition() {
        return Position.GOALKEEPER;
    }

    @Override
    public boolean isGoalkeeper() {
        return true;
    }

    @Override
    public Map<String, Integer> getTechnicalAttributes() {
        Map<String, Integer> attributes = new LinkedHashMap<>();
        attributes.put("Reflexes", reflexes);
        attributes.put("One On One", oneOnOne);
        attributes.put("Kicking", kicking);
        attributes.put("Passing", passing);
        attributes.put("Speed", speed);
        attributes.put("Jumping", jumping);
        return attributes;
    }

    @Override
    public void addGoalConceded() {
        goalsConceded++;
    }
}
