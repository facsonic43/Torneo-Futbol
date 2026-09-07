package model.tournament;

import model.participant.Person;
import model.participant.Player;
import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;
import java.util.*;

public class Tournament {
    private List<Team> teams;
    private List<Referee> referees;
    private List<Group> groups;


    //sorteo de grupos separandolos por ranking
    public List<Group> drawGroups(List<Team> teams) {
        List<Group> groups = new ArrayList<>();
        teams.sort(Comparator.comparing(Team::getRanking));

        List<Team> pot1 = new ArrayList<>();
        List<Team> pot2 = new ArrayList<>();
        List<Team> pot3 = new ArrayList<>();
        List<Team> pot4 = new ArrayList<>();

        for(int i=0;i<4;i++){
            pot1.add(teams.get(i));
            pot2.add(teams.get(i+4));
            pot3.add(teams.get(i+8));
            pot4.add(teams.get(i+12));
        }
        Collections.shuffle(pot1);
        Collections.shuffle(pot2);
        Collections.shuffle(pot3);
        Collections.shuffle(pot4);

        groups.add(new Group("Group A",new ArrayList<>(List.of(pot1.get(0),pot2.get(0),pot3.get(0),pot4.get(0)))));
        groups.add(new Group("Group B",new ArrayList<>(List.of(pot1.get(1),pot2.get(1),pot3.get(1),pot4.get(1)))));
        groups.add(new Group("Group C",new ArrayList<>(List.of(pot1.get(2),pot2.get(2),pot3.get(2),pot4.get(2)))));
        groups.add(new Group("Group D",new ArrayList<>(List.of(pot1.get(3),pot2.get(3),pot3.get(3),pot4.get(3)))));

        return groups;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public List<Referee> getReferees() {
        return referees;
    }

    public List<Group> getGroups() {
        return groups;
    }
}
