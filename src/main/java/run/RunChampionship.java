package run;

import PDF.PDFGenerator;
import control.MatchSimulator;
import dao.CityDAO;
import dao.StadiumDAO;
import main.loader.TournamentData;
import main.loader.TournamentDataLoader;
import model.match.City;
import model.match.Event;
import model.match.GroupMatch;
import model.match.Stadium;
import model.participant.Player;
import model.participant.Team;
import model.tournament.Group;
import model.tournament.Standing;
import model.tournament.Tournament;
import reports.ReportData;
import reports.ReportOptions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;

public class RunChampionship {

    public static void main(String[] args) {
        TournamentDataLoader loader = new TournamentDataLoader();

        try {
            TournamentData data = loader.load("torneo.json");
            System.out.println("\n====================================");
            System.out.println("BASE DE DATOS - CIUDADES Y ESTADIOS");
            System.out.println("====================================");

            CityDAO cityDAO = new CityDAO();
            StadiumDAO stadiumDAO = new StadiumDAO();

            List<City> cities = cityDAO.findAll();
            List<Stadium> stadiums = stadiumDAO.findAll();

            System.out.println("Ciudades cargadas desde la DB: " + cities.size());
            for (City city : cities) {
                System.out.println("- " + city.getName() + " | " + city.getCountry());
            }

            System.out.println("\nEstadios cargados desde la DB: " + stadiums.size());
            for (Stadium stadium : stadiums) {
                System.out.println("- " + stadium.getName() + " (id ciudad: " + stadium.getCityId() + ")");
            }
            Tournament tournament = new Tournament();
            List<Group> groups = tournament.drawGroups(data.getTeams());
            tournament.generateGroupMatches(groups, data.getReferees(), stadiums);

            MatchSimulator simulator = new MatchSimulator();
            tournament.simulateGroupStage(groups, simulator);

            ReportData reportData = ReportData.fromInitialData(data);
            groups.forEach(reportData::addGroup);
            PDFGenerator.generate(reportData, Path.of("Report.pdf"), ReportOptions.defaults());
            System.out.println("PDF generado correctamente: Report.pdf");

            System.out.println("====================================");
            System.out.println("FASE DE GRUPOS - TABLAS DE POSICIONES");
            System.out.println("====================================");

            int standingsTeamWidth = getStandingsTeamWidth(groups);
            for (Group group : groups) {
                printStandings(group, standingsTeamWidth);
            }

            System.out.println("\n====================================");
            System.out.println("GOLEADORES");
            System.out.println("====================================");
            printTopScorers(groups);

            System.out.println("\n====================================");
            System.out.println("PARTIDOS JUGADOS");
            System.out.println("====================================");
            printMatchesByGroup(groups);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static int getStandingsTeamWidth(List<Group> groups) {
        int teamWidth = "EQUIPO".length();
        for (Group group : groups) {
            for (Team team : group.getTeams()) {
                teamWidth = Math.max(teamWidth, team.getName().length());
            }
        }
        return teamWidth;
    }

    private static void printStandings(Group group, int teamWidth) {
        List<Standing> standings = group.getStandings();

        System.out.println("\n" + group.getName());
        System.out.printf("%-4s | %-" + teamWidth + "s | %-4s | %-3s | %-3s | %-3s | %-3s | %-4s | %-4s | %-4s%n",
            "POS", "EQUIPO", "PTS", "PJ", "PG", "PE", "PP", "GF", "GC", "DIF");
        System.out.println("-".repeat(7 + teamWidth + 59));

        for (int i = 0; i < standings.size(); i++) {
            Standing standing = standings.get(i);
            int wins = standing.getWon();
            int draws = standing.getDrawn();
            int losses = standing.getLost();

            System.out.printf("%-4d | %-" + teamWidth + "s | %-4d | %-3d | %-3d | %-3d | %-3d | %-4d | %-4d | %-4d%n",
                i + 1,
                standing.getTeam().getName(),
                standing.getPoints(),
                standing.getPlayed(),
                wins,
                draws,
                losses,
                standing.getScored(),
                standing.getConceded(),
                standing.getGoalDifference());
        }
    }

    private static void printTopScorers(List<Group> groups) {
        List<ScorerEntry> scorers = new ArrayList<>();

        for (Group group : groups) {
            for (Team team : group.getTeams()) {
                for (Player player : team.getSquad()) {
                    if (player.getGoals() > 0) {
                        scorers.add(new ScorerEntry(player, team));
                    }
                }
            }
        }

        scorers.sort(Comparator.comparingInt(ScorerEntry::goals).reversed()
                .thenComparing(entry -> entry.player().getName().toLowerCase()));

        System.out.printf("%-4s | %-4s | %-20s | %-18s%n", "POS", "GOL", "JUGADOR", "EQUIPO");
        System.out.println("-------------------------------------------------------------------");

        for (int i = 0; i < scorers.size(); i++) {
            ScorerEntry entry = scorers.get(i);
            String fullName = entry.player().getName();
            String[] parts = fullName.trim().split("\\s+");
            String nombre = parts.length > 0 ? parts[0] : fullName;
            String apellido = parts.length > 1 ? parts[parts.length - 1] : "";
            String jugador = nombre + (apellido.isBlank() ? "" : " " + apellido);

            System.out.printf("%-4d | %-4d | %-20s | %-18s%n",
                i + 1,
                    entry.goals(),
                jugador,
                    entry.team().getName());
        }
    }

    private static void printMatchesByGroup(List<Group> groups) {
        for (Group group : groups) {
            System.out.println("\n" + group.getName());
            LocalDate lastDate = null;
            int matchday = 0;
            for (GroupMatch match : group.getMatches()) {
                if (!match.getMatchDate().equals(lastDate)) {
                    matchday++;
                    System.out.println("\nFECHA " + matchday);
                    lastDate = match.getMatchDate();
                }
                System.out.printf("%s %d - %d %s%n",
                        match.getHomeTeam().getName(),
                        match.getHomeGoals(),
                        match.getAwayGoals(),
                        match.getAwayTeam().getName());
                System.out.println("   Formaciones: "
                    + match.getHomeFormation().getLabel() + " - "
                    + match.getAwayFormation().getLabel());
                System.out.println("   Estadio: " + match.getStadium().getName());
                System.out.println("   Arbitro: " + match.getReferee().getName());
                System.out.println("   DT local: " + match.getHomeTeam().getCoach().getName());
                System.out.println("   DT visitante: " + match.getAwayTeam().getCoach().getName());

                for (Event event : match.getEvents()) {
                    System.out.println("   - " + event.getDescription());
                }

                if (match.getEvents().isEmpty()) {
                    System.out.println("   - Partido no registrado");
                }
            }
        }
    }

    private record ScorerEntry(Player player, Team team) {
        public int goals() {
            return player.getGoals();
        }
    }
}
