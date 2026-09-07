package model.participant;

import java.time.LocalDate;

public abstract class Player extends Person {
    protected int matchesPlayed;
    protected int minutesPlayed;
    protected int yellowCards;
    protected int redCards;
    protected int goals;
    protected int assists;

    protected int suspensionMatchesLeft = 0;
    protected int injuryMatchesLeft = 0;
    protected int tournamentYellowCards = 0;

    public Player(String name, int idNumber, String idType, LocalDate birthDate, Country nationality,
                  int matchesPlayed, int minutesPlayed, int yellowCards, int redCards, int goals, int assists) {
        super(name, idNumber, idType, birthDate, nationality);
        this.matchesPlayed = matchesPlayed;
        this.minutesPlayed = minutesPlayed;
        this.yellowCards = yellowCards;
        this.redCards = redCards;
        this.goals = goals;
        this.assists = assists;
    }

    public abstract double getOverall();
    public abstract Position getPosition();

    public boolean isAvailable() {
        return this.suspensionMatchesLeft == 0 && this.injuryMatchesLeft == 0;
    }

    public boolean isInjured() {
        return this.injuryMatchesLeft > 0;
    }

    public boolean isSuspended() {
        return this.suspensionMatchesLeft > 0;
    }

    public void injure(int matches) {
        this.injuryMatchesLeft = matches;
    }

    public void addYellowCard() {
        this.yellowCards++;
        this.tournamentYellowCards++;
        if (this.tournamentYellowCards >= 3) {
            this.suspensionMatchesLeft = 1;
            this.tournamentYellowCards = 0;
        }
    }

    public void addRedCard() {
        this.redCards++;
        this.suspensionMatchesLeft = 1;
    }

    public void updateMatchAvailability() {
        if (this.suspensionMatchesLeft > 0) {
            this.suspensionMatchesLeft--;
        }
        if (this.injuryMatchesLeft > 0) {
            this.injuryMatchesLeft--;
        }
    }

    public void addGoal() { this.goals++; }
    public void addAssist() { this.assists++; }
    public void addMatchesPlayed() { this.matchesPlayed++; }
    public void addMinutesPlayed(int mins) { this.minutesPlayed += mins; }

    public int getMatchesPlayed() { return matchesPlayed; }
    public int getMinutesPlayed() { return minutesPlayed; }
    public int getYellowCards() { return yellowCards; }
    public int getRedCards() { return redCards; }
    public int getGoals() { return goals; }
    public int getAssists() { return assists; }
    public int getSuspensionMatchesLeft() { return suspensionMatchesLeft; }
    public int getInjuryMatchesLeft() { return injuryMatchesLeft; }

    @Override
    protected String getAditionalInfo() {
        StringBuilder sb=new StringBuilder();
        sb.append("Matches played: "+matchesPlayed+"\tMinutes played: "+minutesPlayed+"\tYellow Cards: "+yellowCards+"\tRed Cards: "+redCards+"\tGoals: "+goals+"\tAssits: "+assists);
        return sb.toString();
    }
}