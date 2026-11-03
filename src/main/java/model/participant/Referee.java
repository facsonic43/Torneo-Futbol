package model.participant;

import java.time.LocalDate;

// Representa al árbitro y comprueba si su nacionalidad permite dirigir el encuentro.
// Cuando ambos equipos son del mismo país no se aplica la restricción de nacionalidad.

public class Referee extends Person {
    private int matchesOfficiated;
    private int yearsOfficiated;

    public Referee(String name, int idNumber, String idType, LocalDate birthDate, Country nationality,
            int matchesOfficiated, int yearsOfficiated) {
        super(name, idNumber, idType, birthDate, nationality);
        this.matchesOfficiated = matchesOfficiated;
        this.yearsOfficiated = yearsOfficiated;
    }

    public boolean canOfficiate(Team team1, Team team2) {
        if (team1.getCountry().equals(team2.getCountry())) {
            return true;
        }
        return !team1.getCountry().equals(getNationality()) && !team2.getCountry().equals(getNationality());
    }

    public void addMatchOfficiated() {
        matchesOfficiated++;
    }

    public int getMatchesOfficiated() {
        return matchesOfficiated;
    }

    public int getYearsOfficiated() {
        return yearsOfficiated;
    }
}
