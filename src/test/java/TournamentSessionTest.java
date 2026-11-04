import exceptions.InsufficientStadiumsException;
import model.match.Stadium;
import model.tournament.Tournament;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.TournamentSessionService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TournamentSessionTest {
    @TempDir
    Path directory;

    @Test
    void newTournamentDeletesPreviousSaveUntilItIsExplicitlySaved() throws Exception {
        TournamentSessionService session = createSession();
        session.createNewTournament();
        assertFalse(session.saveExists());
        session.save();
        Tournament previous = session.load();
        assertTrue(session.isLoadedFromSave());

        Tournament next = session.createNewTournament();

        assertNotSame(previous, next);
        assertSame(next, session.getTournament());
        assertFalse(session.isLoadedFromSave());
        assertFalse(Files.exists(directory.resolve("tournament.dat")));
        assertFalse(session.saveExists());
        assertThrows(IllegalStateException.class, session::load);
        assertFalse(createSession().saveExists());

        session.save();
        assertTrue(session.saveExists());
        TournamentSessionService restarted = createSession();
        assertTrue(restarted.saveExists());
        assertEquals(next.getGroups().size(), restarted.load().getGroups().size());
    }

    @Test
    void failedCreationPreservesPreviousSaveAndSession() throws Exception {
        TestSession session = createSession();
        session.createNewTournament();
        session.save();
        Tournament previous = session.load();
        Path save = directory.resolve("tournament.dat");
        byte[] savedData = Files.readAllBytes(save);
        session.failStadiumLoad = true;

        assertThrows(InsufficientStadiumsException.class, session::createNewTournament);

        assertArrayEquals(savedData, Files.readAllBytes(save));
        assertSame(previous, session.getTournament());
        assertTrue(session.isLoadedFromSave());
    }

    @Test
    void failedDeletionPreventsActivatingNewTournament() throws Exception {
        TournamentSessionService session = createSession();
        Tournament previous = session.createNewTournament();
        Path save = directory.resolve("tournament.dat");
        Files.createDirectory(save);
        Files.writeString(save.resolve("blocked.txt"), "Cannot delete a nonempty directory");

        assertThrows(IOException.class, session::createNewTournament);

        assertSame(previous, session.getTournament());
    }

    private TestSession createSession() throws Exception {
        TestSession session = new TestSession();
        List<Stadium> stadiums = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            stadiums.add(new Stadium("Session Stadium " + i, i));
        }
        setField(session, "stadiums", stadiums);
        setField(session, "saveFile", directory.resolve("tournament.dat").toString());
        return session;
    }

    private void setField(TournamentSessionService session, String name, Object value) throws Exception {
        Field field = TournamentSessionService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(session, value);
    }

    private static class TestSession extends TournamentSessionService {
        private boolean failStadiumLoad;

        @Override
        public void refreshStadiums() throws InsufficientStadiumsException {
            // Use isolated fixtures instead of connecting to the database.
            if (failStadiumLoad) {
                throw new InsufficientStadiumsException(13, 0);
            }
        }
    }
}
