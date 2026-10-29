package gui;

import javafx.scene.control.*;

public class RefereeReportEntry {
    private final String name;
    private final String nationality;
    private final java.time.LocalDate birthDate;
    private final String idType;
    private final int idNumber;
    private final int yearsOfficiated;
    private final int matchesOfficiated;

    public RefereeReportEntry(String name, String nationality, java.time.LocalDate birthDate,
                              String idType, int idNumber, int yearsOfficiated, int matchesOfficiated) {
        this.name = name;
        this.nationality = nationality;
        this.birthDate = birthDate;
        this.idType = idType;
        this.idNumber = idNumber;
        this.yearsOfficiated = yearsOfficiated;
        this.matchesOfficiated = matchesOfficiated;
    }

    public String getName() { return name; }
    public String getNationality() { return nationality; }
    public java.time.LocalDate getBirthDate() { return birthDate; }
    public String getIdType() { return idType; }
    public int getIdNumber() { return idNumber; }
    public int getYearsOfficiated() { return yearsOfficiated; }
    public int getMatchesOfficiated() { return matchesOfficiated; }
}
