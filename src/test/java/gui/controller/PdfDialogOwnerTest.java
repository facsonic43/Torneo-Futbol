package gui.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PdfDialogOwnerTest {
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

    @Test
    void pdfUsesTheMainWindowFromEveryScreenWithoutChangingTheCurrentView() throws Exception {
        FutureTask<Void> scenario = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-view.fxml"));
            Parent root = loader.load();
            MainController main = loader.getController();
            StackPane host = (StackPane) field(main, "contentHost");
            HBox reportsPanel = new HBox();
            StackPane tournamentView = new StackPane(reportsPanel);
            StackPane matchDayView = new StackPane();
            StackPane databaseView = new StackPane();
            RecordingTournamentController tournament = new RecordingTournamentController();
            setField(main, "tournamentView", tournamentView);
            setField(main, "matchDayView", matchDayView);
            setField(main, "databaseView", databaseView);
            setField(main, "tournamentController", tournament);
            setField(main, "matchDayController", new MatchDayController() {
                @Override public void refreshFromMain() { }
            });
            setField(main, "databaseController", new DatabaseController() {
                @Override public void refreshFromMain() { }
            });
            Stage window = new Stage();
            Scene scene = new Scene(root);
            window.setScene(scene);
            try {
                MenuButton reports = (MenuButton) field(main, "reportsButton");
                var exportItem = reports.getItems().get(1);
                for (String button : new String[]{"tournamentButton", "matchDayButton", "databaseButton"}) {
                    ((Button) field(main, button)).fire();
                    Parent currentView = (Parent) host.getChildren().getFirst();
                    if (!button.equals("tournamentButton")) assertNull(reportsPanel.getScene());
                    exportItem.fire();
                    assertSame(window, tournament.lastOwner);
                    assertSame(currentView, host.getChildren().getFirst());
                }
                reports.getItems().getFirst().fire();
                assertSame(scene, reportsPanel.getScene());
                exportItem.fire();
                assertSame(window, tournament.lastOwner);
                assertSame(tournamentView, host.getChildren().getFirst());
                assertEquals(4, tournament.exports);
            } finally {
                window.close();
            }
            return null;
        });
        Platform.runLater(scenario);
        scenario.get(30, TimeUnit.SECONDS);
    }

    private static Object field(MainController controller, String name) throws Exception {
        Field field = MainController.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(controller);
    }

    private static void setField(MainController controller, String name, Object value) throws Exception {
        Field field = MainController.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private static final class RecordingTournamentController extends TournamentController {
        private Window lastOwner;
        private int exports;

        @Override public void showOverview() { }
        @Override public void showReports() { }
        @Override public void refreshAll() { }
        @Override public void handleShowReportsPane(ActionEvent event) { }

        @Override public void handleGeneratePdf(Window owner) {
            lastOwner = owner;
            exports++;
        }
    }
}
