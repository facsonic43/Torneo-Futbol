package model.tournament;

import control.MatchSimulator;
import model.match.FinalMatch;
import model.match.FirstLegMatch;
import model.match.GroupMatch;
import model.match.Match;
import model.match.SecondLegMatch;
import model.match.GroupMatch;
import model.match.Stadium;
import model.participant.Referee;
import model.participant.Team;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java.io.Serializable;
/*
 * Representa el campeonato completo y conserva el estado de todas sus etapas.
 * Se encarga del sorteo de grupos, generación de partidos, cruces eliminatorios,
 * selección de árbitros y estadios y avance hasta obtener al campeón.
 */
public class Tournament implements Serializable{
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

    // Sortea cuatro grupos tomando un equipo de cada bombo.
    public List<Group> drawGroups() {
        if (teams.size() != 16) {
            throw new IllegalStateException(
                    "The tournament must contain exactly 16 teams."
            );
        }

        List<Team> orderedTeams = new ArrayList<>(teams);

        orderedTeams.sort(
                Comparator.comparingInt(Team::getRanking)
        );
    public List<Group> drawGroups(List<Team> teams) {
        List<Group> groups = new ArrayList<>();
        teams.sort(Comparator.comparingInt(Team::getRanking));

        List<Team> pot1 = new ArrayList<>();
        List<Team> pot2 = new ArrayList<>();
        List<Team> pot3 = new ArrayList<>();
        List<Team> pot4 = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            pot1.add(orderedTeams.get(i));
            pot2.add(orderedTeams.get(i + 4));
            pot3.add(orderedTeams.get(i + 8));
            pot4.add(orderedTeams.get(i + 12));
            pot1.add(teams.get(i));
            pot2.add(teams.get(i + 4));
            pot3.add(teams.get(i + 8));
            pot4.add(teams.get(i + 12));
        }

        Collections.shuffle(pot1);
        Collections.shuffle(pot2);
        Collections.shuffle(pot3);
        Collections.shuffle(pot4);

        groups = new ArrayList<>();

        groups.add(
                new Group(
                        "Group A",
                        new ArrayList<>(
                                List.of(
                                        pot1.get(0),
                                        pot2.get(0),
                                        pot3.get(0),
                                        pot4.get(0)
                                )
                        )
                )
        );

        groups.add(
                new Group(
                        "Group B",
                        new ArrayList<>(
                                List.of(
                                        pot1.get(1),
                                        pot2.get(1),
                                        pot3.get(1),
                                        pot4.get(1)
                                )
                        )
                )
        );

        groups.add(
                new Group(
                        "Group C",
                        new ArrayList<>(
                                List.of(
                                        pot1.get(2),
                                        pot2.get(2),
                                        pot3.get(2),
                                        pot4.get(2)
                                )
                        )
                )
        );

        groups.add(
                new Group(
                        "Group D",
                        new ArrayList<>(
                                List.of(
                                        pot1.get(3),
                                        pot2.get(3),
                                        pot3.get(3),
                                        pot4.get(3)
                                )
                        )
                )
        );
        groups.add(new Group("Group A", new ArrayList<>(List.of(pot1.get(0), pot2.get(0), pot3.get(0), pot4.get(0)))));
        groups.add(new Group("Group B", new ArrayList<>(List.of(pot1.get(1), pot2.get(1), pot3.get(1), pot4.get(1)))));
        groups.add(new Group("Group C", new ArrayList<>(List.of(pot1.get(2), pot2.get(2), pot3.get(2), pot4.get(2)))));
        groups.add(new Group("Group D", new ArrayList<>(List.of(pot1.get(3), pot2.get(3), pot3.get(3), pot4.get(3)))));

        return groups;
    }

    public List<Group> drawGroups(List<Team> teams) {
        this.teams = new ArrayList<>(teams);
        return drawGroups();
    }

    public void generateGroupMatches(List<Stadium> stadiums) {
        generateGroupMatches(
                groups,
                referees,
                stadiums
        );
    }

    public void generateGroupMatches(
            List<Group> groups,
            List<Referee> referees) {

        generateGroupMatches(
                groups,
                referees,
                Collections.emptyList()
        );
    }

    public void generateGroupMatches(
            List<Group> groups,
            List<Referee> referees,
            List<Stadium> stadiums) {

        this.groups = groups;
        this.referees = new ArrayList<>(referees);

        int stadiumIndex = 0;

        LocalDate firstMatchDate = LocalDate.now();

        int[][] pairingsByRound = {
                {0, 1, 2, 3},
                {0, 2, 1, 3},
                {0, 3, 1, 2}
        };

        for (Group group : groups) {
            List<Team> groupTeams = group.getTeams();

            for (int round = 0;
                 round < pairingsByRound.length;
                 round++) {

                int[] pairings = pairingsByRound[round];

                for (int pairing = 0;
                     pairing < pairings.length;
                     pairing += 2) {

                    Team home =
                            groupTeams.get(
                                    pairings[pairing]
                            );

                    Team away =
                            groupTeams.get(
                                    pairings[pairing + 1]
                            );

                    Referee referee =
                            selectReferee(
                                    home,
                                    away
                            );

                    Stadium stadium;

                    if (stadiums.isEmpty()) {
                        stadium =
                                new Stadium(
                                        "Stadium of "
                                                + home.getName(),
                                        1
                                );
                    } else {
                        stadium =
                                stadiums.get(
                                        stadiumIndex
                                                % stadiums.size()
                                );

                        stadiumIndex++;
                    }

                    GroupMatch match =
                            new GroupMatch(
                                    home,
                                    away,
                                    referee,
                                    stadium,
                                    firstMatchDate.plusDays(round)
                            );

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

    public void simulateGroupStage(
            MatchSimulator simulator) {

        simulateGroupStage(
                groups,
                simulator
        );
    }

    public void simulateGroupStage(
            List<Group> groups,
            MatchSimulator simulator) {

        for (Group group : groups) {
            for (GroupMatch match :
                    group.getMatches()) {

                simulator.simulateMatch(
                        match
                );
            }
        }
    }

    // Genera los cruces exactos de cuartos.
    public void generateQuarterFinals(
            List<Stadium> stadiums) {

        if (groups.size() != 4) {
            throw new IllegalStateException(
                    "The four groups must exist before generating quarter-finals."
            );
        }

        prepareKnockoutStadiums(
                stadiums
        );

        quarterFinalFirstLegs.clear();
        quarterFinalSecondLegs.clear();

        Team a1 =
                groups.get(0)
                        .getQualifiedTeams()
                        .get(0);

        Team a2 =
                groups.get(0)
                        .getQualifiedTeams()
                        .get(1);

        Team b1 =
                groups.get(1)
                        .getQualifiedTeams()
                        .get(0);

        Team b2 =
                groups.get(1)
                        .getQualifiedTeams()
                        .get(1);

        Team c1 =
                groups.get(2)
                        .getQualifiedTeams()
                        .get(0);

        Team c2 =
                groups.get(2)
                        .getQualifiedTeams()
                        .get(1);

        Team d1 =
                groups.get(3)
                        .getQualifiedTeams()
                        .get(0);

        Team d2 =
                groups.get(3)
                        .getQualifiedTeams()
                        .get(1);

        LocalDate firstLegDate =
                getLastGroupDate()
                        .plusDays(7);

        LocalDate secondLegDate =
                firstLegDate
                        .plusDays(7);

        addTwoLegSeries(
                a1,
                d2,
                firstLegDate,
                secondLegDate,
                quarterFinalFirstLegs,
                quarterFinalSecondLegs
        );

        addTwoLegSeries(
                b1,
                c2,
                firstLegDate,
                secondLegDate,
                quarterFinalFirstLegs,
                quarterFinalSecondLegs
        );

        addTwoLegSeries(
                c1,
                a2,
                firstLegDate,
                secondLegDate,
                quarterFinalFirstLegs,
                quarterFinalSecondLegs
        );

        addTwoLegSeries(
                d1,
                b2,
                firstLegDate,
                secondLegDate,
                quarterFinalFirstLegs,
                quarterFinalSecondLegs
        );
    }

    public void simulateQuarterFinals(
            MatchSimulator simulator) {

        simulateTwoLegRound(
                quarterFinalFirstLegs,
                quarterFinalSecondLegs,
                simulator
        );
    }

    // Genera semifinales con Ganador I vs II y Ganador III vs IV.
    public void generateSemiFinals() {
        if (quarterFinalSecondLegs.size() != 4) {
            throw new IllegalStateException(
                    "Quarter-finals must be generated first."
            );
        }

        Team winner1 =
                requireWinner(
                        quarterFinalSecondLegs.get(0)
                );

        Team winner2 =
                requireWinner(
                        quarterFinalSecondLegs.get(1)
                );

        Team winner3 =
                requireWinner(
                        quarterFinalSecondLegs.get(2)
                );

        Team winner4 =
                requireWinner(
                        quarterFinalSecondLegs.get(3)
                );

        semiFinalFirstLegs.clear();
        semiFinalSecondLegs.clear();

        LocalDate firstLegDate =
                quarterFinalSecondLegs
                        .get(0)
                        .getMatchDate()
                        .plusDays(7);

        LocalDate secondLegDate =
                firstLegDate
                        .plusDays(7);

        addTwoLegSeries(
                winner1,
                winner2,
                firstLegDate,
                secondLegDate,
                semiFinalFirstLegs,
                semiFinalSecondLegs
        );

        addTwoLegSeries(
                winner3,
                winner4,
                firstLegDate,
                secondLegDate,
                semiFinalFirstLegs,
                semiFinalSecondLegs
        );
    }

    public void simulateSemiFinals(
            MatchSimulator simulator) {

        simulateTwoLegRound(
                semiFinalFirstLegs,
                semiFinalSecondLegs,
                simulator
        );
    }

    public void generateFinal() {
        if (semiFinalSecondLegs.size() != 2) {
            throw new IllegalStateException(
                    "Semi-finals must be generated first."
            );
        }

        Team finalist1 =
                requireWinner(
                        semiFinalSecondLegs.get(0)
                );

        Team finalist2 =
                requireWinner(
                        semiFinalSecondLegs.get(1)
                );

        LocalDate finalDate =
                semiFinalSecondLegs
                        .get(0)
                        .getMatchDate()
                        .plusDays(7);

        finalMatch =
                new FinalMatch(
                        finalist1,
                        finalist2,
                        selectReferee(
                                finalist1,
                                finalist2
                        ),
                        nextKnockoutStadium(),
                        finalDate
                );
    }

    public void simulateFinal(
            MatchSimulator simulator) {

        if (finalMatch == null) {
            throw new IllegalStateException(
                    "The final must be generated first."
            );
        }

        simulator.simulateMatch(
                finalMatch
        );

        champion =
                finalMatch.getWinner();
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
                        selectReferee(
                                firstLegHome,
                                firstLegAway
                        ),
                        nextKnockoutStadium(),
                        firstLegDate
                );

        SecondLegMatch secondLeg =
                new SecondLegMatch(
                        firstLegAway,
                        firstLegHome,
                        selectReferee(
                                firstLegAway,
                                firstLegHome
                        ),
                        nextKnockoutStadium(),
                        secondLegDate,
                        firstLeg
                );

        firstLegMatches.add(
                firstLeg
        );

        secondLegMatches.add(
                secondLeg
        );
    }

    private void simulateTwoLegRound(
            List<FirstLegMatch> firstLegMatches,
            List<SecondLegMatch> secondLegMatches,
            MatchSimulator simulator) {

        if (firstLegMatches.size()
                != secondLegMatches.size()) {

            throw new IllegalStateException(
                    "Invalid two-leg round configuration."
            );
        }

        for (int i = 0;
             i < firstLegMatches.size();
             i++) {

            simulator.simulateMatch(
                    firstLegMatches.get(i)
            );

            simulator.simulateMatch(
                    secondLegMatches.get(i)
            );
        }
    }

    // Selecciona únicamente árbitros habilitados para dirigir el partido.
    private Referee selectReferee(
            Team home,
            Team away) {

        if (referees.isEmpty()) {
            throw new IllegalStateException(
                    "No referees are available."
            );
        }

        List<Referee> eligibleReferees =
                new ArrayList<>();

        for (Referee referee : referees) {
            if (referee.canOfficiate(
                    home,
                    away)) {

                eligibleReferees.add(
                        referee
                );
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
            throw new IllegalStateException(
                    "No eligible referee for "
                            + home.getName()
                            + " vs "
                            + away.getName()
            );
        }

        Referee selected =
                eligibleReferees.get(
                        refereeIndex
                                % eligibleReferees.size()
                );

        refereeIndex++;

        return selected;
    }

    // Reserva estadios diferentes para todos los partidos eliminatorios.
    private void prepareKnockoutStadiums(
            List<Stadium> stadiums) {

        if (stadiums == null
                || stadiums.size() < 13) {

            throw new IllegalStateException(
                    "At least 13 stadiums are required for the knockout stage without repetitions."
            );
        }

        knockoutStadiums =
                new ArrayList<>(stadiums);

        Collections.shuffle(
                knockoutStadiums
        );

        knockoutStadiumIndex = 0;
    }

    private Stadium nextKnockoutStadium() {
        if (knockoutStadiumIndex
                >= knockoutStadiums.size()) {

            throw new IllegalStateException(
                    "There are no unused stadiums available."
            );
        }

        Stadium stadium =
                knockoutStadiums.get(
                        knockoutStadiumIndex
                );

        knockoutStadiumIndex++;

        return stadium;
    }

    private LocalDate getLastGroupDate() {
        LocalDate lastDate =
                LocalDate.now();

        for (Group group : groups) {
            for (GroupMatch match :
                    group.getMatches()) {

                if (match.getMatchDate()
                        .isAfter(lastDate)) {

                    lastDate =
                            match.getMatchDate();
                }
            }
        }

        return lastDate;
    }

    private Team requireWinner(
            SecondLegMatch match) {

        Team winner =
                match.getWinner();

        if (winner == null) {
            throw new IllegalStateException(
                    "The knockout series does not have a winner yet."
            );
        }

        return winner;
    }

    public List<Match> getAllMatches() {
        List<Match> allMatches =
                new ArrayList<>();

        for (Group group : groups) {
            allMatches.addAll(
                    group.getMatches()
            );
        }

        allMatches.addAll(
                quarterFinalFirstLegs
        );

        allMatches.addAll(
                quarterFinalSecondLegs
        );

        allMatches.addAll(
                semiFinalFirstLegs
        );

        allMatches.addAll(
                semiFinalSecondLegs
        );

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
}
            return referees.get(startIndex % referees.size());
        }

        return eligibleReferees.get(startIndex % eligibleReferees.size());
    }
}

