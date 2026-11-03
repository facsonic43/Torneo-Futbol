package persistence;

import model.tournament.Tournament;
import model.tournament.TournamentStage;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guarda y recupera el estado completo del campeonato mediante serialización de Java.
 * Permite comprobar y eliminar partidas guardadas y determinar la etapa alcanzada por el torneo.
 */
public class TournamentPersistenceService {
    private static final String INVALID_SAVE = "The save file does not contain a valid tournament.";

    public void saveTournament(Tournament tournament, String fileName) throws IOException {
        if (tournament == null) {
            throw new IllegalArgumentException("The tournament cannot be null.");
        }

        Path path = Path.of(fileName);

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
            output.writeObject(tournament);
        }
    }

    public Tournament loadTournament(String fileName) throws IOException,
            ClassNotFoundException {
        Path path = Path.of(fileName);

        if (!Files.exists(path)) {
            throw new IOException("Tournament save file not found: " + fileName);
        }

        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(path.toFile()))) {
            Object object = input.readObject();

            try {
                Tournament tournament = (Tournament) object;

                if (tournament == null) {
                    throw new IOException(INVALID_SAVE);
                }

                return tournament;
            } catch (ClassCastException exception) {
                throw new IOException(INVALID_SAVE, exception);
            }
        }
    }

    public boolean saveExists(String fileName) {
        return Files.exists(Path.of(fileName));
    }

    public void deleteSave(String fileName) throws IOException {
        Files.deleteIfExists(Path.of(fileName));
    }

    public TournamentStage getStage(Tournament tournament) {
        if (tournament == null) {
            return TournamentStage.NOT_STARTED;
        }

        if (tournament.getChampion() != null) {
            return TournamentStage.FINISHED;
        }

        if (tournament.getFinalMatch() != null) {
            return TournamentStage.FINAL;
        }

        if (!tournament.getSemiFinalSecondLegs().isEmpty()) {
            return TournamentStage.SEMI_FINALS;
        }

        if (!tournament.getQuarterFinalSecondLegs().isEmpty()) {
            return TournamentStage.QUARTER_FINALS;
        }

        if (!tournament.getGroups().isEmpty()) {
            return TournamentStage.GROUP_STAGE;
        }

        return TournamentStage.NOT_STARTED;
    }
}
