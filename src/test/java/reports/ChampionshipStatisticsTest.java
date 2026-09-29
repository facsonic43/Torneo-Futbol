package reports;

import main.loader.TournamentData;
import model.match.*;
import model.participant.*;
import model.tournament.Group;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChampionshipStatisticsTest {
    private static final Country COUNTRY = new Country("Argentina");
    private static final LocalDate BIRTH = LocalDate.of(2000, 1, 1);

    @Test
    void historicalCountersAndUnplayedMatchesDoNotPolluteRankings() {
        Team home = team("Zeta");
        Team away = team("Alfa");
        Player historicalScorer = player("Histórico", Position.FORWARD);
        home.addPlayer(historicalScorer);
        Referee referee = referee("Árbitro", 20);
        ReportData data = new ReportData(new TournamentData(List.of(home, away), List.of(referee)));
        Match pending = match(home, away, referee);
        data.getMatchDetails(pending).setStartingPlayers(List.of(historicalScorer), List.of());
        pending.addEvent(new Goal(10, home, historicalScorer, null));
        pending.setHomeGoals(1);
        data.addMatch(pending);

        ChampionshipStatistics stats = new ChampionshipStatistics(data);

        assertEquals(0, stats.getPlayedMatchCount());
        assertTrue(stats.getTopScorers().isEmpty());
        assertEquals(0, stats.getPlayerStats(historicalScorer).goals());
        assertEquals(0, stats.getPlayerStats(historicalScorer).matches());
        assertEquals(0, stats.getPlayerStats(historicalScorer).minutes());
        assertEquals(0, stats.getRefereeRanking().getFirst().matches());
        assertEquals("Alfa", stats.getTeamsAlphabetically().getFirst().team().getName());
        assertEquals(0, stats.getTeamsAlphabetically().getFirst().efficiency());
    }

    @Test
    void distinguishesRegulationPenaltiesOwnGoalsAndShootoutsAndTracksBothGoalkeepers() {
        Team home = team("Local");
        Team away = team("Visitante");
        Player scorer = player("Goleador", Position.FORWARD);
        Player assister = player("Asistente", Position.MIDFIELDER);
        Player opponent = player("Rival", Position.DEFENDER);
        Player keeperHome = keeper("Arquero local");
        Player keeperAway = keeper("Arquero visitante");
        Player replacement = keeper("Arquero suplente");
        List.of(scorer, assister, keeperHome).forEach(home::addPlayer);
        List.of(opponent, keeperAway, replacement).forEach(away::addPlayer);
        ReportData data = new ReportData(new TournamentData(List.of(home, away), List.of()));
        Match match = match(home, away, null);
        ReportMatchDetails details = data.getMatchDetails(match);
        details.setStartingPlayers(List.of(scorer, assister, keeperHome), List.of(opponent, keeperAway));
        Goal opening = new Goal(10, home, scorer, assister);
        Goal equalizer = new Goal(20, away, opponent, null);
        Goal penalty = new Goal(40, home, scorer, assister);
        Goal ownGoal = new Goal(70, home, opponent, scorer);
        details.setGoalDetails(opening, false, false, null);
        details.setGoalDetails(equalizer, false, false, keeperHome);
        details.setGoalDetails(penalty, true, false, keeperAway);
        details.setGoalDetails(ownGoal, false, true, null);
        match.addEvent(opening);
        match.addEvent(equalizer);
        match.addEvent(penalty);
        match.addEvent(new Substitution(60, away, keeperAway, replacement));
        match.addEvent(ownGoal);
        match.addEvent(new PenaltyTaken(120, home, scorer, true));
        match.addEvent(new PenaltyTaken(120, away, opponent, true));
        match.setHomeGoals(3);
        match.setAwayGoals(1);
        match.setPlayed(true);
        data.addMatch(match);

        ChampionshipStatistics stats = new ChampionshipStatistics(data);

        assertEquals(0, stats.getIncompleteMatchCount());
        assertEquals(scorer, stats.getTopScorers().getFirst().player());
        assertEquals(2, stats.getPlayerStats(scorer).goals());
        assertEquals(1, stats.getPlayerStats(scorer).penaltyGoals());
        assertEquals(0, stats.getPlayerStats(scorer).assists());
        assertEquals(1, stats.getPlayerStats(assister).assists());
        assertEquals(assister, penalty.getAssistPlayer()); // El reporte no modifica el Goal compartido.
        assertEquals(1, stats.getPlayerStats(opponent).goals());
        assertEquals(1, stats.getPlayerStats(keeperHome).goalsConceded());
        assertEquals(2, stats.getPlayerStats(keeperAway).goalsConceded());
        assertEquals(1, stats.getPlayerStats(replacement).goalsConceded());
        assertEquals(2.0, stats.getPlayerStats(keeperAway).averageGoalsConceded());
        assertEquals(60, stats.getPlayerStats(keeperAway).minutes());
        assertEquals(30, stats.getPlayerStats(replacement).minutes());
        ChampionshipStatistics.TeamStats local = stats.getTeamsAlphabetically().getFirst();
        assertEquals(3, local.goalsFor());
        assertEquals(1, local.goalsAgainst());
        assertEquals(100.0, local.efficiency());
    }

    @Test
    void minutesIncludeSubstitutionsExpulsionsInjuriesAndExtraTimeWithoutCountingBench() {
        Team home = team("Local");
        Team away = team("Visitante");
        Player starter = player("Titular", Position.FORWARD);
        Player substitute = player("Cambio", Position.FORWARD);
        Player expelled = player("Expulsado", Position.DEFENDER);
        Player injured = player("Lesionado", Position.MIDFIELDER);
        Player injurySubstitute = player("Reemplazo lesión", Position.MIDFIELDER);
        Player lastMinute = player("Último minuto", Position.FORWARD);
        Player bench = player("Banco", Position.FORWARD);
        List.of(starter, substitute, expelled, injured, injurySubstitute, lastMinute, bench).forEach(home::addPlayer);
        ReportData data = new ReportData(new TournamentData(List.of(home, away), List.of()));
        Match match = match(home, away, null);
        data.getMatchDetails(match).setStartingPlayers(List.of(starter, expelled, injured), List.of());
        match.setExtraTimePlayed(true);
        match.addEvent(new Substitution(30, home, starter, substitute));
        match.addEvent(new RedCard(75, home, expelled, true));
        match.addEvent(new Injury(40, home, injured, 1)); // Inserción sin ordenar.
        match.addEvent(new Substitution(40, home, injured, injurySubstitute));
        match.addEvent(new Substitution(120, home, substitute, lastMinute));
        match.setPlayed(true);
        data.addMatch(match);

        ChampionshipStatistics stats = new ChampionshipStatistics(data);

        assertEquals(30, stats.getPlayerStats(starter).minutes());
        assertEquals(90, stats.getPlayerStats(substitute).minutes());
        assertEquals(75, stats.getPlayerStats(expelled).minutes());
        assertEquals(40, stats.getPlayerStats(injured).minutes());
        assertEquals(80, stats.getPlayerStats(injurySubstitute).minutes());
        assertEquals(0, stats.getPlayerStats(lastMinute).minutes());
        assertEquals(1, stats.getPlayerStats(lastMinute).matches());
        assertEquals(0, stats.getPlayerStats(bench).matches());
        assertEquals(substitute, stats.getMinutesRanking().getFirst().player());
        assertEquals(33.333333333333336, stats.getTeamsAlphabetically().getFirst().efficiency());
    }

    @Test
    void fairPlayCountsSecondYellowOnceWithNewAndLegacyEvents() {
        Team home = team("Local");
        Team away = team("Visitante");
        Player legacy = player("Anterior", Position.DEFENDER);
        Player explicit = player("Nuevo", Position.DEFENDER);
        home.addPlayer(legacy);
        away.addPlayer(explicit);
        ReportData data = new ReportData(new TournamentData(List.of(home, away), List.of()));
        Match match = match(home, away, null);
        data.getMatchDetails(match).setStartingPlayers(List.of(legacy), List.of(explicit));
        match.addEvent(new YellowCard(20, home, legacy));
        match.addEvent(new RedCard(40, home, legacy, false));
        match.addEvent(new YellowCard(20, away, explicit));
        match.addEvent(new YellowCard(50, away, explicit));
        match.addEvent(new RedCard(50, away, explicit, false));
        match.setPlayed(true);
        data.addMatch(match);

        ChampionshipStatistics stats = new ChampionshipStatistics(data);

        assertEquals(2, stats.getPlayerStats(legacy).yellowCards());
        assertEquals(2, stats.getPlayerStats(explicit).yellowCards());
        assertEquals(1, stats.getPlayerStats(legacy).redCards());
        assertTrue(stats.getFairPlayRanking().stream().allMatch(team -> team.fairPlayScore() == 5));
        assertEquals(home, stats.getFairPlayRanking().getFirst().team());
    }

    @Test
    void duplicateRegistrationCountsOneMatchAndRefereesUseThisChampionship() {
        Team home = team("Local");
        Team away = team("Visitante");
        Referee active = referee("Zeta", 10);
        Referee unused = referee("Alfa", 20);
        ReportData data = new ReportData(new TournamentData(List.of(home, away), List.of(active, unused)));
        data.setRefereeExperience(active, 10);
        data.setRefereeExperience(unused, 20);
        GroupMatch match = match(home, away, active);
        match.setPlayed(true);
        Group group = new Group("A", List.of(home, away));
        group.addMatch(match);
        data.addGroup(group);
        data.addMatch(match);

        ChampionshipStatistics stats = new ChampionshipStatistics(data);

        assertEquals(1, stats.getPlayedMatchCount());
        assertEquals(1, stats.getTeamsAlphabetically().getFirst().played());
        assertEquals(active, stats.getRefereeRanking().getFirst().referee());
        assertEquals(1, stats.getRefereeRanking().getFirst().matches());
        assertEquals(0, stats.getRefereeRanking().getLast().matches());
        assertEquals(15.0, stats.getAverageRefereeExperience());
        assertEquals(2, stats.getRefereesWithKnownExperienceCount());
    }

    @Test
    void playerFiltersAndEmptyAveragesAreWellDefined() {
        Team home = team("Local");
        Player goalkeeper = keeper("Zeta");
        Player striker = player("Alfa", Position.FORWARD);
        home.addPlayer(goalkeeper);
        home.addPlayer(striker);
        ChampionshipStatistics stats = new ChampionshipStatistics(new TournamentData(List.of(home), List.of()));

        assertEquals(striker, stats.getPlayers(null, null).getFirst().player());
        assertEquals(List.of(stats.getPlayerStats(goalkeeper)), stats.getPlayers(Position.GOALKEEPER, null));
        assertEquals(List.of(stats.getPlayerStats(striker)), stats.getPlayers(null, striker));
        assertTrue(stats.getPlayers(Position.GOALKEEPER, striker).isEmpty());
        assertEquals(0.0, stats.getPlayerStats(goalkeeper).averageGoalsConceded());
        assertEquals(0.0, stats.getAverageRefereeExperience());
        assertThrows(IllegalArgumentException.class, () -> stats.getPlayerStats(keeper("Ajeno")));
    }

    @Test
    void snapshotRemainsStableUntilReportIsRebuilt() {
        Team home = team("Local");
        Team away = team("Visitante");
        ReportData data = new ReportData(new TournamentData(List.of(home, away), List.of()));
        ChampionshipStatistics initial = new ChampionshipStatistics(data);
        Match match = match(home, away, null);
        match.setPlayed(true);
        data.addMatch(match);

        assertEquals(0, initial.getPlayedMatchCount());
        assertEquals(1, new ChampionshipStatistics(data).getPlayedMatchCount());
    }

    @Test
    void missingMetadataIsFlaggedUntilSupplementedWithoutChangingSharedModels() {
        Team home = team("Local");
        Team away = team("Visitante");
        Player scorer = player("Goleador", Position.FORWARD);
        Player goalkeeper = keeper("Arquero");
        home.addPlayer(scorer);
        away.addPlayer(goalkeeper);
        ReportData data = new ReportData(new TournamentData(List.of(home, away), List.of()));
        Match match = match(home, away, null);
        Goal goal = new Goal(20, home, scorer, null);
        match.addEvent(goal);
        match.setHomeGoals(1);
        match.setPlayed(true);
        data.addMatch(match);

        ChampionshipStatistics incomplete = new ChampionshipStatistics(data);
        assertEquals(1, incomplete.getIncompleteMatchCount());
        assertFalse(incomplete.hasCompleteStartingLineups());
        assertFalse(incomplete.hasCompleteGoalInformation());
        assertEquals(0, incomplete.getPlayerStats(scorer).minutes());
        assertEquals(0, incomplete.getPlayerStats(scorer).penaltyGoals());
        assertEquals(0, incomplete.getPlayerStats(goalkeeper).goalsConceded());

        ReportMatchDetails details = data.getMatchDetails(match);
        details.setStartingPlayers(List.of(scorer), List.of(goalkeeper));
        details.setGoalDetails(goal, true, false, goalkeeper);
        ChampionshipStatistics complete = new ChampionshipStatistics(data);

        assertEquals(0, complete.getIncompleteMatchCount());
        assertTrue(complete.hasCompleteStartingLineups());
        assertTrue(complete.hasCompleteGoalInformation());
        assertEquals(90, complete.getPlayerStats(scorer).minutes());
        assertEquals(1, complete.getPlayerStats(scorer).penaltyGoals());
        assertEquals(1, complete.getPlayerStats(goalkeeper).goalsConceded());
        assertEquals(1, incomplete.getIncompleteMatchCount());
        assertEquals(50, scorer.getGoals());
    }

    @Test
    void unknownRefereeExperienceIsExcludedFromAverage() {
        Referee known = referee("Conocido", 20);
        Referee unknown = referee("Sin metadata", 10);
        ReportData data = new ReportData(new TournamentData(List.of(), List.of(known, unknown)));
        data.setRefereeExperience(known, 20);

        ChampionshipStatistics stats = new ChampionshipStatistics(data);

        assertEquals(20.0, stats.getAverageRefereeExperience());
        assertEquals(1, stats.getRefereesWithKnownExperienceCount());
        assertNull(stats.getRefereeRanking().getLast().yearsOfExperience());
    }

    @Test
    void metadataCopiesLineupsAndDoesNotInferInjuryExitWithoutSubstitution() {
        Team home = team("Local");
        Team away = team("Visitante");
        Player injured = player("Lesionado", Position.FORWARD);
        Match match = match(home, away, null);
        List<Player> original = new java.util.ArrayList<>(List.of(injured));
        ReportMatchDetails details = new ReportMatchDetails();
        details.setStartingPlayers(original, List.of());
        original.clear();
        match.addEvent(new Injury(40, home, injured, 1));

        assertEquals(List.of(injured), details.getHomeStarters());
        assertEquals(90, details.getPlayerMinutes(match).get(injured));
        assertThrows(UnsupportedOperationException.class, () -> details.getHomeStarters().clear());
        assertNull(details.getGoalDetails(new Goal(10, home, injured, null)));
        assertThrows(IllegalArgumentException.class, () ->
                details.setGoalDetails(new Goal(10, home, injured, null), true, true, null));
    }

    @Test
    void absentGoalEventsFlagIncompleteStatisticsEvenWithKnownLineups() {
        Team home = team("Local");
        Team away = team("Visitante");
        ReportData data = new ReportData(new TournamentData(List.of(home, away), List.of()));
        Match match = match(home, away, null);
        data.getMatchDetails(match).setStartingPlayers(List.of(), List.of());
        match.setHomeGoals(2);
        match.setPlayed(true);
        data.addMatch(match);

        ChampionshipStatistics stats = new ChampionshipStatistics(data);

        assertEquals(1, stats.getIncompleteMatchCount());
        assertEquals(2, stats.getTeamsAlphabetically().getFirst().goalsFor());
        assertTrue(stats.getTopScorers().isEmpty());
    }

    private static Team team(String name) {
        Coach coach = new Coach("DT " + name, 1, "DNI", LocalDate.of(1980, 1, 1), COUNTRY, 2);
        return new Team(name, COUNTRY, 1, coach, 0, 0, 0, null);
    }

    private static Player player(String name, Position position) {
        // Datos históricos deliberadamente altos para descubrir su mezcla con el torneo.
        return new FieldPlayer(name, 1, "DNI", BIRTH, COUNTRY, 100, 9000, 20, 5, 50, 25,
                position, 70, 70, 70, 70, 70, 70, 70, 70);
    }

    private static Player keeper(String name) {
        return new Goalkeeper(name, 1, "DNI", BIRTH, COUNTRY, 100, 9000, 20, 5, 0, 25,
                70, 70, 70, 70, 70, 70);
    }

    private static Referee referee(String name, int years) {
        return new Referee(name, Math.floorMod(name.hashCode(), Integer.MAX_VALUE), "DNI", BIRTH, COUNTRY, 100, years);
    }

    private static GroupMatch match(Team home, Team away, Referee referee) {
        return new GroupMatch(home, away, referee, null, LocalDate.now());
    }
}
