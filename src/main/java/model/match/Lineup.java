package model.match;

import model.participant.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Lineup {
    private List<Player> starters = new ArrayList<>();
    private List<Player> subs = new ArrayList<>();
    private final Formation formation;

    public Lineup(Team team, Team opponent){
        List<Player> availablePlayers = new ArrayList<>();
        List<Player> injuredPlayers = new ArrayList<>();

        for(Player p : team.getSquad()){
            if(p.isAvailable()){
                availablePlayers.add(p);
            }else if(p.isInjured() && !p.isSuspended()){
                injuredPlayers.add(p);
            }
        }

        // Injured players are only called up when the squad could not field any formation
        // without them. Suspended players never play (a sent-off player misses the next match).
        if(!canCompleteAnyFormation(availablePlayers)){
            availablePlayers.addAll(injuredPlayers);
        }

        availablePlayers.sort(Comparator.comparing(Player::getOverall).reversed());

        Formation preferredFormation = team.getCoach().chooseFormation(team, opponent);
        this.formation = findCompatibleFormation(preferredFormation, availablePlayers);

        int g=0,d=0,m=0,f=0;
        for(Player p : availablePlayers){
            if(p.getPosition()==Position.GOALKEEPER && g<1){
                starters.add(p);
                g++;
            }else if(p.getPosition()==Position.DEFENDER && d<formation.getDefenders()){
                starters.add(p);
                d++;
            }else if(p.getPosition()==Position.MIDFIELDER && m<formation.getMidfielders()){
                starters.add(p);
                m++;
            }else if(p.getPosition()==Position.FORWARD && f<formation.getForwards()){
                starters.add(p);
                f++;
            }else{
                subs.add(p);
            }
        }

        if (g == 0 && !subs.isEmpty()) {
            // No goalkeeper available: the best remaining field player goes in goal.
            starters.add(subs.remove(0));
        }

        if (starters.size() < 11) {
            throw new IllegalStateException(
                    "No hay jugadores disponibles para completar la formación "
                            + formation.getLabel() + " de " + team.getName());
        }
    }

    private Formation findCompatibleFormation(Formation preferredFormation, List<Player> availablePlayers) {
        if (canComplete(preferredFormation, availablePlayers)) {
            return preferredFormation;
        }

        for (Formation alternative : Formation.values()) {
            if (canComplete(alternative, availablePlayers)) {
                return alternative;
            }
        }

        throw new IllegalStateException("El plantel no puede completar ninguna formación disponible.");
    }

    private boolean canCompleteAnyFormation(List<Player> availablePlayers) {
        for (Formation formation : Formation.values()) {
            if (canComplete(formation, availablePlayers)) {
                return true;
            }
        }

        return false;
    }

    private boolean canComplete(Formation formation, List<Player> availablePlayers) {
        int goalkeepers = 0;
        int defenders = 0;
        int midfielders = 0;
        int forwards = 0;

        for (Player player : availablePlayers) {
            switch (player.getPosition()) {
                case GOALKEEPER -> goalkeepers++;
                case DEFENDER -> defenders++;
                case MIDFIELDER -> midfielders++;
                case FORWARD -> forwards++;
            }
        }

        int outfieldNeeded = formation.getDefenders()
                + formation.getMidfielders()
                + formation.getForwards();

        boolean outfieldCovered = defenders >= formation.getDefenders()
                && midfielders >= formation.getMidfielders()
                && forwards >= formation.getForwards();

        // Without any goalkeeper, one extra field player has to play in goal.
        boolean goalCovered = goalkeepers >= 1
                || defenders + midfielders + forwards > outfieldNeeded;

        return outfieldCovered && goalCovered;
    }

    public List<Player> getStarters() {
        return starters;
    }

    public List<Player> getSubs() {
        return subs;
    }

    public Formation getFormation() {
        return formation;
    }
}