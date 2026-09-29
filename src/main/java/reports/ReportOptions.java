package reports;

import model.participant.Player;
import model.participant.Position;

import java.nio.file.Path;
import java.util.Objects;

/** Filters for player profiles (section VIII); the other reports always include everyone. */
public record ReportOptions(Position position, Player player, Path photosDirectory) {
    public ReportOptions {
        Objects.requireNonNull(photosDirectory, "A photo directory is required.");
        if (player != null && position != null && player.getPosition() != position) {
            throw new IllegalArgumentException("The selected player does not play in the selected position.");
        }
    }

    public static ReportOptions defaults() {
        return new ReportOptions(null, null, Path.of("photos"));
    }
}
