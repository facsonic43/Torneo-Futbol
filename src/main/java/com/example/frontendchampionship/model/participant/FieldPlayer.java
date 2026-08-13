package com.example.frontendchampionship.model.participant;

import java.time.LocalDate;

public class FieldPlayer extends Player {
    Position position;
    private int dribbling;          //regate
    private int defensiveSkills;        //capacidad defensiva
    private int finishing;      //remate al arco
    private int stamina;        //energía
    private int vision;     //vision de juego
    private int heading;        //cabezazo
    private int speed;      //velocidad
    private int passing;        //pase

    public FieldPlayer(String name, int idNumber, String idType, LocalDate birthDate, Country nationality, int matchesPlayed, int minutesPlayed, int yellowCards, int redCards, int goals, int assists, Position position, int dribbling, int defensiveSkills, int finishing, int stamina, int vision, int heading, int speed, int passing) {
        super(name, idNumber, idType, birthDate, nationality, matchesPlayed, minutesPlayed, yellowCards, redCards, goals, assists);
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

    public Position getPosition () {
        return position;
    }

    @Override
    public double getOverall() {
        return switch (this.getPosition()) {
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
            default -> (dribbling
                    + defensiveSkills
                    + finishing
                    + stamina
                    + vision
                    + heading
                    + speed
                    + passing)/8.0;
        };
    }
}