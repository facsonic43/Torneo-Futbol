package model.tournament;

import control.MatchSimulator;
import model.match.FinalMatch;
import model.match.FirstLegMatch;
import model.match.GroupMatch;
import model.match.Match;
import model.match.SecondLegMatch;
import model.match.Stadium;
import model.participant.Referee;
import model.participant.Team;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Mantiene los participantes, las fases, los partidos y el campeón del campeonato.
// Delega el armado del calendario y coordina su simulación; conserva el estado necesario
// para continuar una partida guardada y reconocer la etapa de cada encuentro.

public class Tournament implements Serializable {
    private static final long serialVersionUID = -3149784396393458075L;
    private List<Team> teams = new ArrayList<>();
    private List<Referee> referees = new ArrayList<>();
    private List<Group> groups = new ArrayList<>();

    private List<FirstLegMatch> quarterFinalFirstLegs = new ArrayList<>();
    private List<SecondLegMatch> quarterFinalSecondLegs = new ArrayList<>();

    private List<FirstLegMatch> semiFinalFirstLegs = new ArrayList<>();
    private List<SecondLegMatch> semiFinalSecondLegs = new ArrayList<>();

    private FinalMatch finalMatch;
    private Team champion;

    private List<Stadium> knockoutStadiums = new ArrayList<>();
    private int knockoutStadiumIndex = 0;
    private int refereeIndex = 0;

    public Tournament() {
    }

    public Tournament(List<Team> teams, List<Referee> referees) {
        this.teams = new ArrayList<>(teams);
        this.referees = new ArrayList<>(referees);
    }

    public List<Group> drawGroups() {
        groups = new TournamentFixtureBuilder(this).drawGroups();
        return groups;
    }

    public void generateGroupMatches(List<Stadium> stadiums) {
        new TournamentFixtureBuilder(this).generateGroupMatches(stadiums);
    }

    public void generateQuarterFinals(List<Stadium> stadiums) {
        new TournamentFixtureBuilder(this).generateQuarterFinals(stadiums);
    }

    public void generateSemiFinals() {
        new TournamentFixtureBuilder(this).generateSemiFinals();
    }

    public void generateFinal() {
        finalMatch = new TournamentFixtureBuilder(this).generateFinal();
    }

    public void simulateGroupStage(MatchSimulator simulator) {
        for (Group group : groups) {
            for (GroupMatch match : group.getMatches()) {
                simulator.simulateMatch( match );
            }
        }
    }

    public void simulateQuarterFinals(MatchSimulator simulator) {
        simulateTwoLegRound( quarterFinalFirstLegs, quarterFinalSecondLegs, simulator );
    }

    public void simulateSemiFinals(MatchSimulator simulator) {
        simulateTwoLegRound( semiFinalFirstLegs, semiFinalSecondLegs, simulator );
    }

    public void simulateFinal(MatchSimulator simulator) {
        if (finalMatch == null) {
            throw new IllegalStateException( "The final must be generated first." );
        }
        simulator.simulateMatch( finalMatch );
        champion = finalMatch.getWinner();
    }

    private void simulateTwoLegRound(List<FirstLegMatch> firstLegMatches, List<SecondLegMatch> secondLegMatches,
            MatchSimulator simulator) {
        if (firstLegMatches.size() != secondLegMatches.size()) {
            throw new IllegalStateException( "Invalid two-leg round configuration." );
        }
        for (int i = 0; i < firstLegMatches.size(); i++) {
            simulator.simulateMatch( firstLegMatches.get(i) );
            simulator.simulateMatch( secondLegMatches.get(i) );
        }
    }

    Referee selectReferee(Team home, Team away) {
        if (referees.isEmpty()) {
            throw new IllegalStateException( "No referees are available." );
        }
        List<Referee> eligibleReferees = new ArrayList<>();
        for (Referee referee : referees) {
            if (referee.canOfficiate(home, away)) {
                eligibleReferees.add( referee );
            }
        }
        if (eligibleReferees.isEmpty()) {
            throw new IllegalStateException("No eligible referee for " + home.getName() + " vs " + away.getName());
        }
        Referee selected = eligibleReferees.get( refereeIndex % eligibleReferees.size() );
        refereeIndex++;
        return selected;
    }

    void prepareKnockoutStadiums(List<Stadium> stadiums) {
        if (stadiums == null || stadiums.size() < 13) {
            throw new IllegalStateException( "At least 13 stadiums are required for the knockout stage without repetitions." );
        }
        knockoutStadiums = new ArrayList<>(stadiums);
        Collections.shuffle( knockoutStadiums );
        knockoutStadiumIndex = 0;
    }

    Stadium nextKnockoutStadium() {
        if (knockoutStadiumIndex >= knockoutStadiums.size()) {
            throw new IllegalStateException( "There are no unused stadiums available." );
        }
        Stadium stadium = knockoutStadiums.get( knockoutStadiumIndex );
        knockoutStadiumIndex++;
        return stadium;
    }

    public List<Match> getAllMatches() {
        List<Match> allMatches = new ArrayList<>();
        for (Group group : groups) {
            allMatches.addAll( group.getMatches() );
        }
        allMatches.addAll( quarterFinalFirstLegs );
        allMatches.addAll( quarterFinalSecondLegs );
        allMatches.addAll( semiFinalFirstLegs );
        allMatches.addAll( semiFinalSecondLegs );
        if (finalMatch != null) {
            allMatches.add(finalMatch);
        }
        return allMatches;
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

    public List<FirstLegMatch> getQuarterFinalFirstLegs() {
        return quarterFinalFirstLegs;
    }

    public List<SecondLegMatch> getQuarterFinalSecondLegs() {
        return quarterFinalSecondLegs;
    }

    public List<FirstLegMatch> getSemiFinalFirstLegs() {
        return semiFinalFirstLegs;
    }

    public List<SecondLegMatch> getSemiFinalSecondLegs() {
        return semiFinalSecondLegs;
    }

    public FinalMatch getFinalMatch() {
        return finalMatch;
    }

    public Team getChampion() {
        return champion;
    }

    public TournamentStage getStageOf(Match match) {
        if (match == getFinalMatch()) {
            return TournamentStage.FINAL;
        }
        if (contains(getSemiFinalFirstLegs(), match) || contains(getSemiFinalSecondLegs(), match)) {
            return TournamentStage.SEMI_FINALS;
        }
        if (contains(getQuarterFinalFirstLegs(), match) || contains(getQuarterFinalSecondLegs(), match)) {
            return TournamentStage.QUARTER_FINALS;
        }
        return TournamentStage.GROUP_STAGE;
    }

    private static boolean contains(List<? extends Match> matches, Match match) {
        return matches != null && matches.contains(match);
    }
}
