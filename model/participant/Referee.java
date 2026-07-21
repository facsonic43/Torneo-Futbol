package model.participant;

import java.time.LocalDate;

public class Referee extends Person {
    private int matchesOfficiated;
    private int yearsOfficiated;

    public Referee(String name, int idNumber, String idType, LocalDate birthDate, Country nationality, int matchesOfficiated, int yearsOfficiated) {
        super(name, idNumber, idType, birthDate, nationality);
        this.matchesOfficiated = matchesOfficiated;
        this.yearsOfficiated = yearsOfficiated;
    }
}
