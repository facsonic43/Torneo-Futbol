package main.loader;

import model.participant.Referee;
import model.participant.Team;

import java.util.List;


// permite crear el torneo y despues llamar al getTeams y getReferees
// agrupa las listas de equipos y arbitros para leer el archivo una sola vez
public class TournamentData {
    private List<Team> teams;
    private List<Referee> referees;

    public TournamentData(List<Team> teams, List<Referee> referees) {
        this.teams = teams;
        this.referees = referees;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public List<Referee> getReferees() {
        return referees;
    }
}