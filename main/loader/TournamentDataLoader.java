package main.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import exceptions.InvalidTournamentDataException;
import model.participant.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Carga los datos iniciales del torneo desde el archivo JSON.
 * Lee equipos, técnicos, jugadores y árbitros y crea manualmente los objetos del modelo.
 * De esta manera la lectura del archivo queda separada de las clases del dominio
 * y los datos pueden validarse antes de ser utilizados por el campeonato.
 */
public class TournamentDataLoader {
    private Map<String, Country> countries;
    private Set<String> documents;
    private DateTimeFormatter dateFormatter;

    public TournamentDataLoader() {
        countries = new HashMap<>();
        documents = new HashSet<>();
        dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    }

    public TournamentData load(String fileName) {
        countries.clear();
        documents.clear();

        try {
            JsonObject tournamentJson = loadTournamentJson(fileName);

            List<Team> teams = loadTeams(tournamentJson);
            List<Referee> referees = loadReferees(tournamentJson);

            return new TournamentData(teams, referees);

        } catch (InvalidTournamentDataException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTournamentDataException(
                    "Error loading tournament data: " + e.getMessage()
            );
        }
    }

    // ABRE JSON

    private JsonObject loadTournamentJson(String fileName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

        if (inputStream == null) {
            throw new InvalidTournamentDataException(
                    "JSON file not found: " + fileName
            );
        }

        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (!root.has("torneo")) {
                throw new InvalidTournamentDataException(
                        "The JSON does not contain tournament data."
                );
            }

            return root.getAsJsonObject("torneo");

        } catch (InvalidTournamentDataException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTournamentDataException(
                    "Error reading JSON file: " + e.getMessage()
            );
        }
    }

    // carga los equipos del json y crea cada objeto, validando

    private List<Team> loadTeams(JsonObject tournamentJson) {
        List<Team> teams = new ArrayList<>();

        JsonObject teamsObject = tournamentJson.getAsJsonObject("equipos");
        JsonArray teamsArray = teamsObject.getAsJsonArray("equipo");

        if (teamsArray.size() != 16) {
            throw new InvalidTournamentDataException(
                    "The tournament must contain exactly 16 teams. Found: " + teamsArray.size()
            );
        }

        for (JsonElement element : teamsArray) {
            JsonObject teamJson = element.getAsJsonObject();

            String name = teamJson.get("name").getAsString();
            Country country = getCountry(teamJson.get("country").getAsString());
            int ranking = teamJson.get("ranking").getAsInt();
            int editionsPlayed = teamJson.get("editionsPlayed").getAsInt();
            int internationalTitles = teamJson.get("internationalTitles").getAsInt();
            int nationalTitles = teamJson.get("nationalTitles").getAsInt();

            JsonObject squadJson = teamJson.getAsJsonObject("plantel");

            Coach coach = loadCoach(squadJson.getAsJsonObject("dt"));

            Team team = new Team(
                    name,
                    country,
                    ranking,
                    coach,
                    editionsPlayed,
                    internationalTitles,
                    nationalTitles,
                    null
            );

            loadPlayers(team, squadJson);
            validateSquad(team);

            teams.add(team);
        }

        return teams;
    }

    private Coach loadCoach(JsonObject coachJson) {
        JsonObject personJson = coachJson.getAsJsonObject("persona");

        String name = personJson.get("name").getAsString();
        int idNumber = personJson.get("idNumber").getAsInt();
        String idType = personJson.get("idType").getAsString();
        LocalDate birthDate = parseDate(personJson.get("birthDate").getAsString());
        Country nationality = getCountry(personJson.get("nationality").getAsString());
        int titlesObtained = coachJson.get("titlesObtained").getAsInt();

        registerDocument(idType, idNumber, name);

        if (titlesObtained < 0) {
            throw new InvalidTournamentDataException(
                    "Coach titles cannot be negative: " + name
            );
        }

        return new Coach(
                name,
                idNumber,
                idType,
                birthDate,
                nationality,
                titlesObtained
        );
    }

    private void loadPlayers(Team team, JsonObject squadJson) {
        JsonObject playersObject = squadJson.getAsJsonObject("jugadores");
        JsonArray playersArray = playersObject.getAsJsonArray("jugador");

        if (playersArray.size() != 18) {
            throw new InvalidTournamentDataException(
                    "Team " + team.getName() +
                            " must have exactly 18 players. Found: " + playersArray.size()
            );
        }

        for (JsonElement element : playersArray) {
            Player player = loadPlayer(element.getAsJsonObject());
            team.addPlayer(player);
        }
    }

    private Player loadPlayer(JsonObject playerJson) {
        String positionText = playerJson.get("position").getAsString();

        Position position;

        try {
            position = Position.valueOf(positionText);
        } catch (IllegalArgumentException e) {
            throw new InvalidTournamentDataException(
                    "Invalid player position: " + positionText
            );
        }

        JsonObject personJson = playerJson.getAsJsonObject("persona");
        JsonObject characteristicsJson = playerJson.getAsJsonObject("caracteristicas");
        JsonObject statisticsJson = playerJson.getAsJsonObject("estadisticas");

        String name = personJson.get("name").getAsString();
        int idNumber = personJson.get("idNumber").getAsInt();
        String idType = personJson.get("idType").getAsString();
        LocalDate birthDate = parseDate(personJson.get("birthDate").getAsString());
        Country nationality = getCountry(personJson.get("nationality").getAsString());

        int matchesPlayed = statisticsJson.get("matchesPlayed").getAsInt();
        int minutesPlayed = statisticsJson.get("minutesPlayed").getAsInt();
        int yellowCards = statisticsJson.get("yellowCards").getAsInt();
        int redCards = statisticsJson.get("redCards").getAsInt();
        int goals = statisticsJson.get("goals").getAsInt();
        int assists = statisticsJson.get("assists").getAsInt();

        registerDocument(idType, idNumber, name);

        if (position == Position.GOALKEEPER) {
            return new Goalkeeper(
                    name,
                    idNumber,
                    idType,
                    birthDate,
                    nationality,
                    matchesPlayed,
                    minutesPlayed,
                    yellowCards,
                    redCards,
                    goals,
                    assists,
                    readSkill(characteristicsJson, "speed", name),
                    readSkill(characteristicsJson, "jumping", name),
                    readSkill(characteristicsJson, "passing", name),
                    readSkill(characteristicsJson, "reflexes", name),
                    readSkill(characteristicsJson, "oneOnOne", name),
                    readSkill(characteristicsJson, "kicking", name)
            );
        }

        return new FieldPlayer(
                name,
                idNumber,
                idType,
                birthDate,
                nationality,
                matchesPlayed,
                minutesPlayed,
                yellowCards,
                redCards,
                goals,
                assists,
                position,
                readSkill(characteristicsJson, "dribbling", name),
                readSkill(characteristicsJson, "defensiveSkills", name),
                readSkill(characteristicsJson, "finishing", name),
                readSkill(characteristicsJson, "stamina", name),
                readSkill(characteristicsJson, "vision", name),
                readSkill(characteristicsJson, "heading", name),
                readSkill(characteristicsJson, "speed", name),
                readSkill(characteristicsJson, "passing", name)
        );
    }

    private List<Referee> loadReferees(JsonObject tournamentJson) {
        List<Referee> referees = new ArrayList<>();

        JsonObject refereesObject = tournamentJson.getAsJsonObject("arbitros");
        JsonArray refereesArray = refereesObject.getAsJsonArray("arbitro");

        if (refereesArray.isEmpty()) {
            throw new InvalidTournamentDataException(
                    "The tournament must contain referees."
            );
        }

        for (JsonElement element : refereesArray) {
            JsonObject refereeJson = element.getAsJsonObject();
            JsonObject personJson = refereeJson.getAsJsonObject("persona");

            String name = personJson.get("name").getAsString();
            int idNumber = personJson.get("idNumber").getAsInt();
            String idType = personJson.get("idType").getAsString();
            LocalDate birthDate = parseDate(personJson.get("birthDate").getAsString());
            Country nationality = getCountry(personJson.get("nationality").getAsString());

            int matchesOfficiated = refereeJson.get("matchesOfficiated").getAsInt();
            int yearsOfficiated = refereeJson.get("yearsOfficiated").getAsInt();

            registerDocument(idType, idNumber, name);

            if (yearsOfficiated < 0) {
                throw new InvalidTournamentDataException(
                        "Years officiated cannot be negative: " + name
                );
            }

            referees.add(new Referee(
                    name,
                    idNumber,
                    idType,
                    birthDate,
                    nationality,
                    matchesOfficiated,
                    yearsOfficiated
            ));
        }

        return referees;
    }

    // busca un pais ya creado y, si no existe, lo crea para reutilizarlo
    private Country getCountry(String countryName) {
        String cleanName = countryName.trim();
        String key = cleanName.toLowerCase();

        if (!countries.containsKey(key)) {
            countries.put(key, new Country(cleanName));
        }

        return countries.get(key);
    }

    // convierte una fecha de texto a un objeto LocalDate
    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, dateFormatter);
        } catch (Exception e) {
            throw new InvalidTournamentDataException(
                    "Invalid date: " + date
            );
        }
    }

    // verifica valores de estadisticas
    private int readSkill(JsonObject characteristicsJson, String skillName, String playerName) {
        int value = characteristicsJson.get(skillName).getAsInt();

        if (value < 0 || value > 100) {
            throw new InvalidTournamentDataException(
                    "Invalid " + skillName + " value for " + playerName + ": " + value
            );
        }

        return value;
    }

    private void registerDocument(String idType, int idNumber, String personName) {
        String document = idType.trim().toUpperCase() + "-" + idNumber;

        if (documents.contains(document)) {
            throw new InvalidTournamentDataException(
                    "Duplicated document for " + personName + ": " + document
            );
        }

        documents.add(document);
    }

    private void validateSquad(Team team) {
        int goalkeepers = 0;
        int defenders = 0;
        int midfielders = 0;
        int forwards = 0;

        for (Player player : team.getSquad()) {
            if (player.getPosition() == Position.GOALKEEPER) {
                goalkeepers++;
            } else if (player.getPosition() == Position.DEFENDER) {
                defenders++;
            } else if (player.getPosition() == Position.MIDFIELDER) {
                midfielders++;
            } else if (player.getPosition() == Position.FORWARD) {
                forwards++;
            }
        }

        if (goalkeepers != 2 ||
                defenders != 6 ||
                midfielders != 5 ||
                forwards != 5) {

            throw new InvalidTournamentDataException(
                    "Invalid squad composition for " + team.getName() +
                            ". Expected 2 goalkeepers, 6 defenders, 5 midfielders and 5 forwards."
            );
        }
    }
}