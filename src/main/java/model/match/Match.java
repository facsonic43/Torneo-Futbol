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
 * Centraliza los criterios comunes y las derrotas administrativas por 3-0 sin goles individuales.
 * También reconstruye incidencias, jugadores en cancha y arquero para las tandas de penales.
 */
public abstract class Match implements Serializable {
    private static final long serialVersionUID = -606546183674919432L;
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
    private boolean forfeited = false;
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

    public final String getResolutionCriteria() {
        return getResolutionCriterion().name();
    }

    public ResolutionCriterion getResolutionCriterion() {
        if (!played) {
            return ResolutionCriterion.NOT_PLAYED;
        }
        if (forfeited) {
            return ResolutionCriterion.FORFEIT;
        }
        return homeGoals == awayGoals ? ResolutionCriterion.DRAW : ResolutionCriterion.REGULAR_TIME;
    }

    protected Team getPenaltyWinner() {
        if (homePenalties == null || awayPenalties == null || homePenalties < 0 || awayPenalties < 0
                || homePenalties.equals(awayPenalties)) {
            return null;
        }
        return homePenalties > awayPenalties ? homeTeam : awayTeam;
    }

    public boolean isForfeited() {
        return forfeited;
    }

    public void forfeit(Team losingTeam) {
        if (played) {
            throw new IllegalStateException("This match has already been played.");
        }
        if (losingTeam != homeTeam && losingTeam != awayTeam) {
            throw new IllegalArgumentException("The losing team must belong to the match.");
        }
        homeGoals = losingTeam == homeTeam ? 0 : 3;
        awayGoals = losingTeam == awayTeam ? 0 : 3;
        homePenalties = null;
        awayPenalties = null;
        forfeited = true;
        played = true;
    }

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

    public boolean hasRecordedStartingPlayers() {
        return !homeStarters.isEmpty() || !awayStarters.isEmpty();
    }

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

    public List<PenaltyTaken> getPenaltyKicks(Team team) {
        List<PenaltyTaken> penalties = new ArrayList<>();

        for (PenaltyTaken penalty : getPenaltyKicks()) {
            if (penalty.getTeam() == team) {
                penalties.add(penalty);
            }
        }

        return penalties;
    }

    public List<Player> getPlayersOnPitchAtEnd(Team team) {
        List<Player> players = new ArrayList<>();

        for (PlayerParticipation participation : getPlayerParticipations()) {
            if (participation.getTeam() == team && participation.getExitMinute() >= 90) {
                players.add(participation.getPlayer());
            }
        }

        if (players.isEmpty()) {
            if (team == getHomeTeam()) {
                players.addAll(getHomeStarters());
            } else {
                players.addAll(getAwayStarters());
            }
        }

        return players;
    }

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
