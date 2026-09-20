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

/*
 * Se encarga de guardar y recuperar el estado completo del campeonato.
 * Utiliza serialización de Java para poder cerrar la aplicación
 * y continuar posteriormente desde la misma etapa.
guardar
cargar
comprobar existencia
borrar
identificar etapa
 */
public class TournamentPersistenceService {

    // Guarda el campeonato completo en un archivo binario.
    public void saveTournament(
            Tournament tournament,
            String fileName) throws IOException {

        if (tournament == null) {
            throw new IllegalArgumentException(
                    "The tournament cannot be null."
            );
        }

        Path path =
                Path.of(fileName);

        if (path.getParent() != null) {
            Files.createDirectories(
                    path.getParent()
            );
        }

        try (ObjectOutputStream output =
                     new ObjectOutputStream(
                             new FileOutputStream(
                                     path.toFile()
                             )
                     )) {

            output.writeObject(
                    tournament
            );
        }
    }

    // Recupera un campeonato previamente guardado.
    public Tournament loadTournament(
            String fileName)
            throws IOException,
            ClassNotFoundException {

        Path path =
                Path.of(fileName);

        if (!Files.exists(path)) {
            throw new IOException(
                    "Tournament save file not found: "
                            + fileName
            );
        }

        try (ObjectInputStream input =
                     new ObjectInputStream(
                             new FileInputStream(
                                     path.toFile()
                             )
                     )) {

            Object object =
                    input.readObject();

            if (!(object instanceof Tournament)) {
                throw new IOException(
                        "The save file does not contain a valid tournament."
                );
            }

            return (Tournament) object;
        }
    }

    // Indica si existe una partida guardada.
    public boolean saveExists(
            String fileName) {

        return Files.exists(
                Path.of(fileName)
        );
    }

    // Elimina una partida guardada.
    public void deleteSave(
            String fileName)
            throws IOException {

        Files.deleteIfExists(
                Path.of(fileName)
        );
    }

    // Determina la etapa actual observando el estado real del campeonato.
    public TournamentStage getStage(
            Tournament tournament) {

        if (tournament == null) {
            return TournamentStage.NOT_STARTED;
        }

        if (tournament.getChampion() != null) {
            return TournamentStage.FINISHED;
        }

        if (tournament.getFinalMatch() != null) {
            return TournamentStage.FINAL;
        }

        if (!tournament
                .getSemiFinalSecondLegs()
                .isEmpty()) {

            return TournamentStage.SEMI_FINALS;
        }

        if (!tournament
                .getQuarterFinalSecondLegs()
                .isEmpty()) {

            return TournamentStage.QUARTER_FINALS;
        }

        if (!tournament
                .getGroups()
                .isEmpty()) {

            return TournamentStage.GROUP_STAGE;
        }

        return TournamentStage.NOT_STARTED;
    }
}