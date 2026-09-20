package report;

import model.match.FirstLegMatch;
import model.match.SecondLegMatch;
import model.participant.Team;
import model.tournament.Group;
import model.tournament.Tournament;

import java.util.List;

/*
 * Genera los reportes relacionados con la estructura del campeonato.
 * Actualmente se encarga del cuadro completo desde los clasificados
 * de grupos hasta el campeón. report 5
 */
public class TournamentReportService {

    public String generateChampionshipBracket(
            Tournament tournament) {

        StringBuilder report =
                new StringBuilder();

        report.append(
                "\n====================================\n"
        );

        report.append(
                "REPORT V - CHAMPIONSHIP BRACKET\n"
        );

        report.append(
                "====================================\n"
        );

        report.append(
                "\nQUALIFIED TEAMS\n"
        );

        for (Group group :
                tournament.getGroups()) {

            appendQualifiedTeams(
                    report,
                    group
            );
        }

        report.append(
                "\nQUARTER-FINALS\n"
        );

        appendTwoLegRound(
                report,
                tournament
                        .getQuarterFinalFirstLegs(),
                tournament
                        .getQuarterFinalSecondLegs()
        );

        report.append(
                "\nSEMI-FINALS\n"
        );

        appendTwoLegRound(
                report,
                tournament
                        .getSemiFinalFirstLegs(),
                tournament
                        .getSemiFinalSecondLegs()
        );

        appendFinal(
                report,
                tournament
        );

        return report.toString();
    }

    private void appendQualifiedTeams(
            StringBuilder report,
            Group group) {

        List<Team> qualified =
                group.getQualifiedTeams();

        report.append(
                group.getName()
                        + ": "
        );

        if (qualified.size() >= 2) {

            report.append(
                    "1st "
                            + qualified.get(0)
                            .getName()
                            + " | 2nd "
                            + qualified.get(1)
                            .getName()
                            + "\n"
            );

        } else {

            report.append(
                    "Not defined\n"
            );
        }
    }

    private void appendTwoLegRound(
            StringBuilder report,
            List<FirstLegMatch> firstLegMatches,
            List<SecondLegMatch> secondLegMatches) {

        if (firstLegMatches.isEmpty()) {

            report.append(
                    "Not generated.\n"
            );

            return;
        }

        for (int i = 0;
             i < firstLegMatches.size();
             i++) {

            FirstLegMatch firstLeg =
                    firstLegMatches.get(i);

            SecondLegMatch secondLeg =
                    secondLegMatches.get(i);

            report.append(
                    "\nSeries "
                            + (i + 1)
                            + "\n"
            );

            report.append(
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
                            + "\n"
            );

            report.append(
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
                            + "\n"
            );

            if (secondLeg.getHomePenalties()
                    != null) {

                report.append(
                        "Penalties: "
                                + secondLeg.getHomePenalties()
                                + " - "
                                + secondLeg.getAwayPenalties()
                                + "\n"
                );
            }

            if (secondLeg.getWinner()
                    != null) {

                report.append(
                        "Winner: "
                                + secondLeg.getWinner()
                                .getName()
                                + "\n"
                );
            }

            report.append(
                    "Resolution: "
                            + secondLeg
                            .getResolutionCriteria()
                            + "\n"
            );
        }
    }

    private void appendFinal(
            StringBuilder report,
            Tournament tournament) {

        report.append(
                "\nFINAL\n"
        );

        if (tournament.getFinalMatch()
                == null) {

            report.append(
                    "Not generated.\n"
            );

            return;
        }

        report.append(
                tournament.getFinalMatch()
                        .getHomeTeam()
                        .getName()
                        + " "
                        + tournament.getFinalMatch()
                        .getHomeGoals()
                        + " - "
                        + tournament.getFinalMatch()
                        .getAwayGoals()
                        + " "
                        + tournament.getFinalMatch()
                        .getAwayTeam()
                        .getName()
                        + "\n"
        );

        if (tournament.getFinalMatch()
                .getHomePenalties()
                != null) {

            report.append(
                    "Penalties: "
                            + tournament.getFinalMatch()
                            .getHomePenalties()
                            + " - "
                            + tournament.getFinalMatch()
                            .getAwayPenalties()
                            + "\n"
            );
        }

        if (tournament.getFinalMatch()
                .getWinner()
                != null) {

            report.append(
                    "Winner: "
                            + tournament.getFinalMatch()
                            .getWinner()
                            .getName()
                            + "\n"
            );
        }

        report.append(
                "\nCHAMPION\n"
        );

        if (tournament.getChampion()
                != null) {

            report.append(
                    tournament.getChampion()
                            .getName()
                            + "\n"
            );

        } else {

            report.append(
                    "Not defined.\n"
            );
        }
    }
}