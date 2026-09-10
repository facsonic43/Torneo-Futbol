package model.participant;

import exceptions.*;
import model.match.Stadium;

import java.util.ArrayList;
import java.util.List;

public class Team {
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

    public void addPlayer(Player p){
        if(squad.size()<18)
            squad.add(p);
        else
            throw new FullSquadException();
    }

    public double getOverall(){
        if(squad.isEmpty()) return 0.0;
        double total=0.0;

        for(Player p : squad)
            total += p.getOverall();

        return total/squad.size();
    }

    public double getPrestigeRating() {//metodo que se va a usar para sacar la 'fuerza del equipo'
                                                    // para hacer simulaciones realistas

        // Cada título internacional suma 10 pts, cada título nacional 4 pts y cada edición jugada 2 pts.
        double prestige = (internationalTitles * 5.0)
                + (nationalTitles * 2.0)
                + (editionsPlayed * 1.0);

        // Math.min asegura que el prestigio no supere nunca los 100 puntos
        return Math.min(100.0, prestige);
    }

    public double getTeamPower(){
        // 1. Rating del Plantel (Escala 0 - 100)
        double squadRating = this.getOverall();

        // 2. Rating del Ranking Continental (Escala 0 - 100)
        double rankingRating = Math.max(0.0, 100.0 - this.ranking);

        // 3. Prestigio Histórico del Club (Escala 0 - 100)
        double prestigeRating = this.getPrestigeRating();

        // 4. Jerarquía del DT (Escala 0 - 100)
        double coachRating = Math.min(100.0, this.coach.getTitlesObtained() * 5.0);

        // 50% PLANTEL ACTUAL - 25% PRESTIGIO HISTÓRICO - 15% PRESTIGIO DT - 10% RANKING ACTUAL
        double totalPower = (squadRating    * 0.50)
                + (prestigeRating * 0.25)
                + (coachRating    * 0.15)
                + (rankingRating  * 0.10);

        // Retorna acotado entre 0.0 y 100.0
        return Math.min(100.0, Math.max(0.0, totalPower));
    }
}
