package model.participant;

import java.time.LocalDate;
import java.util.Map;

/**
 * Reúne los datos y estadísticas comunes de los futbolistas y controla su disponibilidad
 * por lesiones y suspensiones. Cada especialidad define su posición y valoración;
 * los goles recibidos se registran únicamente en los arqueros.
 */
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

    public boolean isGoalkeeper() {
        return false;
    }

    public int getGoalsConceded() {
        return 0;
    }

    public void addGoalConceded() {
    }

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

    public int getSuspensionMatchesLeft() {
        return suspensionMatchesLeft;
    }

    public int getInjuryMatchesLeft() {
        return injuryMatchesLeft;
    }

    public int getFinishing(){ return 0; }

    public abstract Map<String, Integer> getTechnicalAttributes();
}
