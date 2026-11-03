package reports;

import model.participant.Player;
import model.participant.Position;

import java.nio.file.Path;
import java.util.Objects;

// Agrupa los filtros de posición y jugador y el directorio de fotografías.
// Las credenciales PDF incluyen a todos los participantes y utilizan únicamente el directorio de imágenes.

public record ReportOptions(Position position, Player player, Path photosDirectory) {
    public ReportOptions {
        Objects.requireNonNull(photosDirectory, "A photo directory is required.");
        if (player != null && position != null && player.getPosition() != position) {
            throw new IllegalArgumentException("The selected player does not play in the selected position.");
        }
    }

    public static ReportOptions defaults() {
        return new ReportOptions(null, null, Path.of("Images", "Person"));
    }
}
