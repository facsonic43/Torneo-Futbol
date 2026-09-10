package run;

import control.MatchSimulator;
import main.loader.TournamentData;
import main.loader.TournamentDataLoader;
import model.match.*;
import model.participant.Country;
import model.participant.Player;
import model.participant.Position;
import model.participant.Referee;
import model.participant.Team;
import model.tournament.Group;
import model.tournament.Standing;
import model.tournament.Tournament;

import java.time.LocalDate;
import java.util.*;

public class RunChampionship {

    private static Random random = new Random();
    private static MatchSimulator simulator = new MatchSimulator();

    public static void main(String[] args) {
        simulateFullTournament();
    }

    public static void simulateFullTournament() {
        System.out.println("============================================================================");
        System.out.println("                 INTERNATIONAL FOOTBALL TOURNAMENT SIMULATION               ");
        System.out.println("============================================================================\n");

        // 1. Load Data from JSON
        System.out.println("Loading tournament data from src/main/resources/torneo.json...");
        TournamentDataLoader loader = new TournamentDataLoader();
        TournamentData tournamentData = loader.load("torneo.json");

        List<Team> teams = tournamentData.getTeams();
        List<Referee> referees = tournamentData.getReferees();

        System.out.println("Successfully loaded " + teams.size() + " teams and " + referees.size() + " referees.\n");

        // 2. Draw Groups
        Tournament tournament = new Tournament();
        List<Group> groups = tournament.drawGroups(teams);

        System.out.println("============================================================================");
        System.out.println("                             GROUP STAGE DRAW                               ");
        System.out.println("============================================================================");
        for (Group group : groups) {
            System.out.println("\n>>> " + group.getName().toUpperCase() + ":");
            for (Team t : group.getTeams()) {
                System.out.println("  - " + String.format("%-30s", t.getName()) +
                        " | Country: " + String.format("%-12s", t.getCountry().getName()) +
                        " | Ranking: " + String.format("%-3d", t.getRanking()) +
                        " | Power: " + String.format("%.2f", t.getTeamPower()));
            }
        }
        System.out.println("\n");

        // 3. Simulate Group Stage (Single round: 3 matchdays per team, 6 matches per group)
        simulateGroupStageSingleRound(groups, referees);

        // 4. Knockout Stage: Quarter-Finals
        System.out.println("\n============================================================================");
        System.out.println("                        KNOCKOUT STAGE: QUARTER-FINALS                      ");
        System.out.println("============================================================================");

        List<Team> qfWinners = new ArrayList<>();

        // Group Winners (1st) and Runners-up (2nd)
        Team qf1Home = groups.get(0).getQualifiedTeams().get(0); // 1st Group A
        Team qf1Away = groups.get(1).getQualifiedTeams().get(1); // 2nd Group B

        Team qf2Home = groups.get(1).getQualifiedTeams().get(0); // 1st Group B
        Team qf2Away = groups.get(0).getQualifiedTeams().get(1); // 2nd Group A

        Team qf3Home = groups.get(2).getQualifiedTeams().get(0); // 1st Group C
        Team qf3Away = groups.get(3).getQualifiedTeams().get(1); // 2nd Group D

        Team qf4Home = groups.get(3).getQualifiedTeams().get(0); // 1st Group D
        Team qf4Away = groups.get(2).getQualifiedTeams().get(1); // 2nd Group C

        qfWinners.add(simulateTwoLeggedTie("QUARTER-FINAL 1", qf1Away, qf1Home, referees));
        qfWinners.add(simulateTwoLeggedTie("QUARTER-FINAL 2", qf2Away, qf2Home, referees));
        qfWinners.add(simulateTwoLeggedTie("QUARTER-FINAL 3", qf3Away, qf3Home, referees));
        qfWinners.add(simulateTwoLeggedTie("QUARTER-FINAL 4", qf4Away, qf4Home, referees));

        // 5. Knockout Stage: Semi-Finals
        System.out.println("\n============================================================================");
        System.out.println("                         KNOCKOUT STAGE: SEMI-FINALS                        ");
        System.out.println("============================================================================");

        List<Team> finalTeams = new ArrayList<>();
        finalTeams.add(simulateTwoLeggedTie("SEMI-FINAL 1", qfWinners.get(0), qfWinners.get(1), referees));
        finalTeams.add(simulateTwoLeggedTie("SEMI-FINAL 2", qfWinners.get(2), qfWinners.get(3), referees));

        // 6. Grand Final (Single match in neutral venue)
        System.out.println("\n============================================================================");
        System.out.println("                          GRAND FINAL (NEUTRAL VENUE)                       ");
        System.out.println("============================================================================");

        Team finalHome = finalTeams.get(0);
        Team finalAway = finalTeams.get(1);

        Referee finalReferee = selectReferee(finalHome, finalAway, referees);
        Country neutralCountry = new Country("Neutral Host Nation");
        City finalCity = new City(1, "Metropolis Stadium", neutralCountry.getName());
        Stadium finalStadium = new Stadium(1, "International Arena", finalCity.getId());

        FinalMatch grandFinal = new FinalMatch(finalHome, finalAway, finalReferee, finalStadium, LocalDate.now());

        System.out.println("Match: " + finalHome.getName() + " (" + finalHome.getCountry() + ") vs " +
                finalAway.getName() + " (" + finalAway.getCountry() + ")");
        System.out.println("Stadium: " + finalStadium.getName() + " | Referee: " + finalReferee.getName() + " (" + finalReferee.getNationality() + ")\n");

        Lineup homeLineup = new Lineup(finalHome);
        Lineup awayLineup = new Lineup(finalAway);
        printTeamLineup(finalHome, homeLineup);
        printTeamLineup(finalAway, awayLineup);

        simulator.simulateMatch(grandFinal);

        System.out.println("========================== FINAL MATCH EVENTS ==============================");
        for (Event event : grandFinal.getEvents()) {
            System.out.println(event.getDescription());
        }
        System.out.println("============================================================================\n");

        System.out.println("FINAL SCORE: " + finalHome.getName() + " " + grandFinal.getHomeGoals() + " - " +
                grandFinal.getAwayGoals() + " " + finalAway.getName());

        printMatchScorersSummary(grandFinal);

        if (grandFinal.getHomePenalties() != null && grandFinal.getAwayPenalties() != null) {
            System.out.println("Penalty Shootout: " + finalHome.getName() + " " + grandFinal.getHomePenalties() +
                    " - " + grandFinal.getAwayPenalties() + " " + finalAway.getName());
        }

        System.out.println("Resolution Criteria: " + grandFinal.getResolutionCriteria());
        Team champion = grandFinal.getWinner();
        if (champion != null) {
            System.out.println("\n🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆");
            System.out.println("  CONGRATULATIONS TO THE TOURNAMENT CHAMPION: " + champion.getName().toUpperCase() + " (" + champion.getCountry() + ")!");
            System.out.println("🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆🏆\n");
        }

        // 7. Display Top Scorers and Top Assisters Tables
        printTournamentPlayerStatistics(teams);
    }

    private static void simulateGroupStageSingleRound(List<Group> groups, List<Referee> referees) {
        System.out.println("============================================================================");
        System.out.println("             SIMULATING GROUP STAGE (SINGLE ROUND - 3 MATCHDAYS)           ");
        System.out.println("============================================================================");

        // 3 Matchdays (each team plays 3 matches once)
        // Round 1: 0 vs 1, 2 vs 3
        // Round 2: 0 vs 2, 3 vs 1
        // Round 3: 0 vs 3, 1 vs 2
        int[][][] roundPairs = {
                {{0, 1}, {2, 3}},
                {{0, 2}, {3, 1}},
                {{0, 3}, {1, 2}}
        };

        for (int r = 0; r < 3; r++) {
            System.out.println("\n============================= MATCHDAY " + (r + 1) + " =============================");
            for (Group group : groups) {
                System.out.println("\n[" + group.getName().toUpperCase() + "]");
                List<Team> gTeams = group.getTeams();
                for (int m = 0; m < 2; m++) {
                    Team home = gTeams.get(roundPairs[r][m][0]);
                    Team away = gTeams.get(roundPairs[r][m][1]);
                    Referee ref = selectReferee(home, away, referees);

                    City city = new City(1, "City", home.getCountry().getName());
                    Stadium stadium = new Stadium(1, "Stadium", city.getId());
                    GroupMatch match = new GroupMatch(home, away, ref, stadium, LocalDate.now());

                    simulator.simulateMatch(match);
                    group.addMatch(match);

                    System.out.println(String.format("  %-24s %d - %d %-24s",
                            home.getName(), match.getHomeGoals(), match.getAwayGoals(), away.getName()));
                    printMatchScorersSummary(match);
                }
            }

            // Update player availability (serve suspension & recovery after each matchday)
            for (Group group : groups) {
                for (Team t : group.getTeams()) {
                    for (Player p : t.getSquad()) {
                        p.updateMatchAvailability();
                    }
                }
            }
        }

        // Print final standings for each group
        System.out.println("\n============================================================================");
        System.out.println("                         FINAL GROUP STANDINGS                              ");
        System.out.println("============================================================================");
        for (Group group : groups) {
            System.out.println("\n>>> " + group.getName().toUpperCase() + " STANDINGS:");
            System.out.println(String.format("%-4s %-25s %3s %3s %3s %3s %4s %4s %4s %4s",
                    "POS", "TEAM", "PTS", "MP", "W", "D", "L", "GF", "GA", "GD"));
            System.out.println("----------------------------------------------------------------------------");

            List<Standing> standings = group.getStandings();
            int pos = 1;
            for (Standing s : standings) {
                String prefix = (pos <= 2) ? "[Q] " : "    ";
                System.out.println(String.format("%-4s %-25s %3d %3d %3d %3d %4d %4d %4d %+4d",
                        prefix + pos,
                        s.getTeam().getName(),
                        s.getPoints(),
                        s.getPlayedMatches(),
                        s.getWonMatches(),
                        s.getTiedMatches(),
                        s.getLostMatches(),
                        s.getGoalsScored(),
                        s.getGoalsConceded(),
                        s.getGoalDifference()));
                pos++;
            }
        }
    }

    private static Team simulateTwoLeggedTie(String tieName, Team team1, Team team2, List<Referee> referees) {
        System.out.println("\n----------------------------------------------------------------------------");
        System.out.println(">>> " + tieName + ": " + team1.getName() + " vs " + team2.getName());
        System.out.println("----------------------------------------------------------------------------");

        // First Leg: Team 1 is Home, Team 2 is Away
        Referee ref1 = selectReferee(team1, team2, referees);
        City city1 = new City(1, "City 1", team1.getCountry().getName());
        Stadium stadium1 = new Stadium(1, "Stadium 1", city1.getId());
        FirstLegMatch firstLeg = new FirstLegMatch(team1, team2, ref1, stadium1, LocalDate.now());
        simulator.simulateMatch(firstLeg);

        System.out.println("First Leg:  " + team1.getName() + " " + firstLeg.getHomeGoals() + " - " +
                firstLeg.getAwayGoals() + " " + team2.getName());
        printMatchScorersSummary(firstLeg);

        // Update availability
        for (Player p : team1.getSquad()) {
            p.updateMatchAvailability();
        }
        for (Player p : team2.getSquad()) {
            p.updateMatchAvailability();
        }

        // Second Leg: Team 2 is Home, Team 1 is Away
        Referee ref2 = selectReferee(team2, team1, referees);
        City city2 = new City(2, "City 2", team2.getCountry().getName());
        Stadium stadium2 = new Stadium(2, "Stadium 2", city2.getId());
        SecondLegMatch secondLeg = new SecondLegMatch(team2, team1, ref2, stadium2, LocalDate.now(), firstLeg);
        simulator.simulateMatch(secondLeg);

        System.out.println("Second Leg: " + team2.getName() + " " + secondLeg.getHomeGoals() + " - " +
                secondLeg.getAwayGoals() + " " + team1.getName());
        printMatchScorersSummary(secondLeg);

        if (secondLeg.getHomePenalties() != null && secondLeg.getAwayPenalties() != null) {
            System.out.println("Penalty Shootout: " + team2.getName() + " " + secondLeg.getHomePenalties() +
                    " - " + secondLeg.getAwayPenalties() + " " + team1.getName());
        }

        System.out.println("Resolution: " + secondLeg.getResolutionCriteria());
        Team winner = secondLeg.getWinner();
        System.out.println(">>> WINNER & ADVANCING: " + winner.getName().toUpperCase() + " (" + winner.getCountry() + ")");

        // Update availability
        for (Player p : team1.getSquad()) {
            p.updateMatchAvailability();
        }
        for (Player p : team2.getSquad()) {
            p.updateMatchAvailability();
        }

        return winner;
    }

    private static void printMatchScorersSummary(Match match) {
        List<String> homeGoalsList = new ArrayList<>();
        List<String> awayGoalsList = new ArrayList<>();

        for (Event ev : match.getEvents()) {
            if (ev instanceof Goal) {
                Goal g = (Goal) ev;
                String info = g.getPlayer().getName() + " " + g.getMinute() + "'";
                if (g.getAssistPlayer() != null) {
                    info += " (ast. " + g.getAssistPlayer().getName() + ")";
                }
                if (g.getTeam().equals(match.getHomeTeam())) {
                    homeGoalsList.add(info);
                } else {
                    awayGoalsList.add(info);
                }
            }
        }

        if (!homeGoalsList.isEmpty() || !awayGoalsList.isEmpty()) {
            if (!homeGoalsList.isEmpty()) {
                System.out.println("    \u26BD " + match.getHomeTeam().getName() + ": " + String.join(", ", homeGoalsList));
            }
            if (!awayGoalsList.isEmpty()) {
                System.out.println("    \u26BD " + match.getAwayTeam().getName() + ": " + String.join(", ", awayGoalsList));
            }
        }
    }

    private static Referee selectReferee(Team t1, Team t2, List<Referee> referees) {
        Referee selected = null;
        for (Referee r : referees) {
            if (selected == null && r.canOfficiate(t1, t2)) {
                selected = r;
            }
        }
        if (selected == null && !referees.isEmpty()) {
            selected = referees.get(0);
        }
        return selected;
    }

    private static void printTournamentPlayerStatistics(List<Team> teams) {
        List<Player> allPlayers = new ArrayList<>();
        Map<Player, Team> playerTeamMap = new HashMap<>();

        for (Team t : teams) {
            for (Player p : t.getSquad()) {
                allPlayers.add(p);
                playerTeamMap.put(p, t);
            }
        }

        // Top Scorers
        List<Player> scorers = new ArrayList<>(allPlayers);
        scorers.sort((p1, p2) -> {
            if (p2.getGoals() != p1.getGoals()) {
                return Integer.compare(p2.getGoals(), p1.getGoals());
            }
            return Integer.compare(p1.getMinutesPlayed(), p2.getMinutesPlayed());
        });

        System.out.println("============================================================================");
        System.out.println("                       TOP TOURNAMENT GOALSCORERS                           ");
        System.out.println("============================================================================");
        System.out.println(String.format("%-4s %-25s %-22s %-12s %6s %8s",
                "POS", "PLAYER", "TEAM", "POSITION", "GOALS", "MINUTES"));
        System.out.println("----------------------------------------------------------------------------");
        int count = 0;
        int rank = 1;
        for (Player p : scorers) {
            if (p.getGoals() > 0 && count < 10) {
                Team t = playerTeamMap.get(p);
                System.out.println(String.format("%-4d %-25s %-22s %-12s %6d %8d'",
                        rank++,
                        p.getName(),
                        t.getName(),
                        "[" + p.getPosition() + "]",
                        p.getGoals(),
                        p.getMinutesPlayed()));
                count++;
            }
        }
        if (count == 0) {
            System.out.println("No goals scored in the tournament.");
        }
        System.out.println("============================================================================\n");

        // Top Assisters
        List<Player> assisters = new ArrayList<>(allPlayers);
        assisters.sort((p1, p2) -> {
            if (p2.getAssists() != p1.getAssists()) {
                return Integer.compare(p2.getAssists(), p1.getAssists());
            }
            return Integer.compare(p1.getMinutesPlayed(), p2.getMinutesPlayed());
        });

        System.out.println("============================================================================");
        System.out.println("                       TOP TOURNAMENT ASSISTERS                             ");
        System.out.println("============================================================================");
        System.out.println(String.format("%-4s %-25s %-22s %-12s %8s %8s",
                "POS", "PLAYER", "TEAM", "POSITION", "ASSISTS", "MINUTES"));
        System.out.println("----------------------------------------------------------------------------");
        count = 0;
        rank = 1;
        for (Player p : assisters) {
            if (p.getAssists() > 0 && count < 10) {
                Team t = playerTeamMap.get(p);
                System.out.println(String.format("%-4d %-25s %-22s %-12s %8d %8d'",
                        rank++,
                        p.getName(),
                        t.getName(),
                        "[" + p.getPosition() + "]",
                        p.getAssists(),
                        p.getMinutesPlayed()));
                count++;
            }
        }
        if (count == 0) {
            System.out.println("No assists recorded in the tournament.");
        }
        System.out.println("============================================================================\n");
    }

    private static void printTeamLineup(Team team, Lineup lineup) {
        System.out.println("============================================================================");
        System.out.println(">>> LINEUP: " + team.getName() + " (" + team.getCountry() + ") - Power Rating: " + String.format("%.2f", team.getTeamPower()));
        System.out.println("Head Coach: " + team.getCoach().getName() + " (Titles: " + team.getCoach().getTitlesObtained() + ")");
        System.out.println("----------------------------------------------------------------------------");

        List<Player> sortedStarters = new ArrayList<>(lineup.getStarters());
        sortedStarters.sort(Comparator.comparingInt(RunChampionship::positionOrder));

        List<Player> sortedSubs = new ArrayList<>(lineup.getSubs());
        sortedSubs.sort(Comparator.comparingInt(RunChampionship::positionOrder));

        System.out.println("STARTING 11:");
        for (Player p : sortedStarters) {
            System.out.println("  - " + String.format("%-14s", "[" + p.getPosition() + "]") + " " +
                    String.format("%-25s", p.getName()) + " (Rating: " + (int) p.getOverall() + ")");
        }

        System.out.println("SUBSTITUTES BENCH (7):");
        for (Player p : sortedSubs) {
            System.out.println("  - " + String.format("%-14s", "[" + p.getPosition() + "]") + " " +
                    String.format("%-25s", p.getName()) + " (Rating: " + (int) p.getOverall() + ")");
        }
        System.out.println("============================================================================\n");
    }

    private static int positionOrder(Player p) {
        if (p.getPosition() == Position.GOALKEEPER) {
            return 1;
        }
        if (p.getPosition() == Position.DEFENDER) {
            return 2;
        }
        if (p.getPosition() == Position.MIDFIELDER) {
            return 3;
        }
        return 4; // FORWARD
    }
}
