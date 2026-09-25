package model.match;

import model.participant.Player;
import model.participant.Position;
import model.participant.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/*
 * Construye los once titulares y los suplentes disponibles de un equipo.
 * Intenta respetar la formación elegida por el técnico y, si las bajas impiden
 * completarla exactamente, utiliza la formación más cercana y jugadores fuera de posición.
 */
public class Lineup {
    private List<Player> starters = new ArrayList<>();
    private List<Player> subs = new ArrayList<>();
    private Formation formation;

    public Lineup(Team team, Team opponent) {
    private final Formation formation;

    public Lineup(Team team, Team opponent){
        List<Player> availablePlayers = new ArrayList<>();

        for (Player player : team.getSquad()) {
            if (player.isAvailable()) {
                availablePlayers.add(player);
            }
        }

        availablePlayers.sort(Comparator.comparing(Player::getOverall).reversed());

        if (availablePlayers.size() < 11) {
            throw new IllegalStateException(
                    "Team " + team.getName() + " has fewer than 11 available players."
            );
        }

        Formation preferredFormation = team.getCoach().chooseFormation(team, opponent);
        formation = findCompatibleFormation(preferredFormation, availablePlayers);

        if (formation != null) {
            buildExactLineup(availablePlayers);
        } else {
            formation = findClosestFormation(preferredFormation, availablePlayers);
            buildEmergencyLineup(availablePlayers);
        }
    }

    private void buildExactLineup(List<Player> availablePlayers) {
        int goalkeepers = 0;
        int defenders = 0;
        int midfielders = 0;
        int forwards = 0;

        for (Player player : availablePlayers) {
            if (player.getPosition() == Position.GOALKEEPER && goalkeepers < 1) {
                starters.add(player);
                goalkeepers++;
            } else if (player.getPosition() == Position.DEFENDER && defenders < formation.getDefenders()) {
                starters.add(player);
                defenders++;
            } else if (player.getPosition() == Position.MIDFIELDER && midfielders < formation.getMidfielders()) {
                starters.add(player);
                midfielders++;
            } else if (player.getPosition() == Position.FORWARD && forwards < formation.getForwards()) {
                starters.add(player);
                forwards++;
            } else {
                subs.add(player);
            }
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

        return goalkeepers >= 1
                && defenders >= formation.getDefenders()
                && midfielders >= formation.getMidfielders()
                && forwards >= formation.getForwards();
    }

    // Si faltan jugadores de una posición, completa el once con los mejores disponibles.
    private void buildEmergencyLineup(List<Player> availablePlayers) {
        Player goalkeeper = null;

        for (Player player : availablePlayers) {
            if (goalkeeper == null && player.getPosition() == Position.GOALKEEPER) {
                goalkeeper = player;
            }
        }

        // Si los dos arqueros están sancionados o lesionados, un jugador de campo ocupa
        // el puesto de manera excepcional para que el torneo pueda continuar.
        if (goalkeeper == null) {
            goalkeeper = availablePlayers.get(0);
        }

        starters.add(goalkeeper);

        addPlayersByPosition(availablePlayers, Position.DEFENDER, formation.getDefenders());
        addPlayersByPosition(availablePlayers, Position.MIDFIELDER, formation.getMidfielders());
        addPlayersByPosition(availablePlayers, Position.FORWARD, formation.getForwards());

        for (Player player : availablePlayers) {
            if (starters.size() < 11
                    && !starters.contains(player)
                    && player.getPosition() != Position.GOALKEEPER) {
                starters.add(player);
            }
        }

        for (Player player : availablePlayers) {
            if (!starters.contains(player)) {
                subs.add(player);
            }
        }

        if (starters.size() < 11) {
            throw new IllegalStateException("The team cannot complete an eleven-player lineup.");
        }
    }

    private void addPlayersByPosition(List<Player> availablePlayers, Position position, int amount) {
        int added = 0;

        for (Player player : availablePlayers) {
            if (added < amount
                    && player.getPosition() == position
                    && !starters.contains(player)) {
                starters.add(player);
                added++;
            }
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

        return null;
    }

    // Busca la formación que necesite la menor cantidad posible de jugadores fuera de posición.
    private Formation findClosestFormation(Formation preferredFormation, List<Player> availablePlayers) {
        Formation bestFormation = preferredFormation;
        int bestShortage = getFormationShortage(preferredFormation, availablePlayers);

        for (Formation alternative : Formation.values()) {
            int shortage = getFormationShortage(alternative, availablePlayers);

            if (shortage < bestShortage) {
                bestShortage = shortage;
                bestFormation = alternative;
            }
        }

        return bestFormation;
    }

    private int getFormationShortage(Formation formation, List<Player> availablePlayers) {
        int defenders = countPosition(availablePlayers, Position.DEFENDER);
        int midfielders = countPosition(availablePlayers, Position.MIDFIELDER);
        int forwards = countPosition(availablePlayers, Position.FORWARD);

        int shortage = 0;
        shortage += Math.max(0, formation.getDefenders() - defenders);
        shortage += Math.max(0, formation.getMidfielders() - midfielders);
        shortage += Math.max(0, formation.getForwards() - forwards);

        return shortage;
    }

    private int countPosition(List<Player> availablePlayers, Position position) {
        int count = 0;

        for (Player player : availablePlayers) {
            if (player.getPosition() == position) {
                count++;
            }
        }

        return count;
    }

    private boolean canComplete(Formation formation, List<Player> availablePlayers) {
        int goalkeepers = countPosition(availablePlayers, Position.GOALKEEPER);
        int defenders = countPosition(availablePlayers, Position.DEFENDER);
        int midfielders = countPosition(availablePlayers, Position.MIDFIELDER);
        int forwards = countPosition(availablePlayers, Position.FORWARD);

        return goalkeepers >= 1
                && defenders >= formation.getDefenders()
                && midfielders >= formation.getMidfielders()
                && forwards >= formation.getForwards();
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
}
