package run;

import PDF.PDFGenerator;
import main.loader.TournamentData;
import main.loader.TournamentDataLoader;
import model.participant.Team;

public class RunChampionship {

    public static void main(String[] args) {
        TournamentDataLoader loader = new TournamentDataLoader();

        try {
            TournamentData data = loader.load("torneo.json");

            System.out.println("Tournament data loaded successfully.");
            System.out.println("Teams: " + data.getTeams().size());
            System.out.println("Referees: " + data.getReferees().size());

            int totalPlayers = 0;

            for (Team team : data.getTeams()) {
                totalPlayers += team.getSquad().size();

                System.out.printf(
                        "%s | %s | Players: %d | Overall: %.2f%n",
                        team.getName(),
                        team.getCountry(),
                        team.getSquad().size(),
                        team.getOverall()
                );
            }

            System.out.println("Total players: " + totalPlayers);

            PDFGenerator pdf=new PDFGenerator(data);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}