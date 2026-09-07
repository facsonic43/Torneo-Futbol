package PDF;

import main.loader.TournamentData;
import model.participant.Person;
import model.participant.Player;
import model.participant.Referee;
import model.participant.Team;

import java.util.ArrayList;
import java.util.List;

public class People {

    private List<Person> people;

    public People(TournamentData data) {
        this.people =new ArrayList<>();
        List<Person> people=new ArrayList<>();
        for (Referee referee: data.getReferees())
            people.add(referee);
        for (Team team: data.getTeams()) {
            people.add(team.getCoach());
            for (Player player: team.getSquad())
                people.add(player);
        }
    }

    public String getInformation(){
        StringBuilder sb=new StringBuilder();
        for (Person person: people)
            sb.append(person.getInformation()+"\n");
        return sb.toString();
    }
}
