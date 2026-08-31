package model.match;

import model.participant.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Lineup {
    private List<Player> starters = new ArrayList<>();
    private List<Player> subs = new ArrayList<>();
    private static final int MAX_GOALKEEPERS = 1;
    private static final int MAX_DEFENDERS = 4;
    private static final int MAX_MIDFIELDERS = 3;
    private static final int MAX_FORWARDS = 3;

    public Lineup(Team team){
        List<Player> availablePlayers = new ArrayList<>();

        for(Player p : team.getSquad()){
            if(p.isAvailable()){ availablePlayers.add(p); }
        }
        availablePlayers.sort(Comparator.comparing(Player::getOverall).reversed());

        int g=0,d=0,m=0,f=0;
        for(Player p : availablePlayers){       //433 de base : CONSULTAR A LOS PROFES
                                                //OPCION 1: AÑADIR COMO ATRIBUTO tactic A LOS COACH
                                                //OPCION 2: AÑADIR COMO ATRIBUTO tactic A LOS TEAM
                                                //OPCION 3: DEJAR TODAS LAS FORMACIONES 433 POR DEFECTO
            if(p.getPosition()==Position.GOALKEEPER && g<MAX_GOALKEEPERS){
                starters.add(p);
                g++;
            }else if(p.getPosition()==Position.DEFENDER && d<MAX_DEFENDERS){
                starters.add(p);
                d++;
            }else if(p.getPosition()==Position.MIDFIELDER && m<MAX_MIDFIELDERS){
                starters.add(p);
                m++;
            }else if(p.getPosition()==Position.FORWARD && f<MAX_FORWARDS){
                starters.add(p);
                f++;
            }else{
                subs.add(p);
            }
        }
        //si por alguna razon no se completa el 11, se rellena con jugadores del banco
        while (starters.size() < 11 && !subs.isEmpty()) {
            starters.add(subs.removeFirst());
        }
    }

    public List<Player> getStarters() {
        return starters;
    }

    public List<Player> getSubs() {
        return subs;
    }
}
