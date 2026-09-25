package reports;

import com.google.gson.*;
import main.loader.TournamentData;
import model.match.Goal;
import model.match.Match;
import model.match.SecondLegMatch;
import model.participant.*;
import model.tournament.Group;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Report input kept outside the simulation and domain classes.
 * The application supplies existing groups, matches and any additional report details.
 */
public final class ReportData {
    private final TournamentData participants;
    private final List<Group> groups = new ArrayList<>();
    private final List<Match> matches = new ArrayList<>();
    private final Map<Match, ReportMatchDetails> matchDetails = new IdentityHashMap<>();
    private final Map<String, Integer> refereeExperience = new HashMap<>();
    private final Map<String, Map<String, Integer>> playerSkills = new HashMap<>();

    public ReportData(TournamentData participants) {
        this.participants = Objects.requireNonNull(participants, "Tournament participants are required");
    }

    /** Uses the provided project's initial file for personal details without adding domain getters. */
    public static ReportData fromInitialData(TournamentData participants) {
        ReportData reports = new ReportData(participants);
        reports.loadInitialProfiles("torneo.json");
        return reports;
    }

    public List<Team> getTeams() { return participants.getTeams(); }
    public List<Referee> getReferees() { return participants.getReferees(); }
    public List<Group> getGroups() { return List.copyOf(groups); }

    public void addGroup(Group group) {
        Objects.requireNonNull(group, "Group is required");
        if (!groups.contains(group)) groups.add(group);
    }

    public void addMatch(Match match) {
        Objects.requireNonNull(match, "Match is required");
        if (!matches.contains(match)) matches.add(match);
    }

    /** Includes group fixtures and first legs referenced by return legs, each match only once. */
    public List<Match> getMatches() {
        List<Match> result = new ArrayList<>();
        Set<Match> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Group group : groups) {
            for (Match match : group.getMatches()) addOnce(result, seen, match);
        }
        for (Match match : matches) addOnce(result, seen, match);
        return List.copyOf(result);
    }

    private static void addOnce(List<Match> result, Set<Match> seen, Match match) {
        if (match == null || !seen.add(match)) return;
        if (match instanceof SecondLegMatch second) addOnce(result, seen, second.getFirstLeg());
        result.add(match);
    }

    public ReportMatchDetails getMatchDetails(Match match) {
        ReportMatchDetails details = matchDetails.computeIfAbsent(Objects.requireNonNull(match), ignored -> new ReportMatchDetails());
        if (!details.hasStartingPlayers() && match.hasRecordedStartingPlayers()) {
            details.setStartingPlayers(match.getHomeStartingPlayers(), match.getAwayStartingPlayers());
        }
        return details;
    }

    public void setRefereeExperience(Referee referee, int years) {
        if (years < 0) throw new IllegalArgumentException("Experience cannot be negative");
        refereeExperience.put(key(referee), years);
    }

    public Integer getRefereeExperience(Referee referee) { return refereeExperience.get(key(referee)); }

    public void setPlayerSkills(Player player, Map<String, Integer> skills) {
        for (Map.Entry<String, Integer> skill : skills.entrySet()) {
            if (skill.getValue() == null || skill.getValue() < 0 || skill.getValue() > 100) {
                throw new IllegalArgumentException("Skills must be between 0 and 100");
            }
        }
        playerSkills.put(key(player), Collections.unmodifiableMap(new LinkedHashMap<>(skills)));
    }

    public Map<String, Integer> getPlayerSkills(Player player) {
        return playerSkills.getOrDefault(key(player), Map.of());
    }

    /**
     * Reads only the optional report profile data; it never loads or simulates results.
     * Missing resources leave the corresponding report fields explicitly unavailable.
     */
    public void loadInitialProfiles(String resourceName) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (stream == null) return;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject tournament = root.getAsJsonObject("torneo");
            if (tournament == null) return;
            JsonObject referees = tournament.getAsJsonObject("arbitros");
            if (referees != null && referees.has("arbitro")) {
                for (JsonElement entry : referees.getAsJsonArray("arbitro")) {
                    JsonObject referee = entry.getAsJsonObject();
                    if (referee.has("yearsOfficiated")) {
                        int years = referee.get("yearsOfficiated").getAsInt();
                        if (years < 0) throw new IllegalArgumentException("Experience cannot be negative");
                        refereeExperience.put(key(referee.getAsJsonObject("persona")), years);
                    }
                }
            }
            JsonObject teams = tournament.getAsJsonObject("equipos");
            if (teams == null || !teams.has("equipo")) return;
            for (JsonElement entry : teams.getAsJsonArray("equipo")) {
                JsonObject squad = entry.getAsJsonObject().getAsJsonObject("plantel");
                if (squad == null || !squad.has("jugadores")) continue;
                for (JsonElement element : squad.getAsJsonObject("jugadores").getAsJsonArray("jugador")) {
                    JsonObject player = element.getAsJsonObject();
                    JsonObject skills = player.getAsJsonObject("caracteristicas");
                    if (skills == null) continue;
                    Map<String, Integer> values = new LinkedHashMap<>();
                    skills.entrySet().forEach(skill -> values.put(skill.getKey(), skill.getValue().getAsInt()));
                    playerSkills.put(key(player.getAsJsonObject("persona")), Collections.unmodifiableMap(values));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read report profiles: " + resourceName, e);
        }
    }

    private static String key(Person person) {
        return person.getIdType().trim().toUpperCase(Locale.ROOT) + "-" + person.getIdNumber();
    }

    private static String key(JsonObject person) {
        return person.get("idType").getAsString().trim().toUpperCase(Locale.ROOT) + "-" + person.get("idNumber").getAsInt();
    }
}
