package run;

import control.MatchSimulator;
import dao.CityDAO;
import dao.StadiumDAO;
import main.loader.TournamentData;
import main.loader.TournamentDataLoader;
import model.match.City;
import model.match.Event;
import model.match.FinalMatch;
import model.match.FirstLegMatch;
import model.match.GroupMatch;
import model.match.SecondLegMatch;
import model.match.Stadium;
import model.participant.Position;
import model.tournament.Group;
import model.tournament.Standing;
import model.tournament.Tournament;
import report.ReportService;

import java.util.List;

/*
 * Es el punto de inicio temporal de la aplicación mientras todavía no existe la interfaz gráfica.
 * Carga los datos, ejecuta el campeonato completo y utiliza ReportService
 * para generar los reportes estadísticos requeridos por el trabajo práctico.
 */
public class RunChampionship {

    public static void main(String[] args) {

        TournamentDataLoader loader =
                new TournamentDataLoader();

        try {
            TournamentData data =
                    loader.load("torneo.json");

            CityDAO cityDAO =
                    new CityDAO();

            StadiumDAO stadiumDAO =
                    new StadiumDAO();

            List<City> cities =
                    cityDAO.findAll();

            List<Stadium> stadiums =
                    stadiumDAO.findAll();

            System.out.println(
                    "\n===================================="
            );

            System.out.println(
                    "DATABASE - CITIES AND STADIUMS"
            );

            System.out.println(
                    "===================================="
            );

            System.out.println(
                    "Cities: "
                            + cities.size()
            );

            System.out.println(
                    "Stadiums: "
                            + stadiums.size()
            );

            Tournament tournament =
                    new Tournament(
                            data.getTeams(),
                            data.getReferees()
                    );

            List<Group> groups =
                    tournament.drawGroups();

            tournament.generateGroupMatches(
                    stadiums
            );

            MatchSimulator simulator =
                    new MatchSimulator();

            simulateGroupStageWithTables(
                    groups,
                    simulator
            );

            System.out.println(
                    "\n===================================="
            );

            System.out.println(
                    "FINAL GROUP STANDINGS"
            );

            System.out.println(
                    "===================================="
            );

            int teamWidth =
                    getStandingsTeamWidth(
                            groups
                    );

            for (Group group : groups) {
                printStandings(
                        group,
                        teamWidth
                );
            }

            tournament.generateQuarterFinals(
                    stadiums
            );

            tournament.simulateQuarterFinals(
                    simulator
            );

            printTwoLegRound(
                    "QUARTER-FINALS",
                    tournament.getQuarterFinalFirstLegs(),
                    tournament.getQuarterFinalSecondLegs()
            );

            tournament.generateSemiFinals();

            tournament.simulateSemiFinals(
                    simulator
            );

            printTwoLegRound(
                    "SEMI-FINALS",
                    tournament.getSemiFinalFirstLegs(),
                    tournament.getSemiFinalSecondLegs()
            );

            tournament.generateFinal();

            tournament.simulateFinal(
                    simulator
            );

            printFinal(
                    tournament.getFinalMatch()
            );

            System.out.println(
                    "\n===================================="
            );

            System.out.println(
                    "CHAMPION"
            );

            System.out.println(
                    "===================================="
            );

            System.out.println(
                    tournament.getChampion()
                            .getName()
            );

            ReportService reportService =
                    new ReportService();

            System.out.println(
                    reportService.generateScorersRanking(
                            tournament.getTeams()
                    )
            );

            System.out.println(
                    reportService.generateFairPlayRanking(
                            tournament.getTeams(),
                            tournament.getAllMatches()
                    )
            );

            System.out.println(
                    reportService.generateMinutesRanking(
                            tournament.getTeams()
                    )
            );

            System.out.println(
                    reportService.generateChampionshipBracket(
                            tournament
                    )
            );

            System.out.println(
                    reportService.generateTeamsReport(
                            tournament
                    )
            );

            System.out.println(
                    reportService.generateRefereesRanking(
                            tournament.getReferees(),
                            tournament.getAllMatches()
                    )
            );

            /*
             * Por ahora mostramos arqueros como ejemplo del Reporte VIII.
             * Más adelante la interfaz gráfica permitirá seleccionar
             * GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD o ALL.
             */
            System.out.println(
                    reportService.generatePlayersReport(
                            tournament.getTeams(),
                            Position.GOALKEEPER
                    )
            );

        } catch (Exception exception) {
            System.out.println(
                    "Error: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    // Simula la fase de grupos mostrando la tabla antes y después de cada partido.
    private static void simulateGroupStageWithTables(
            List<Group> groups,
            MatchSimulator simulator) {

        int teamWidth =
                getStandingsTeamWidth(
                        groups
                );

        for (Group group : groups) {

            System.out.println(
                    "\n===================================="
            );

            System.out.println(
                    group.getName()
                            .toUpperCase()
            );

            System.out.println(
                    "===================================="
            );

            for (GroupMatch match :
                    group.getMatches()) {

                System.out.println(
                        "\nTABLE BEFORE MATCH"
                );

                printStandings(
                        group,
                        teamWidth
                );

                System.out.println(
                        "\nMATCH"
                );

                System.out.println(
                        match.getHomeTeam()
                                .getName()
                                + " vs "
                                + match.getAwayTeam()
                                .getName()
                );

                simulator.simulateMatch(
                        match
                );

                printMatchDetails(
                        match
                );

                System.out.println(
                        "\nTABLE AFTER MATCH"
                );

                printStandings(
                        group,
                        teamWidth
                );
            }
        }
    }

    private static int getStandingsTeamWidth(
            List<Group> groups) {

        int teamWidth =
                "TEAM".length();

        for (Group group : groups) {
            for (model.participant.Team team :
                    group.getTeams()) {

                teamWidth =
                        Math.max(
                                teamWidth,
                                team.getName()
                                        .length()
                        );
            }
        }

        return teamWidth;
    }

    private static void printStandings(
            Group group,
            int teamWidth) {

        List<Standing> standings =
                group.getStandings();

        System.out.println(
                "\n"
                        + group.getName()
        );

        System.out.printf(
                "%-4s | %-"
                        + teamWidth
                        + "s | %-4s | %-3s | %-3s | %-3s | %-3s | %-4s | %-4s | %-4s%n",
                "POS",
                "TEAM",
                "PTS",
                "MP",
                "W",
                "D",
                "L",
                "GF",
                "GA",
                "GD"
        );

        System.out.println(
                "-".repeat(
                        7
                                + teamWidth
                                + 59
                )
        );

        for (int i = 0;
             i < standings.size();
             i++) {

            Standing standing =
                    standings.get(i);

            System.out.printf(
                    "%-4d | %-"
                            + teamWidth
                            + "s | %-4d | %-3d | %-3d | %-3d | %-3d | %-4d | %-4d | %-4d%n",
                    i + 1,
                    standing.getTeam()
                            .getName(),
                    standing.getPoints(),
                    standing.getPlayed(),
                    standing.getWon(),
                    standing.getDrawn(),
                    standing.getLost(),
                    standing.getScored(),
                    standing.getConceded(),
                    standing.getGoalDifference()
            );
        }
    }

    private static void printMatchDetails(
            GroupMatch match) {

        System.out.println(
                match.getHomeTeam()
                        .getName()
                        + " "
                        + match.getHomeGoals()
                        + " - "
                        + match.getAwayGoals()
                        + " "
                        + match.getAwayTeam()
                        .getName()
        );

        System.out.println(
                "Stadium: "
                        + match.getStadium()
                        .getName()
        );

        System.out.println(
                "Referee: "
                        + match.getReferee()
                        .getName()
        );

        System.out.println(
                "Formations: "
                        + match.getHomeFormation()
                        .getLabel()
                        + " / "
                        + match.getAwayFormation()
                        .getLabel()
        );

        for (Event event :
                match.getEvents()) {

            System.out.println(
                    "- "
                            + event.getDescription()
            );
        }
    }

    private static void printTwoLegRound(
            String title,
            List<FirstLegMatch> firstLegMatches,
            List<SecondLegMatch> secondLegMatches) {

        System.out.println(
                "\n===================================="
        );

        System.out.println(
                title
        );

        System.out.println(
                "===================================="
        );

        for (int i = 0;
             i < firstLegMatches.size();
             i++) {

            FirstLegMatch firstLeg =
                    firstLegMatches.get(i);

            SecondLegMatch secondLeg =
                    secondLegMatches.get(i);

            System.out.println(
                    "\nSeries "
                            + (i + 1)
            );

            System.out.println(
                    "First leg: "
                            + firstLeg.getHomeTeam()
                            .getName()
                            + " "
                            + firstLeg.getHomeGoals()
                            + " - "
                            + firstLeg.getAwayGoals()
                            + " "
                            + firstLeg.getAwayTeam()
                            .getName()
            );

            System.out.println(
                    "Second leg: "
                            + secondLeg.getHomeTeam()
                            .getName()
                            + " "
                            + secondLeg.getHomeGoals()
                            + " - "
                            + secondLeg.getAwayGoals()
                            + " "
                            + secondLeg.getAwayTeam()
                            .getName()
            );

            if (secondLeg.getHomePenalties()
                    != null) {

                System.out.println(
                        "Penalties: "
                                + secondLeg.getHomePenalties()
                                + " - "
                                + secondLeg.getAwayPenalties()
                );
            }

            System.out.println(
                    "Winner: "
                            + secondLeg.getWinner()
                            .getName()
            );

            System.out.println(
                    "Resolution: "
                            + secondLeg
                            .getResolutionCriteria()
            );
        }
    }

    private static void printFinal(
            FinalMatch match) {

        System.out.println(
                "\n===================================="
        );

        System.out.println(
                "FINAL"
        );

        System.out.println(
                "===================================="
        );

        System.out.println(
                match.getHomeTeam()
                        .getName()
                        + " "
                        + match.getHomeGoals()
                        + " - "
                        + match.getAwayGoals()
                        + " "
                        + match.getAwayTeam()
                        .getName()
        );

        if (match.getHomePenalties()
                != null) {

            System.out.println(
                    "Penalties: "
                            + match.getHomePenalties()
                            + " - "
                            + match.getAwayPenalties()
            );
        }

        System.out.println(
                "Stadium: "
                        + match.getStadium()
                        .getName()
        );

        System.out.println(
                "Referee: "
                        + match.getReferee()
                        .getName()
        );

        System.out.println(
                "Winner: "
                        + match.getWinner()
                        .getName()
        );
    }
}