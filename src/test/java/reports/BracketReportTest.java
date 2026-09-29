package reports;

import main.loader.TournamentData;
import model.match.*;
import model.participant.Team;
import model.tournament.Group;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BracketReportTest {
    @Test
    void undrawnTournamentHasNoInventedQualifiersOrChampion() {
        BracketReport report = new BracketReport(new ReportData(new TournamentData(List.of(), List.of())));
        assertEquals(4, report.getQualifiers().size());
        assertEquals(7, report.getNodes().size());
        assertTrue(report.getQualifiers().stream().allMatch(q -> q.first() == null && q.second() == null));
        assertNull(report.getChampion());
        assertTrue(report.getText().contains("Not drawn"));
    }

    @Test
    void groupMustCompleteSixUniquePairingsBeforeQualification() {
        ReportData data = completedGroups();
        data.getGroups().get(0).getMatches().get(5).setPlayed(false);
        BracketReport report = new BracketReport(data);
        assertNull(report.getQualifiers().get(0).first());
        assertEquals(5, report.getQualifiers().get(0).playedMatches());
        assertNull(report.getNodes().get(0).homeTeam());
    }

    @Test
    void registeringAnExistingGroupMatchDoesNotCountItTwice() {
        ReportData data = completedGroups();
        GroupMatch match = data.getGroups().get(0).getMatches().get(0);
        data.addMatch(match);
        data.addMatch(match);
        assertEquals(24, data.getMatches().size());
        assertEquals(6, new BracketReport(data).getQualifiers().get(0).playedMatches());
    }

    @Test
    void duplicatePairingCannotSubstituteForMissingFixture() {
        ReportData data = completedGroups();
        Group group = data.getGroups().get(0);
        group.getMatches().remove(5);
        GroupMatch repeated = groupMatch(group.getTeams().get(0), group.getTeams().get(1), 1, 0);
        group.addMatch(repeated);
        BracketReport.GroupQualification q = new BracketReport(data).getQualifiers().get(0);
        assertNull(q.first());
        assertTrue(q.status().contains("duplicate"));
    }

    @Test
    void tiedStandingsUseDirectMatchBeforeInsertionOrder() {
        Team a = team("A");
        Team b = team("B");
        Team c = team("C");
        Team d = team("D");
        Group group = new Group("Direct result", List.of(b, a, c, d));
        group.addMatch(groupMatch(a, b, 1, 0));
        group.addMatch(groupMatch(a, c, 0, 1));
        group.addMatch(groupMatch(a, d, 1, 0));
        group.addMatch(groupMatch(b, c, 1, 0));
        group.addMatch(groupMatch(b, d, 1, 0));
        group.addMatch(groupMatch(c, d, 0, 1));
        ReportData data = new ReportData(new TournamentData(group.getTeams(), List.of()));
        data.addGroup(group);
        BracketReport.GroupQualification q = new BracketReport(data).getQualifiers().get(0);
        assertSame(a, q.first());
        assertSame(b, q.second());
    }

    @Test
    void anUnresolvedGroupTieDoesNotChooseAlphabetically() {
        ReportData data = completedGroups();
        data.getGroups().get(0).getMatches().forEach(m -> { m.setHomeGoals(0); m.setAwayGoals(0); });
        BracketReport.GroupQualification q = new BracketReport(data).getQualifiers().get(0);
        assertNull(q.first());
        assertNull(q.second());
        assertEquals("Unresolved qualification tie", q.status());
    }

    @Test
    void quarterFinalPairingsFollowAssignmentAndFirstLegCannotQualifyWinner() {
        ReportData data = completedGroups();
        BracketReport report = new BracketReport(data);
        List<BracketReport.BracketNode> nodes = report.getNodes();
        assertSame(data.getGroups().get(0).getTeams().get(0), nodes.get(0).homeTeam());
        assertSame(data.getGroups().get(3).getTeams().get(1), nodes.get(0).awayTeam());
        assertSame(data.getGroups().get(1).getTeams().get(0), nodes.get(1).homeTeam());
        assertSame(data.getGroups().get(2).getTeams().get(1), nodes.get(1).awayTeam());
        assertSame(data.getGroups().get(2).getTeams().get(0), nodes.get(2).homeTeam());
        assertSame(data.getGroups().get(0).getTeams().get(1), nodes.get(2).awayTeam());
        assertSame(data.getGroups().get(3).getTeams().get(0), nodes.get(3).homeTeam());
        assertSame(data.getGroups().get(1).getTeams().get(1), nodes.get(3).awayTeam());
        FirstLegMatch first = firstLeg(nodes.get(0).homeTeam(), nodes.get(0).awayTeam(), 8, 0);
        data.addMatch(first);
        BracketReport afterFirst = new BracketReport(data);
        assertNull(afterFirst.getNodes().get(0).winner());
        assertNull(afterFirst.getNodes().get(4).homeTeam());
        assertEquals("Second leg pending", afterFirst.getNodes().get(0).status());
    }

    @Test
    void pointsTakePriorityOverWeightedAwayGoals() {
        ReportData data = completedGroups();
        BracketReport.BracketNode quarter = new BracketReport(data).getNodes().get(0);
        addTie(data, quarter.homeTeam(), quarter.awayTeam(), 3, 3, 0, 1);
        BracketReport.BracketNode resolved = new BracketReport(data).getNodes().get(0);
        assertSame(quarter.homeTeam(), resolved.winner());
        assertEquals("Points across both legs: 4 - 1", resolved.resolution());
    }

    @Test
    void weightedAwayGoalsResolveTiesAndPenaltiesResolveEqualWeightedGoals() {
        ReportData data = completedGroups();
        BracketReport.BracketNode quarter = new BracketReport(data).getNodes().get(0);
        // Equal points; ordinary aggregate favors A 4-3, but weighted goals favor B 5-4.
        addTie(data, quarter.homeTeam(), quarter.awayTeam(), 4, 2, 1, 0);
        BracketReport.BracketNode resolved = new BracketReport(data).getNodes().get(0);
        assertSame(quarter.awayTeam(), resolved.winner());
        assertTrue(resolved.resolution().contains("away goals doubled"));

        BracketReport.BracketNode other = new BracketReport(data).getNodes().get(1);
        SecondLegMatch returnLeg = addTie(data, other.homeTeam(), other.awayTeam(), 0, 0, 0, 0);
        assertNull(new BracketReport(data).getNodes().get(1).winner());
        returnLeg.setHomePenalties(4);
        returnLeg.setAwayPenalties(5);
        assertSame(other.homeTeam(), new BracketReport(data).getNodes().get(1).winner());
    }

    @Test
    void finalChampionIsOnlyPublishedAfterFinalHasBeenPlayed() {
        ReportData data = completedGroups();
        for (BracketReport.BracketNode quarter : new BracketReport(data).getNodes().subList(0, 4))
            addTie(data, quarter.homeTeam(), quarter.awayTeam(), 1, 0, 0, 1);
        for (BracketReport.BracketNode semi : new BracketReport(data).getNodes().subList(4, 6))
            addTie(data, semi.homeTeam(), semi.awayTeam(), 1, 0, 0, 1);
        BracketReport.BracketNode finalNode = new BracketReport(data).getNodes().get(6);
        FinalMatch finalMatch = new FinalMatch(finalNode.homeTeam(), finalNode.awayTeam(), null, null, LocalDate.of(2026, 10, 1));
        finalMatch.setHomeGoals(2);
        data.addMatch(finalMatch);
        assertNull(new BracketReport(data).getChampion());
        finalMatch.setPlayed(true);
        BracketReport snapshot = new BracketReport(data);
        assertSame(finalNode.homeTeam(), snapshot.getChampion());
        finalMatch.setHomeGoals(0);
        finalMatch.setAwayGoals(3);
        assertEquals(2, snapshot.getNodes().get(6).finalMatch().homeGoals());
        assertSame(finalNode.homeTeam(), snapshot.getChampion());
        assertSame(finalNode.awayTeam(), new BracketReport(data).getChampion());
    }

    private static ReportData completedGroups() {
        List<Team> allTeams = new ArrayList<>();
        ReportData data = new ReportData(new TournamentData(allTeams, List.of()));
        for (int zone = 1; zone <= 4; zone++) {
            List<Team> teams = new ArrayList<>();
            for (int place = 1; place <= 4; place++) teams.add(team("Z" + zone + "T" + place));
            allTeams.addAll(teams);
            Group group = new Group("Group " + zone, teams);
            for (int a = 0; a < 4; a++) for (int b = a + 1; b < 4; b++)
                group.addMatch(groupMatch(teams.get(a), teams.get(b), 1, 0));
            data.addGroup(group);
        }
        return data;
    }

    private static Team team(String name) { return new Team(name, null, 1, null, 0, 0, 0, null); }
    private static GroupMatch groupMatch(Team home, Team away, int hg, int ag) {
        GroupMatch match = new GroupMatch(home, away, null, null, LocalDate.of(2026, 9, 1));
        score(match, hg, ag);
        return match;
    }
    private static FirstLegMatch firstLeg(Team home, Team away, int hg, int ag) {
        FirstLegMatch match = new FirstLegMatch(home, away, null, null, LocalDate.of(2026, 9, 15));
        score(match, hg, ag);
        return match;
    }
    private static SecondLegMatch addTie(ReportData data, Team home, Team away, int firstHome, int firstAway,
                                         int secondHome, int secondAway) {
        FirstLegMatch first = firstLeg(home, away, firstHome, firstAway);
        SecondLegMatch second = new SecondLegMatch(away, home, null, null, LocalDate.of(2026, 9, 20), first);
        score(second, secondHome, secondAway);
        data.addMatch(second); // The registration also makes its first leg available to reports.
        return second;
    }
    private static void score(Match match, int hg, int ag) {
        match.setHomeGoals(hg);
        match.setAwayGoals(ag);
        match.setPlayed(true);
    }
}
