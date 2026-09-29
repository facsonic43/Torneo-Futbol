package reports;

import main.loader.TournamentData;
import model.match.*;
import model.participant.Team;
import model.tournament.Group;

import java.time.LocalDate;
import java.util.*;

/** A read-only snapshot of qualification and the knockout tree. */
public final class BracketReport {
    public record GroupQualification(String groupName, Team first, Team second,
                                     String status, int playedMatches, int requiredMatches) {}

    public record MatchResult(Team homeTeam, Team awayTeam, LocalDate date, boolean played,
                              int homeGoals, int awayGoals, Integer homePenalties, Integer awayPenalties) {
        static MatchResult of(Match match) {
            return match == null ? null : new MatchResult(match.getHomeTeam(), match.getAwayTeam(),
                    match.getMatchDate(), match.isPlayed(), match.getHomeGoals(), match.getAwayGoals(),
                    match.getHomePenalties(), match.getAwayPenalties());
        }

        public String description() {
            String score = played ? homeGoals + " - " + awayGoals : "vs";
            String result = name(homeTeam) + " " + score + " " + name(awayTeam);
            if (!played) result += " (not played)";
            if (played && homePenalties != null && awayPenalties != null)
                result += " | penalties " + homePenalties + " - " + awayPenalties;
            if (date != null) result += " | " + date;
            return result;
        }
    }

    public record BracketNode(String id, String round, String homeSource, String awaySource,
                              Team homeTeam, Team awayTeam, MatchResult firstLeg, MatchResult secondLeg,
                              MatchResult finalMatch, Team winner, String status, String resolution) {}

    private final List<GroupQualification> qualifiers;
    private final List<BracketNode> nodes;
    private final List<MatchResult> unassignedMatches;

    public BracketReport(TournamentData data) {
        this(new ReportData(data));
    }

    public BracketReport(ReportData data) {
        Objects.requireNonNull(data, "data");
        List<Match> matches = data.getMatches();
        List<GroupQualification> qualification = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            qualification.add(i < data.getGroups().size()
                    ? qualify(data.getGroups().get(i), matches)
                    : new GroupQualification("Zone " + (i + 1), null, null, "Not drawn", 0, 6));
        }
        qualifiers = List.copyOf(qualification);
        Set<Match> used = Collections.newSetFromMap(new IdentityHashMap<>());
        List<BracketNode> tree = new ArrayList<>();
        tree.add(tie("I", "Quarter-final", "1st Zone 1", "2nd Zone 4",
                qualification.get(0).first(), qualification.get(3).second(), matches, used));
        tree.add(tie("II", "Quarter-final", "1st Zone 2", "2nd Zone 3",
                qualification.get(1).first(), qualification.get(2).second(), matches, used));
        tree.add(tie("III", "Quarter-final", "1st Zone 3", "2nd Zone 1",
                qualification.get(2).first(), qualification.get(0).second(), matches, used));
        tree.add(tie("IV", "Quarter-final", "1st Zone 4", "2nd Zone 2",
                qualification.get(3).first(), qualification.get(1).second(), matches, used));
        tree.add(tie("V", "Semi-final", "Winner I", "Winner II",
                tree.get(0).winner(), tree.get(1).winner(), matches, used));
        tree.add(tie("VI", "Semi-final", "Winner III", "Winner IV",
                tree.get(2).winner(), tree.get(3).winner(), matches, used));
        tree.add(finalNode(tree.get(4).winner(), tree.get(5).winner(), matches, used));
        nodes = List.copyOf(tree);
        unassignedMatches = matches.stream().filter(Match::isKnockout).filter(m -> !used.contains(m))
                .map(MatchResult::of).toList();
    }

    public List<GroupQualification> getQualifiers() { return qualifiers; }
    public List<BracketNode> getNodes() { return nodes; }
    public List<MatchResult> getUnassignedMatches() { return unassignedMatches; }
    public Team getChampion() { return nodes.get(6).winner(); }

    public String getText() {
        StringBuilder text = new StringBuilder("V. Championship bracket\n\n");
        for (int i = 0; i < qualifiers.size(); i++) {
            GroupQualification q = qualifiers.get(i);
            text.append("Zone ").append(i + 1).append(" (").append(q.groupName()).append("): ")
                    .append(q.status()).append(" | Played ").append(q.playedMatches()).append('/')
                    .append(q.requiredMatches()).append("\n1st: ").append(name(q.first()))
                    .append(" | 2nd: ").append(name(q.second())).append("\n");
        }
        for (BracketNode node : nodes) {
            text.append('\n').append(node.round()).append(' ').append(node.id()).append(": ")
                    .append(node.homeSource()).append(" vs ").append(node.awaySource()).append('\n')
                    .append(name(node.homeTeam())).append(" vs ").append(name(node.awayTeam())).append('\n');
            if (!node.round().equals("Final")) {
                text.append("First leg: ").append(describe(node.firstLeg())).append('\n')
                        .append("Second leg: ").append(describe(node.secondLeg())).append('\n');
            } else text.append("Match: ").append(describe(node.finalMatch())).append('\n');
            text.append(node.status()).append(" | Winner: ").append(name(node.winner()));
            if (!node.resolution().isBlank()) text.append(" | ").append(node.resolution());
            text.append('\n');
        }
        text.append("\nChampion: ").append(name(getChampion())).append('\n');
        if (!unassignedMatches.isEmpty()) {
            text.append("\nRegistered knockout matches awaiting confirmed bracket placement:\n");
            for (MatchResult match : unassignedMatches) text.append(match.description()).append('\n');
        }
        return text.toString();
    }

    public static String name(Team team) { return team == null ? "Pending" : team.getName(); }
    public static String describe(MatchResult match) { return match == null ? "Not scheduled" : match.description(); }

    private static GroupQualification qualify(Group group, List<Match> allMatches) {
        List<Team> teams = group.getTeams();
        if (teams.size() != 4 || new HashSet<>(teams).size() != 4 || teams.stream().anyMatch(Objects::isNull))
            return new GroupQualification(group.getName(), null, null, "Incomplete zone: four distinct teams required", 0, 6);
        Map<Team, Row> rows = new LinkedHashMap<>();
        teams.forEach(team -> rows.put(team, new Row(team)));
        Map<Set<Team>, GroupMatch> fixtures = new HashMap<>();
        boolean duplicatePair = false;
        for (Match match : allMatches) {
            if (!(match instanceof GroupMatch gm) || !rows.containsKey(match.getHomeTeam())
                    || !rows.containsKey(match.getAwayTeam()) || match.getHomeTeam() == match.getAwayTeam()) continue;
            Set<Team> pair = Set.of(match.getHomeTeam(), match.getAwayTeam());
            if (fixtures.putIfAbsent(pair, gm) != null) duplicatePair = true;
        }
        int played = 0;
        for (GroupMatch match : fixtures.values()) {
            if (!match.isPlayed()) continue;
            played++;
            rows.get(match.getHomeTeam()).add(match.getHomeGoals(), match.getAwayGoals());
            rows.get(match.getAwayTeam()).add(match.getAwayGoals(), match.getHomeGoals());
        }
        if (duplicatePair) return new GroupQualification(group.getName(), null, null,
                "Incomplete zone: duplicate pairing", played, 6);
        if (played != 6) return new GroupQualification(group.getName(), null, null,
                played == 0 ? "Not started" : "In progress", played, 6);

        List<Row> sorted = new ArrayList<>(rows.values());
        Comparator<Row> order = Comparator.comparingInt((Row r) -> r.points).reversed()
                .thenComparing(Comparator.comparingInt((Row r) -> r.scored - r.conceded).reversed())
                .thenComparing(Comparator.comparingInt((Row r) -> r.scored).reversed());
        sorted.sort(order);
        Set<Team> unresolved = new HashSet<>();
        for (int start = 0; start < sorted.size();) {
            int end = start + 1;
            while (end < sorted.size() && order.compare(sorted.get(start), sorted.get(end)) == 0) end++;
            if (end - start == 2) {
                Team first = sorted.get(start).team;
                Team second = sorted.get(start + 1).team;
                GroupMatch direct = fixtures.get(Set.of(first, second));
                if (direct.getHomeGoals() == direct.getAwayGoals()) {
                    unresolved.add(first);
                    unresolved.add(second);
                } else {
                    Team winner = direct.getHomeGoals() > direct.getAwayGoals() ? direct.getHomeTeam() : direct.getAwayTeam();
                    if (winner == second) Collections.swap(sorted, start, start + 1);
                }
            } else if (end - start > 2) {
                for (int i = start; i < end; i++) unresolved.add(sorted.get(i).team);
            }
            start = end;
        }
        Team first = sorted.get(0).team;
        Team second = sorted.get(1).team;
        boolean qualificationTied = unresolved.contains(first) || unresolved.contains(second);
        return new GroupQualification(group.getName(), unresolved.contains(first) ? null : first,
                unresolved.contains(second) ? null : second,
                qualificationTied ? "Unresolved qualification tie" : "Completed", played, 6);
    }

    private static BracketNode tie(String id, String round, String homeSource, String awaySource,
                                   Team home, Team away, List<Match> matches, Set<Match> used) {
        if (home == null || away == null)
            return new BracketNode(id, round, homeSource, awaySource, home, away, null, null, null,
                    null, "Awaiting qualification", "");
        List<FirstLegMatch> firsts = matches.stream().filter(m -> m instanceof FirstLegMatch && pair(m, home, away))
                .map(m -> (FirstLegMatch) m).toList();
        List<SecondLegMatch> seconds = matches.stream().filter(m -> m instanceof SecondLegMatch && pair(m, home, away))
                .map(m -> (SecondLegMatch) m).toList();
        if (firsts.size() > 1 || seconds.size() > 1)
            return new BracketNode(id, round, homeSource, awaySource, home, away, null, null, null,
                    null, "Conflicting tie records", "");
        FirstLegMatch first = firsts.isEmpty() ? null : firsts.get(0);
        SecondLegMatch second = seconds.isEmpty() ? null : seconds.get(0);
        if (first != null) used.add(first);
        if (second != null) used.add(second);
        String status = "Not scheduled";
        String resolution = "";
        Team winner = null;
        if (second != null && (second.getFirstLeg() != first || first == null
                || first.getHomeTeam() != second.getAwayTeam() || first.getAwayTeam() != second.getHomeTeam())) {
            status = "Invalid return-leg pairing";
        } else if (first != null && second != null && first.isPlayed() && second.isPlayed()) {
            int homePoints = points(goals(first, home), goals(first, away)) + points(goals(second, home), goals(second, away));
            int awayPoints = points(goals(first, away), goals(first, home)) + points(goals(second, away), goals(second, home));
            if (homePoints != awayPoints) {
                winner = homePoints > awayPoints ? home : away;
                resolution = "Points across both legs: " + homePoints + " - " + awayPoints;
            } else {
                int homeWeighted = weightedGoals(first, home) + weightedGoals(second, home);
                int awayWeighted = weightedGoals(first, away) + weightedGoals(second, away);
                if (homeWeighted != awayWeighted) {
                    winner = homeWeighted > awayWeighted ? home : away;
                    resolution = "Goal difference with away goals doubled: " + homeWeighted + " - " + awayWeighted;
                } else if (validPenalties(second)) {
                    winner = second.getHomePenalties() > second.getAwayPenalties() ? second.getHomeTeam() : second.getAwayTeam();
                    resolution = "Penalty shootout";
                } else resolution = "Tied on points and weighted goals; penalty shootout pending";
            }
            status = winner == null ? "Unresolved tie" : "Completed";
        } else if (first != null || second != null) {
            status = first != null && first.isPlayed() ? "Second leg pending"
                    : second != null && second.isPlayed() ? "First leg pending" : "Not played";
        }
        return new BracketNode(id, round, homeSource, awaySource, home, away,
                MatchResult.of(first), MatchResult.of(second), null, winner, status, resolution);
    }

    private static BracketNode finalNode(Team home, Team away, List<Match> matches, Set<Match> used) {
        Match match = null;
        Team winner = null;
        String status = "Awaiting qualification";
        String resolution = "";
        if (home != null && away != null) {
            List<Match> finals = matches.stream().filter(m -> m instanceof FinalMatch && pair(m, home, away)).toList();
            status = finals.size() > 1 ? "Conflicting final records" : "Not scheduled";
            if (finals.size() == 1) {
                match = finals.get(0);
                used.add(match);
                status = "Not played";
                if (match.isPlayed()) {
                    if (match.getHomeGoals() != match.getAwayGoals()) {
                        winner = match.getHomeGoals() > match.getAwayGoals() ? match.getHomeTeam() : match.getAwayTeam();
                        resolution = match.isExtraTimePlayed() ? "After extra time" : "Regular time";
                    } else if (validPenalties(match)) {
                        winner = match.getHomePenalties() > match.getAwayPenalties() ? match.getHomeTeam() : match.getAwayTeam();
                        resolution = "Penalty shootout";
                    }
                    status = winner == null ? "Unresolved tie" : "Completed";
                }
            }
        }
        return new BracketNode("VII", "Final", "Winner V", "Winner VI", home, away,
                null, null, MatchResult.of(match), winner, status, resolution);
    }

    private static boolean pair(Match match, Team a, Team b) {
        return match.getHomeTeam() == a && match.getAwayTeam() == b
                || match.getHomeTeam() == b && match.getAwayTeam() == a;
    }
    private static int goals(Match match, Team team) {
        return match.getHomeTeam() == team ? match.getHomeGoals() : match.getAwayGoals();
    }
    private static int weightedGoals(Match match, Team team) {
        return goals(match, team) * (match.getAwayTeam() == team ? 2 : 1);
    }
    private static int points(int scored, int conceded) { return scored > conceded ? 3 : scored == conceded ? 1 : 0; }
    private static boolean validPenalties(Match match) {
        return match.getHomePenalties() != null && match.getAwayPenalties() != null
                && match.getHomePenalties() >= 0 && match.getAwayPenalties() >= 0
                && !match.getHomePenalties().equals(match.getAwayPenalties());
    }
    private static final class Row {
        final Team team;
        int points;
        int scored;
        int conceded;
        Row(Team team) { this.team = team; }
        void add(int gf, int ga) { points += BracketReport.points(gf, ga); scored += gf; conceded += ga; }
    }
}
