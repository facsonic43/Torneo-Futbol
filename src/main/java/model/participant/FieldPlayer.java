package model.participant;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Outfield player (defender, midfielder or forward). Its overall rating weighs the
 * common skills differently depending on the position it plays.
 */
public class FieldPlayer extends Player {

    private static final long serialVersionUID = -2473516079032478862L;

    private final Position position;
    private final int dribbling;
    private final int defensiveSkills;
    private final int finishing;
    private final int stamina;
    private final int vision;
    private final int heading;
    private final int speed;
    private final int passing;

    public FieldPlayer(String name, int idNumber, String idType, LocalDate birthDate, Country nationality,
                       int matchesPlayed, int minutesPlayed, int yellowCards, int redCards, int goals, int assists,
                       Position position, int dribbling, int defensiveSkills, int finishing, int stamina,
                       int vision, int heading, int speed, int passing) {
        super(name, idNumber, idType, birthDate, nationality, matchesPlayed, minutesPlayed,
                yellowCards, redCards, goals, assists);

        if (position == Position.GOALKEEPER) {
            throw new IllegalArgumentException("A goalkeeper is not a field player: " + name);
        }

        this.position = position;
        this.dribbling = dribbling;
        this.defensiveSkills = defensiveSkills;
        this.finishing = finishing;
        this.stamina = stamina;
        this.vision = vision;
        this.heading = heading;
        this.speed = speed;
        this.passing = passing;
    }

    @Override
    public double getOverall() {
        return switch (position) {
            case DEFENDER -> (dribbling * 0.05)
                    + (defensiveSkills * 0.35)
                    + (finishing * 0.05)
                    + (stamina * 0.10)
                    + (vision * 0.05)
                    + (heading * 0.20)
                    + (speed * 0.10)
                    + (passing * 0.10);
            case MIDFIELDER -> (dribbling * 0.15)
                    + (defensiveSkills * 0.15)
                    + (finishing * 0.05)
                    + (stamina * 0.15)
                    + (vision * 0.20)
                    + (heading * 0.05)
                    + (speed * 0.05)
                    + (passing * 0.20);
            case FORWARD -> (dribbling * 0.10)
                    + (defensiveSkills * 0.05)
                    + (finishing * 0.30)
                    + (stamina * 0.10)
                    + (vision * 0.05)
                    + (heading * 0.15)
                    + (speed * 0.15)
                    + (passing * 0.10);
            case GOALKEEPER -> throw new IllegalStateException("A field player cannot be a goalkeeper");
        };
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public int getFinishing(){ return finishing; }

    @Override
    public Map<String, Integer> getTechnicalAttributes() {
        Map<String, Integer> attributes = new LinkedHashMap<>();
        attributes.put("Finishing", finishing);
        attributes.put("Dribbling", dribbling);
        attributes.put("Passing", passing);
        attributes.put("Vision", vision);
        attributes.put("Speed", speed);
        attributes.put("Stamina", stamina);
        attributes.put("Heading", heading);
        attributes.put("Defensive Skills", defensiveSkills);
        return attributes;
    }
}
