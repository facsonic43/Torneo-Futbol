package gui.controller;

import gui.ErrorDialogs;
import gui.StatisticsReportView;
import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import model.match.Match;
import model.tournament.Tournament;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reports.ReportSection;
import service.TournamentSessionService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ErrorDialogsTest {
    private Stage owner;
    private PrintStream previousOut;
    private PrintStream previousErr;
    private ByteArrayOutputStream console;

    @BeforeAll
    static void startJavaFx() throws Exception {
        boolean linux = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux");
        assumeTrue(!linux || System.getenv("DISPLAY") != null || System.getenv("WAYLAND_DISPLAY") != null,
                "JavaFX requires a graphical display");
        CountDownLatch ready = new CountDownLatch(1);
        Runnable initialize = () -> {
            Platform.setImplicitExit(false);
            ready.countDown();
        };
        try {
            Platform.startup(initialize);
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(initialize);
        }
        assertTrue(ready.await(15, TimeUnit.SECONDS));
    }

    @BeforeEach
    void prepareWindowAndCaptureConsole() throws Exception {
        owner = onFxThread(() -> {
            Stage stage = new Stage();
            Scene scene = new Scene(new StackPane(), 600, 400);
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
            return stage;
        });
        previousOut = System.out;
        previousErr = System.err;
        console = new ByteArrayOutputStream();
        System.setOut(new PrintStream(console));
        System.setErr(new PrintStream(console));
    }

    @AfterEach
    void closeWindowsAndRestoreConsole() throws Exception {
        try {
            onFxThread(() -> {
                List.copyOf(Window.getWindows()).forEach(Window::hide);
                return null;
            });
        } finally {
            System.setOut(previousOut);
            System.setErr(previousErr);
        }
        assertEquals("", console.toString(), "Exceptions must not be printed to the console");
    }

    @Test
    void dialogPreservesOwnerThemeAndUnderlyingMessage() throws Exception {
        onFxThread(() -> {
            owner.getScene().getRoot().getStyleClass().add("light-theme");
            ErrorDialogs.show(owner, "Could not save", new InvocationTargetException(new IOException("Disk is full.")));
            Stage dialog = errorWindow();
            DialogPane pane = (DialogPane) dialog.getScene().getRoot();
            assertSame(owner, dialog.getOwner());
            assertEquals(Modality.WINDOW_MODAL, dialog.getModality());
            assertTrue(pane.getStyleClass().contains("light-theme"));
            assertTrue(pane.getStylesheets().containsAll(owner.getScene().getStylesheets()));
            assertMessage("Could not save", "Disk is full.");
            return null;
        });
    }

    @Test
    void exceptionWithoutMessageStillHasVisibleDetails() throws Exception {
        onFxThread(() -> {
            ErrorDialogs.show(owner, "Unexpected error", new IllegalStateException(" "));
            assertMessage("Unexpected error", "IllegalStateException");
            return null;
        });
    }

    @Test
    void failedSaveOpensADialogInsteadOfWritingOnlyToTheStatusBar() throws Exception {
        onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-view.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            Field session = MainController.class.getDeclaredField("session");
            session.setAccessible(true);
            session.set(controller, new TournamentSessionService() {
                @Override public void save() throws IOException { throw new IOException("Save permission denied."); }
            });
            owner.getScene().setRoot(root);
            root.applyCss();
            Button save = root.lookupAll("Button").stream().map(node -> (Button) node)
                    .filter(button -> "SAVE".equals(button.getText())).findFirst().orElseThrow();
            save.fire();
            assertMessage("Error saving tournament", "Save permission denied.");
            assertSame(owner, errorWindow().getOwner());
            return null;
        });
    }

    @Test
    void failedBackgroundReportOpensADialog() throws Exception {
        CountDownLatch failed = new CountDownLatch(1);
        onFxThread(() -> {
            StatisticsReportView report = new StatisticsReportView();
            owner.getScene().setRoot(report);
            report.showReport(new BrokenTournament(), ReportSection.TOP_SCORERS);
            report.getCurrentTask().addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, event -> failed.countDown());
            return null;
        });
        assertTrue(failed.await(15, TimeUnit.SECONDS));
        onFxThread(() -> {
            assertMessage("Could not display this report", "Report computation failed.");
            assertSame(owner, errorWindow().getOwner());
            return null;
        });
    }

    @Test
    void uncaughtBackgroundExceptionIsForwardedToTheFxThread() throws Exception {
        Thread.UncaughtExceptionHandler previousDefault = Thread.getDefaultUncaughtExceptionHandler();
        Thread.UncaughtExceptionHandler previousFx = onFxThread(() -> {
            Thread.UncaughtExceptionHandler previous = Thread.currentThread().getUncaughtExceptionHandler();
            ErrorDialogs.installUncaughtExceptionHandler(owner);
            return previous;
        });
        try {
            Thread worker = new Thread(() -> { throw new IllegalStateException("Unexpected worker failure."); });
            worker.start();
            worker.join(10000);
            assertFalse(worker.isAlive());
            onFxThread(() -> {
                assertMessage("Unexpected error", "Unexpected worker failure.");
                return null;
            });
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previousDefault);
            onFxThread(() -> {
                Thread.currentThread().setUncaughtExceptionHandler(previousFx);
                return null;
            });
        }
    }

    private static Stage errorWindow() {
        return Window.getWindows().stream()
                .filter(window -> window.getScene().getRoot().getStyleClass().contains("dialog-pane"))
                .map(Stage.class::cast).findFirst().orElseThrow();
    }

    private static void assertMessage(String heading, String detail) {
        String text = errorWindow().getScene().getRoot().lookupAll(".label").stream()
                .map(Label.class::cast).map(Label::getText).collect(Collectors.joining("\n"));
        assertTrue(text.contains(heading), text);
        assertTrue(text.contains(detail), text);
    }

    private static <T> T onFxThread(Callable<T> operation) throws Exception {
        FutureTask<T> task = new FutureTask<>(operation);
        Platform.runLater(task);
        return task.get(30, TimeUnit.SECONDS);
    }

    private static final class BrokenTournament extends Tournament {
        @Override public List<Match> getAllMatches() {
            throw new IllegalStateException("Report computation failed.");
        }
    }
}
