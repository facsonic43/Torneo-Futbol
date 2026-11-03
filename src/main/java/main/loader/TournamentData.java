package main.loader;

import model.participant.Referee;
import model.participant.Team;

import java.util.List;

/**
 * Agrupa los equipos y árbitros cargados para crear el torneo sin volver a leer el archivo inicial.
 */
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
