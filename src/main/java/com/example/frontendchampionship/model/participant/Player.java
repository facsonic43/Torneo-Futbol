package com.example.frontendchampionship.model.participant;

import java.time.LocalDate;

public abstract class Player extends Person{
    int matchesPlayed;
    int minutesPlayed;
    int yellowCards;
    int redCards;
    int goals;
    int assists;

    public Player(String name, int idNumber, String idType, LocalDate birthDate, Country nationality, int matchesPlayed, int minutesPlayed, int yellowCards, int redCards, int goals, int assists) {
        super(name, idNumber, idType, birthDate, nationality);
        this.matchesPlayed = matchesPlayed;
        this.minutesPlayed = minutesPlayed;
        this.yellowCards = yellowCards;
        this.redCards = redCards;
        this.goals = goals;
        this.assists = assists;
    }

    public abstract double getOverall();
}
