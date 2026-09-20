package model.participant;

import java.time.LocalDate;

/*
 * Representa a un árbitro del campeonato y controla si puede dirigir un encuentro.
 * También registra la cantidad de partidos dirigidos para poder generar luego
 * el ranking de árbitros solicitado por el trabajo práctico.
 */
public class Referee extends Person {
    private int matchesOfficiated;
    private int yearsOfficiated;

    public Referee(String name, int idNumber, String idType, LocalDate birthDate, Country nationality,
                   int matchesOfficiated, int yearsOfficiated) {
        super(name, idNumber, idType, birthDate, nationality);
        this.matchesOfficiated = matchesOfficiated;
        this.yearsOfficiated = yearsOfficiated;
    }

    //revisa si el arbitro es de misma nacion que uno de los dos equipos
    //si los dos equipos son de la misma nacion, no hay restriccion de arbitro
    public boolean canOfficiate(Team team1, Team team2) {
        if (team1.getCountry().equals(team2.getCountry())) {
            return true;
        }

        return !team1.getCountry().equals(getNationality())
                && !team2.getCountry().equals(getNationality());
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