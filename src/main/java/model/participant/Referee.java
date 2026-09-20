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

    //revisa si el arbitro es de misma nacion que uno de los dos equipos
    //si los dos equipos son de la misma nacion, no hay restriccion de arbitro
    public boolean canOfficiate(Team t1,Team t2){
        if(t1.getCountry().equals(t2.getCountry()))
            return true;
        else return !t1.getCountry().equals(this.getNationality()) && !t2.getCountry().equals(this.getNationality());
    }

    @Override
    protected String getAditionalInfo() {
        StringBuilder sb=new StringBuilder();
        sb.append("Matches officiated: "+matchesOfficiated+"\tYears officiated: "+yearsOfficiated);
        return sb.toString();
    }
}
