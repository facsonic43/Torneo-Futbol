package model.participant;

import exceptions.*;
import model.match.Stadium;

import java.util.ArrayList;
import java.util.List;

import java.io.Serializable;

// Mantiene los datos del club, su entrenador, estadio y plantel de hasta 18 jugadores.
// Calcula su valoración y potencia combinando habilidades del plantel, ranking, historial
// y títulos del entrenador para orientar las simulaciones.

public class Team implements Serializable{
    private static final long serialVersionUID = 7043864968606219809L;
    private String name;
    private Country country;
    private int ranking;
    private Coach coach;
    private List<Player> squad;
    private int editionsPlayed;
    private int internationalTitles;
    private int nationalTitles;
    private Stadium stadium;

    public Team(String name, Country country, int ranking, Coach coach, int editionsPlayed,int internationalTitles, int nationalTitles,Stadium stadium) {
        this.name = name;
        this.country = country;
        this.ranking = ranking;
        this.coach = coach;
        this.editionsPlayed = editionsPlayed;
        this.internationalTitles = internationalTitles;
        this.nationalTitles = nationalTitles;
        this.squad = new ArrayList<>();
        this.stadium = stadium;
    }

    public String getName() {
        return name;
    }

    public Country getCountry() {
        return country;
    }

    public int getRanking() {
        return ranking;
    }

    public Coach getCoach() {
        return coach;
    }

    public List<Player> getSquad() {
        return squad;
    }

    public Stadium getStadium() { return stadium; }

    public int getEditionsPlayed() {
        return editionsPlayed;
    }

    public int getInternationalTitles() {
        return internationalTitles;
    }

    public int getNationalTitles() {
        return nationalTitles;
    }

    public double getOverall(){
        if(squad.isEmpty()) return 0.0;
        double total=0.0;
        for(Player p : squad)
            total += p.getOverall();
        return total/squad.size();
    }

    private double getPrestigeRating() {
        double prestige = (internationalTitles * 5.0) + (nationalTitles * 2.0) + (editionsPlayed * 1.0);
        return Math.min(100.0, prestige);
    }

    public double getTeamPower(){
        double squadRating = this.getOverall();
        double rankingRating = Math.max(0.0, 100.0 - this.ranking);
        double prestigeRating = this.getPrestigeRating();
        double coachRating = Math.min(100.0, this.coach.getTitlesObtained() * 5.0);
        double totalPower = (squadRating    * 0.50) + (prestigeRating * 0.25)
                + (coachRating    * 0.15) + (rankingRating  * 0.10);
        return Math.min(100.0, Math.max(0.0, totalPower));
    }

    public void addPlayer(Player p){
        if(squad.size()<18)
            squad.add(p);
        else
            throw new FullSquadException();
    }
}
