package model.tournament;

import model.match.FinalMatch;
import model.match.FirstLegMatch;
import model.match.GroupMatch;
import model.match.SecondLegMatch;
import model.match.Stadium;
import model.participant.Referee;
import model.participant.Team;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
 * Construye el fixture de grupos, cuartos, semifinales y final.
 * El estado del torneo (fechas, árbitros, estadios, listas) sigue en Tournament;
 * esta clase solamente concentra las reglas de armado de los partidos.
 */
class TournamentFixtureBuilder {
    private Tournament tournament;

    TournamentFixtureBuilder(Tournament tournament) {
        this.tournament = tournament;
    }

    List<Group> drawGroups() {
        if (tournament.getTeams().size() != 16) {
            throw new IllegalStateException( "The tournament must contain exactly 16 teams." );
        }

        List<Team> orderedTeams = new ArrayList<>(tournament.getTeams());

        orderedTeams.sort( Comparator.comparingInt(Team::getRanking) );

        // Cuatro bombos por ranking: cada grupo recibe un equipo de cada bombo.
        List<List<Team>> pots = new ArrayList<>();

        for (int pot = 0; pot < 4; pot++) {
            List<Team> potTeams = new ArrayList<>( orderedTeams.subList(pot * 4, pot * 4 + 4) );

            Collections.shuffle(potTeams);
            pots.add(potTeams);
        }

        List<Group> groups = new ArrayList<>();

        for (int g = 0; g < 4; g++) {
            List<Team> groupTeams = new ArrayList<>();

            for (List<Team> potTeams : pots) {
                groupTeams.add(potTeams.get(g));
            }

            groups.add( new Group( "Group " + (char) ('A' + g), groupTeams ) );
        }

        return groups;
    }

    void generateGroupMatches(List<Stadium> stadiums) {

        int stadiumIndex = 0;

        LocalDate firstMatchDate = LocalDate.now();

        int[][] pairingsByRound = {
                {0, 1, 2, 3},
                {0, 2, 1, 3},
                {0, 3, 1, 2}
        };

        for (Group group : tournament.getGroups()) {
            List<Team> groupTeams = group.getTeams();

            for (int round = 0;
                 round < pairingsByRound.length;
                 round++) {

                int[] pairings = pairingsByRound[round];

                for (int pairing = 0;
                     pairing < pairings.length;
                     pairing += 2) {

                    Team home = groupTeams.get( pairings[pairing] );

                    Team away = groupTeams.get( pairings[pairing + 1] );

                    Referee referee = tournament.selectReferee( home, away );

                    Stadium stadium;

                    if (stadiums.isEmpty()) {
                        stadium = new Stadium( "Stadium of " + home.getName(), 1 );
                    } else {
                        stadium = stadiums.get( stadiumIndex % stadiums.size() );

                        stadiumIndex++;
                    }

                    GroupMatch match =
                            new GroupMatch(
                                    home,
                                    away,
                                    referee,
                                    stadium,
                                    firstMatchDate.plusDays(round) );

                    group.addMatch(match);
                }
            }
        }
    }

    void generateQuarterFinals(
            List<Stadium> stadiums) {

        if (tournament.getGroups().size() != 4) {
            throw new IllegalStateException( "The four groups must exist before generating quarter-finals." );
        }

        tournament.prepareKnockoutStadiums( stadiums );

        tournament.getQuarterFinalFirstLegs().clear();
        tournament.getQuarterFinalSecondLegs().clear();

        Team a1 = tournament.getGroups().get(0).getQualifiedTeams().get(0);

        Team a2 = tournament.getGroups().get(0).getQualifiedTeams().get(1);

        Team b1 = tournament.getGroups().get(1).getQualifiedTeams().get(0);

        Team b2 = tournament.getGroups().get(1).getQualifiedTeams().get(1);

        Team c1 = tournament.getGroups().get(2).getQualifiedTeams().get(0);

        Team c2 = tournament.getGroups().get(2).getQualifiedTeams().get(1);

        Team d1 = tournament.getGroups().get(3).getQualifiedTeams().get(0);

        Team d2 = tournament.getGroups().get(3).getQualifiedTeams().get(1);

        LocalDate firstLegDate = getLastGroupDate().plusDays(7);

        LocalDate secondLegDate = firstLegDate.plusDays(7);

        addTwoLegSeries(
                a1,
                d2,
                firstLegDate,
                secondLegDate,
                tournament.getQuarterFinalFirstLegs(), tournament.getQuarterFinalSecondLegs() );

        addTwoLegSeries(
                b1,
                c2,
                firstLegDate,
                secondLegDate,
                tournament.getQuarterFinalFirstLegs(), tournament.getQuarterFinalSecondLegs() );

        addTwoLegSeries(
                c1,
                a2,
                firstLegDate,
                secondLegDate,
                tournament.getQuarterFinalFirstLegs(), tournament.getQuarterFinalSecondLegs() );

        addTwoLegSeries(
                d1,
                b2,
                firstLegDate,
                secondLegDate,
                tournament.getQuarterFinalFirstLegs(), tournament.getQuarterFinalSecondLegs() );
    }

    void generateSemiFinals() {
        if (tournament.getQuarterFinalSecondLegs().size() != 4) {
            throw new IllegalStateException( "Quarter-finals must be generated first." );
        }

        Team winner1 = requireWinner( tournament.getQuarterFinalSecondLegs().get(0) );

        Team winner2 = requireWinner( tournament.getQuarterFinalSecondLegs().get(1) );

        Team winner3 = requireWinner( tournament.getQuarterFinalSecondLegs().get(2) );

        Team winner4 = requireWinner( tournament.getQuarterFinalSecondLegs().get(3) );

        tournament.getSemiFinalFirstLegs().clear();
        tournament.getSemiFinalSecondLegs().clear();

        LocalDate firstLegDate = tournament.getQuarterFinalSecondLegs().get(0).getMatchDate().plusDays(7);

        LocalDate secondLegDate = firstLegDate.plusDays(7);

        addTwoLegSeries(
                winner1,
                winner2,
                firstLegDate,
                secondLegDate,
                tournament.getSemiFinalFirstLegs(), tournament.getSemiFinalSecondLegs() );

        addTwoLegSeries(
                winner3,
                winner4,
                firstLegDate,
                secondLegDate,
                tournament.getSemiFinalFirstLegs(), tournament.getSemiFinalSecondLegs() );
    }

    FinalMatch generateFinal() {
        if (tournament.getSemiFinalSecondLegs().size() != 2) {
            throw new IllegalStateException( "Semi-finals must be generated first." );
        }

        Team finalist1 = requireWinner( tournament.getSemiFinalSecondLegs().get(0) );

        Team finalist2 = requireWinner( tournament.getSemiFinalSecondLegs().get(1) );

        LocalDate finalDate = tournament.getSemiFinalSecondLegs().get(0).getMatchDate().plusDays(7);

        return new FinalMatch(
                finalist1,
                finalist2,
                tournament.selectReferee(
                        finalist1,
                        finalist2
                ),
                tournament.nextKnockoutStadium(), finalDate );
    }

    private void addTwoLegSeries(
            Team firstLegHome,
            Team firstLegAway,
            LocalDate firstLegDate,
            LocalDate secondLegDate,
            List<FirstLegMatch> firstLegMatches,
            List<SecondLegMatch> secondLegMatches) {

        FirstLegMatch firstLeg =
                new FirstLegMatch(
                        firstLegHome,
                        firstLegAway,
                        tournament.selectReferee(
                                firstLegHome,
                                firstLegAway
                        ),
                        tournament.nextKnockoutStadium(), firstLegDate );

        SecondLegMatch secondLeg =
                new SecondLegMatch(
                        firstLegAway,
                        firstLegHome,
                        tournament.selectReferee(
                                firstLegAway,
                                firstLegHome
                        ),
                        tournament.nextKnockoutStadium(), secondLegDate, firstLeg );

        firstLegMatches.add( firstLeg );

        secondLegMatches.add( secondLeg );
    }

    private LocalDate getLastGroupDate() {
        LocalDate lastDate = LocalDate.now();

        for (Group group : tournament.getGroups()) {
            for (GroupMatch match :
                    group.getMatches()) {

                if (match.getMatchDate()
                        .isAfter(lastDate)) {

                    lastDate = match.getMatchDate();
                }
            }
        }

        return lastDate;
    }

    private Team requireWinner(
            SecondLegMatch match) {

        Team winner = match.getWinner();

        if (winner == null) {
            throw new IllegalStateException( "The knockout series does not have a winner yet." );
        }

        return winner;
    }

}
