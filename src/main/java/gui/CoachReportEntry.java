package gui;

import javafx.scene.control.*;

public class CoachReportEntry {
    private final String name;
    private final String teamName;
    private final String nationality;
    private final int titlesWon;
    private final java.time.LocalDate birthDate;

    public CoachReportEntry(String name, String teamName, String nationality, int titlesWon, java.time.LocalDate birthDate) {
        this.name = name;
        this.teamName = teamName;
        this.nationality = nationality;
        this.titlesWon = titlesWon;
        this.birthDate = birthDate;
    }

    public String getName() { return name; }
    public String getTeamName() { return teamName; }
    public String getNationality() { return nationality; }
    public int getTitlesWon() { return titlesWon; }
    public java.time.LocalDate getBirthDate() { return birthDate; }
}
