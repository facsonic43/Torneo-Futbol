package model.tournament;

import model.participant.Team;

public class Standing implements Comparable<Standing> {
    private Team team;
    private int played = 0;
    private int won = 0;
    private int drawn = 0;
    private int lost = 0;
    private int scored = 0;
    private int conceded = 0;

    public Standing(Team team) {
        this.team = team;
    }

    public void update(int gS, int gC) {
        this.played++;
        this.scored += gS;
        this.conceded += gC;

        if (gS > gC) {
            this.won++;
        } else if (gS == gC) {
            this.drawn++;
        } else {
            this.lost++;
        }
    }

    public int getPoints() {
        return (won * 3) + drawn;
    }

    public int getGoalDifference() {
        return scored - conceded;
    }

    public Team getTeam() {
        return team;
    }

    public int getPlayed() {
        return played;
    }

    public int getWon() {
        return won;
    }

    public int getDrawn() {
        return drawn;
    }

    public int getLost() {
        return lost;
    }

    public int getScored() {
        return scored;
    }

    public int getConceded() {
        return conceded;
    }

    @Override
    public int compareTo(Standing t2) {
        if (this.getPoints() != t2.getPoints()) {
            return Integer.compare(t2.getPoints(), this.getPoints());
        }
        if (this.getGoalDifference() != t2.getGoalDifference()) {
            return Integer.compare(t2.getGoalDifference(), this.getGoalDifference());
        }
        return Integer.compare(t2.getScored(), this.getScored());
    }
}
