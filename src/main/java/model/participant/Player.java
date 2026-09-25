package model.participant;

import java.time.LocalDate;

/*
 * Representa los datos y estadísticas comunes de todos los jugadores del torneo.
 * También administra tarjetas, suspensiones, lesiones y goles de penal para poder
 * construir correctamente los reportes y controlar la disponibilidad entre partidos.
 */
public abstract class Player extends Person {
    protected int matchesPlayed;
    protected int minutesPlayed;
    protected int yellowCards;
    protected int redCards;
    protected int goals;
    protected int assists;
    protected int penaltyGoals;

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
        this.penaltyGoals = 0;
    }

    public abstract double getOverall();

    public abstract Position getPosition();

    public boolean isAvailable() {
        return suspensionMatchesLeft == 0 && injuryMatchesLeft == 0;
    }

    public boolean isInjured() {
        return injuryMatchesLeft > 0;
    }

    public boolean isSuspended() {
        return suspensionMatchesLeft > 0;
    }

    public void injure(int matches) {
        injuryMatchesLeft = matches;
    }

    public void addYellowCard() {
        yellowCards++;
        tournamentYellowCards++;

        if (tournamentYellowCards >= 3) {
            suspensionMatchesLeft = 1;
            tournamentYellowCards = 0;
        }
    }

    public void addRedCard() {
        redCards++;
        suspensionMatchesLeft = 1;
    }

    public void updateMatchAvailability() {
        if (suspensionMatchesLeft > 0) {
            suspensionMatchesLeft--;
        }

        if (injuryMatchesLeft > 0) {
            injuryMatchesLeft--;
        }
    }

    public void addGoal() {
        goals++;
    }

    public void addPenaltyGoal() {
        penaltyGoals++;
    }

    public void addAssist() {
        assists++;
    }

    public void addMatchesPlayed() {
        matchesPlayed++;
    }

    public void addMinutesPlayed(int minutes) {
        minutesPlayed += minutes;
    }

    public int getMatchesPlayed() {
        return matchesPlayed;
    }

    public int getMinutesPlayed() {
        return minutesPlayed;
    }

    public int getYellowCards() {
        return yellowCards;
    }

    public int getRedCards() {
        return redCards;
    }

    public int getGoals() {
        return goals;
    }

    public int getAssists() {
        return assists;
    }

    public int getPenaltyGoals() {
        return penaltyGoals;
    }

    public int getSuspensionMatchesLeft() {
        return suspensionMatchesLeft;
    }

    public int getInjuryMatchesLeft() {
        return injuryMatchesLeft;
    }

    public int getTournamentYellowCards() {
        return tournamentYellowCards;
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