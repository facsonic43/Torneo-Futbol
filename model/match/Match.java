package model.match;

import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class Match {
    protected Team homeTeam;
    protected Team awayTeam;
    protected Referee referee;
    protected Stadium stadium;
    protected LocalDate matchDate;

    protected int homeGoals = 0;
    protected int awayGoals = 0;
    protected Integer homePenalties = null;
    protected Integer awayPenalties = null;

    protected boolean played = false;
    protected boolean extraTimePlayed = false;
    protected List<Event> events = new ArrayList<>();

    public Match(Team homeTeam, Team awayTeam, Referee referee, Stadium stadium, LocalDate matchDate) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.referee = referee;
        this.stadium = stadium;
        this.matchDate = matchDate;
    }

    public abstract boolean isKnockout();
    public abstract boolean isTied();

    public void addEvent(Event event) {
        this.events.add(event);
    }

    public Team getHomeTeam() { return homeTeam; }
    public Team getAwayTeam() { return awayTeam; }
    public Referee getReferee() { return referee; }
    public Stadium getStadium() { return stadium; }
    public LocalDate getMatchDate() { return matchDate; }

    public int getHomeGoals() { return homeGoals; }
    public void setHomeGoals(int homeGoals) { this.homeGoals = homeGoals; }

    public int getAwayGoals() { return awayGoals; }
    public void setAwayGoals(int awayGoals) { this.awayGoals = awayGoals; }

    public Integer getHomePenalties() { return homePenalties; }
    public void setHomePenalties(Integer homePenalties) { this.homePenalties = homePenalties; }

    public Integer getAwayPenalties() { return awayPenalties; }
    public void setAwayPenalties(Integer awayPenalties) { this.awayPenalties = awayPenalties; }

    public boolean isPlayed() { return played; }
    public void setPlayed(boolean played) { this.played = played; }

    public boolean isExtraTimePlayed() { return extraTimePlayed; }
    public void setExtraTimePlayed(boolean extraTimePlayed) { this.extraTimePlayed = extraTimePlayed; }

    public List<Event> getEvents() { return events; }
}