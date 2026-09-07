package model.participant;

import java.time.LocalDate;

public class Coach extends Person{
    private int titlesObtained;

    public Coach(String name, int idNumber, String idType, LocalDate birthDate, Country nationality, int titlesObtained) {
        super(name, idNumber, idType, birthDate, nationality);
        this.titlesObtained = titlesObtained;
    }

    public int getTitlesObtained() {
        return titlesObtained;
    }

    @Override
    protected String getAditionalInfo() {
        StringBuilder sb=new StringBuilder();
        sb.append("Tittles obtained: "+titlesObtained);
        return sb.toString();
    }
}
