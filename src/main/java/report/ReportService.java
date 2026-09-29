package report;

import model.match.Match;
import model.participant.Position;
import model.participant.Referee;
import model.participant.Team;
import model.tournament.Tournament;

import java.util.List;

/*
 * Es el punto de acceso general a los reportes del campeonato.
 * Delega los cálculos estadísticos a StatisticsReportService
 * y el cuadro eliminatorio a TournamentReportService.
 */
public class ReportService {

    private StatisticsReportService statisticsReportService;
    private TournamentReportService tournamentReportService;

    public ReportService() {
        statisticsReportService =
                new StatisticsReportService();

        tournamentReportService =
                new TournamentReportService();
    }

    public String generateScorersRanking(
            List<Team> teams) {

        return statisticsReportService
                .generateScorersRanking(
                        teams
                );
    }

    public String generateFairPlayRanking(
            List<Team> teams,
            List<Match> matches) {

        return statisticsReportService
                .generateFairPlayRanking(
                        teams,
                        matches
                );
    }

    public String generateMinutesRanking(
            List<Team> teams) {

        return statisticsReportService
                .generateMinutesRanking(
                        teams
                );
    }

    public String generateChampionshipBracket(
            Tournament tournament) {

        return tournamentReportService
                .generateChampionshipBracket(
                        tournament
                );
    }

    public String generateTeamsReport(
            Tournament tournament) {

        return statisticsReportService
                .generateTeamsReport(
                        tournament
                );
    }

    public String generateRefereesRanking(
            List<Referee> referees,
            List<Match> matches) {

        return statisticsReportService
                .generateRefereesRanking(
                        referees,
                        matches
                );
    }

    public String generatePlayersReport(
            List<Team> teams) {

        return statisticsReportService
                .generatePlayersReport(
                        teams
                );
    }

    public String generatePlayersReport(
            List<Team> teams,
            Position position) {

        return statisticsReportService
                .generatePlayersReport(
                        teams,
                        position
                );
    }
}