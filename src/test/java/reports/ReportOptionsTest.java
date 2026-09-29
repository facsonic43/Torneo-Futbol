package reports;

import model.participant.Player;
import model.participant.Position;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportOptionsTest {
    @Test
    void defaultsIncludeEveryPlayerAndUseThePhotosDirectory() {
        ReportOptions options = ReportOptions.defaults();

        assertNull(options.position());
        assertNull(options.player());
        assertEquals(Path.of("photos"), options.photosDirectory());
    }

    @Test
    void selectedPlayerCanBeExportedWithAllPositions() {
        Player player = player(Position.FORWARD);
        ReportOptions options = new ReportOptions(null, player, Path.of("player-images"));

        assertSame(player, options.player());
        assertEquals(Path.of("player-images"), options.photosDirectory());
    }

    @Test
    void selectedPositionCanIncludeEveryMatchingPlayer() {
        ReportOptions options = new ReportOptions(Position.DEFENDER, null, Path.of("photos"));

        assertEquals(Position.DEFENDER, options.position());
        assertNull(options.player());
    }

    @Test
    void selectedPlayerMustMatchTheSelectedPosition() {
        Player goalkeeper = player(Position.GOALKEEPER);
        ReportOptions options = new ReportOptions(Position.GOALKEEPER, goalkeeper, Path.of("photos"));

        assertSame(goalkeeper, options.player());
        assertThrows(IllegalArgumentException.class,
                () -> new ReportOptions(Position.FORWARD, goalkeeper, Path.of("photos")));
    }

    @Test
    void photoDirectoryMustBeProvidedEvenWhenNoPhotosAreAvailable() {
        assertThrows(NullPointerException.class, () -> new ReportOptions(null, null, null));
    }

    private static Player player(Position position) {
        return new Player("Test player", 10000001, "DU", LocalDate.of(2000, 1, 1), null,
                0, 0, 0, 0, 0, 0) {
            @Override
            public double getOverall() { return 0; }

            @Override
            public Position getPosition() { return position; }
        };
    }
}
