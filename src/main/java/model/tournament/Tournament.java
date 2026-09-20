package model.tournament;

import control.MatchSimulator;
import model.match.GroupMatch;
import model.match.Stadium;
import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;
import java.util.*;

public class Tournament {
    private List<Team> teams;
    private List<Referee> referees;
    private List<Group> groups;

    public List<Group> drawGroups(List<Team> teams) {
        List<Group> groups = new ArrayList<>();
        teams.sort(Comparator.comparingInt(Team::getRanking));

        List<Team> pot1 = new ArrayList<>();
        List<Team> pot2 = new ArrayList<>();
        List<Team> pot3 = new ArrayList<>();
        List<Team> pot4 = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            pot1.add(teams.get(i));
            pot2.add(teams.get(i + 4));
            pot3.add(teams.get(i + 8));
            pot4.add(teams.get(i + 12));
        }

        Collections.shuffle(pot1);
        Collections.shuffle(pot2);
        Collections.shuffle(pot3);
        Collections.shuffle(pot4);

        groups.add(new Group("Group A", new ArrayList<>(List.of(pot1.get(0), pot2.get(0), pot3.get(0), pot4.get(0)))));
        groups.add(new Group("Group B", new ArrayList<>(List.of(pot1.get(1), pot2.get(1), pot3.get(1), pot4.get(1)))));
        groups.add(new Group("Group C", new ArrayList<>(List.of(pot1.get(2), pot2.get(2), pot3.get(2), pot4.get(2)))));
        groups.add(new Group("Group D", new ArrayList<>(List.of(pot1.get(3), pot2.get(3), pot3.get(3), pot4.get(3)))));

        return groups;
    }

    public void generateGroupMatches(List<Group> groups, List<Referee> referees) {
        generateGroupMatches(groups, referees, Collections.emptyList());
    }

    public void generateGroupMatches(List<Group> groups, List<Referee> referees, List<Stadium> stadiums) {
        int stadiumIndex = 0;
        int refereeIndex = 0;
        LocalDate firstMatchDate = LocalDate.now();
        for (Group group : groups) {
            List<Team> teams = group.getTeams();
            int[][] pairingsByRound = {
                    {0, 1, 2, 3},
                    {0, 2, 1, 3},
                    {0, 3, 1, 2}
            };

            for (int round = 0; round < pairingsByRound.length; round++) {
                int[] pairings = pairingsByRound[round];
                for (int pairing = 0; pairing < pairings.length; pairing += 2) {
                    Team home = teams.get(pairings[pairing]);
                    Team away = teams.get(pairings[pairing + 1]);
                    Referee referee = selectReferee(home, away, referees, refereeIndex);
                    refereeIndex++;
                    Stadium stadium;
                    if (stadiums.isEmpty()) {
                        stadium = new Stadium("Estadio " + home.getName(), 1);
                    } else {
                        stadium = stadiums.get(stadiumIndex % stadiums.size());
                        stadiumIndex++;
                    }
                    GroupMatch match = new GroupMatch(home, away, referee, stadium,
                            firstMatchDate.plusDays(round));
                    group.addMatch(match);
                }
            }
        }
    }

    public void simulateGroupStage(List<Group> groups, MatchSimulator simulator) {
        for (Group group : groups) {
            for (GroupMatch match : group.getMatches()) {
                simulator.simulateMatch(match);
            }
        }
    }

    private Referee selectReferee(Team home, Team away, List<Referee> referees, int startIndex) {
        if (referees.isEmpty()) {
            return null;
        }

        List<Referee> eligibleReferees = new ArrayList<>();
        for (Referee referee : referees) {
            if (referee.canOfficiate(home, away)) {
                eligibleReferees.add(referee);
            }
        }

        if (eligibleReferees.isEmpty()) {
            return referees.get(startIndex % referees.size());
        }

        return eligibleReferees.get(startIndex % eligibleReferees.size());
    }
}

