package model.match;

import java.util.Optional;
import model.participant.Player;
import model.participant.PlayerParticipation;
import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;
/*
 * Clase base de todos los partidos del campeonato.
 * Guarda equipos, resultado, árbitro, estadio, formaciones, titulares, incidencias
 * y participaciones de jugadores para que cada tipo de partido resuelva su ganador.
 */
public abstract class Match implements Serializable{
    protected Team homeTeam;
    protected Team awayTeam;
    protected Referee referee;
    protected Stadium stadium;
    protected LocalDate matchDate;
    protected Formation homeFormation;
    protected Formation awayFormation;

    protected int homeGoals = 0;
    protected int awayGoals = 0;
    protected Integer homePenalties = null;
    protected Integer awayPenalties = null;

    protected boolean played = false;
    protected boolean extraTimePlayed = false;
    protected List<Event> events = new ArrayList<>();
    protected List<Player> homeStarters = new ArrayList<>();
    protected List<Player> awayStarters = new ArrayList<>();
    protected List<PlayerParticipation> playerParticipations = new ArrayList<>();

    public Match(Team homeTeam, Team awayTeam, Referee referee, Stadium stadium, LocalDate matchDate) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.referee = referee;
        this.stadium = stadium;
        this.matchDate = matchDate;
    }

    public abstract boolean isKnockout();

    public abstract boolean requiresTieBreak();

    /**
     * Winner decided by the goals of this match alone; {@code null} on a draw or if
     * it has not been played. Knockout subclasses override it to add their own rules.
     */
    public Team getWinner() {
        Team winner = null;

        if (played) {
            if (homeGoals > awayGoals) {
                winner = homeTeam;
            } else if (awayGoals > homeGoals) {
                winner = awayTeam;
            }
        }

        return winner;
    }

    public abstract String getResolutionCriteria();

    public void addEvent(Event event) {
        events.add(event);
    }

    public void setStartingLineups(List<Player> homeStarters, List<Player> awayStarters) {
        this.homeStarters = new ArrayList<>(homeStarters);
        this.awayStarters = new ArrayList<>(awayStarters);
    }

    public void addPlayerParticipation(PlayerParticipation participation) {
        playerParticipations.add(participation);
    }

    public Team getHomeTeam() {
        return homeTeam;
    }

    public Team getAwayTeam() {
        return awayTeam;
    }

    public Referee getReferee() {
        return referee;
    }

    public Stadium getStadium() {
        return stadium;
    }

    public LocalDate getMatchDate() {
        return matchDate;
    }

    public Formation getHomeFormation() {
        return homeFormation;
    }

    public Formation getAwayFormation() {
        return awayFormation;
    }

    public void setFormations(Formation homeFormation, Formation awayFormation) {
        this.homeFormation = homeFormation;
        this.awayFormation = awayFormation;
    }


    public List<Player> getHomeStarters() {
        return homeStarters;
    }

    public List<Player> getAwayStarters() {
        return awayStarters;
    }

    public List<PlayerParticipation> getPlayerParticipations() {
        return playerParticipations;
    }

    public int getHomeGoals() {
        return homeGoals;
    }

    public void setHomeGoals(int homeGoals) {
        this.homeGoals = homeGoals;
    }

    public int getAwayGoals() {
        return awayGoals;
    }

    public void setAwayGoals(int awayGoals) {
        this.awayGoals = awayGoals;
    }

    public Integer getHomePenalties() {
        return homePenalties;
    }

    public void setHomePenalties(Integer homePenalties) {
        this.homePenalties = homePenalties;
    }

    public Integer getAwayPenalties() {
        return awayPenalties;
    }

    public void setAwayPenalties(Integer awayPenalties) {
        this.awayPenalties = awayPenalties;
    }

    public boolean isPlayed() {
        return played;
    }

    public boolean isExtraTimePlayed() {
        return extraTimePlayed;
    }

    public void setExtraTimePlayed(boolean extraTimePlayed) {
        this.extraTimePlayed = extraTimePlayed;
    }

    public void setPlayed(boolean played) {
        this.played = played;
    }

    public List<Event> getEvents() {
        return events;
    }


    // Indica si ya se registraron los titulares de al menos un equipo.
    public boolean hasRecordedStartingPlayers() {
        return !homeStarters.isEmpty() || !awayStarters.isEmpty();
    }

    // Typed views: every subclass answers for itself, so nobody has to ask for its class.

    public Optional<GroupMatch> asGroupMatch() {
        return Optional.empty();
    }

    public Optional<FirstLegMatch> asFirstLeg() {
        return Optional.empty();
    }

    public Optional<SecondLegMatch> asSecondLeg() {
        return Optional.empty();
    }

    public Optional<FinalMatch> asFinal() {
        return Optional.empty();
    }

    // Incidents by type, collected with a visitor.

    public List<Goal> getGoals() {
        return collectEvents().goals;
    }

    public List<YellowCard> getYellowCards() {
        return collectEvents().yellowCards;
    }

    public List<PenaltyTaken> getPenaltyKicks() {
        return collectEvents().penaltyKicks;
    }

    private EventCollector collectEvents() {
        EventCollector collector = new EventCollector();

        for (Event event : events) {
            event.accept(collector);
        }

        return collector;
    }

    private static final class EventCollector implements EventVisitor {
        private final List<Goal> goals = new ArrayList<>();
        private final List<YellowCard> yellowCards = new ArrayList<>();
        private final List<PenaltyTaken> penaltyKicks = new ArrayList<>();

        @Override
        public void visit(Goal goal) {
            goals.add(goal);
        }

        @Override
        public void visit(YellowCard yellowCard) {
            yellowCards.add(yellowCard);
        }

        @Override
        public void visit(PenaltyTaken penaltyTaken) {
            penaltyKicks.add(penaltyTaken);
        }
    }

    /** Penalty kicks of the shootout taken by the given team. */
    public List<PenaltyTaken> getPenaltyKicks(Team team) {
        List<PenaltyTaken> penalties = new ArrayList<>();

        for (PenaltyTaken penalty : getPenaltyKicks()) {
            if (penalty.getTeam() == team) {
                penalties.add(penalty);
            }
        }

        return penalties;
    }

    /** Players of the team who were still on the pitch when the 90 minutes ended. */
    public List<Player> getPlayersOnPitchAtEnd(Team team) {
        List<Player> players = new ArrayList<>();

        for (PlayerParticipation participation : getPlayerParticipations()) {
            if (participation.getTeam() == team && participation.getExitMinute() >= 90) {
                players.add(participation.getPlayer());
            }
        }

        // Fallback for matches without recorded participations: the starting players.
        if (players.isEmpty()) {
            if (team == getHomeTeam()) {
                players.addAll(getHomeStarters());
            } else {
                players.addAll(getAwayStarters());
            }
        }

        return players;
    }

    /**
     * Goalkeeper that finished the match, who defends the shootout. When no goalkeeper is left,
     * the best-rated player on the pitch goes in goal. Null if nobody is on record.
     */
    public Player getShootoutGoalkeeper(Team team) {
        List<Player> players = getPlayersOnPitchAtEnd(team);

        if (players.isEmpty()) {
            return null;
        }

        for (Player player : players) {
            if (player.isGoalkeeper()) {
                return player;
            }
        }

        Player emergencyGoalkeeper = players.get(0);

        for (Player player : players) {
            if (player.getOverall() > emergencyGoalkeeper.getOverall()) {
                emergencyGoalkeeper = player;
            }
        }

        return emergencyGoalkeeper;
    }
}
