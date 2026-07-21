package model.participant;

import java.time.LocalDate;

public class Goalkeeper extends Player{
    private int speed;      //velocidad
    private int jumping;        //salto
    private int passing;        //pases
    private int reflexes;       //reflejos
    private int oneOnOne;       //mano a mano
    private int kicking;        //saque largo

    public Goalkeeper(String name, int idNumber, String idType, LocalDate birthDate, Country nationality, int matchesPlayed, int minutesPlayed, int yellowCards, int redCards, int goals, int assists, int speed, int jumping, int passing, int reflexes, int oneOnOne, int kicking) {
        super(name, idNumber, idType, birthDate, nationality, matchesPlayed, minutesPlayed, yellowCards, redCards, goals, assists);
        this.speed = speed;
        this.jumping = jumping;
        this.passing = passing;
        this.reflexes = reflexes;
        this.oneOnOne = oneOnOne;
        this.kicking = kicking;
    }

    @Override
    public double getOverall() {
        return (speed + jumping + passing + reflexes + oneOnOne + kicking)/6.0;
    }
}
