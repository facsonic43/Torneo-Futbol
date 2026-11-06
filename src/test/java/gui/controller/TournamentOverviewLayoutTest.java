package gui.controller;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.VBox;
import main.loader.TournamentDataLoader;
import model.match.Stadium;
import model.tournament.Tournament;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import service.TournamentExecutionService;
import service.TournamentSessionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TournamentOverviewLayoutTest {
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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void dividerCanBeGrabbedAndDraggedBothWaysWithQuarterFinalsLoaded(boolean lightTheme) throws Exception {
        FutureTask<Void> scenario = new FutureTask<>(() -> {
            var data = new TournamentDataLoader().load("tournament.json");
            Tournament tournament = new Tournament(data.getTeams(), data.getReferees());
            List<Stadium> stadiums = new ArrayList<>();
            for (int i = 1; i <= 16; i++) stadiums.add(new Stadium("UI Stadium " + i, i));
            tournament.drawGroups();
            tournament.generateGroupMatches(stadiums);
            new TournamentExecutionService(20261105L).simulateRemainingPhase(tournament, stadiums);
            assertEquals(4, tournament.getQuarterFinalFirstLegs().size());

            FXMLLoader tournamentLoader = new FXMLLoader(getClass().getResource("/view/tournament-view.fxml"));
            Parent tournamentView = tournamentLoader.load();
            TournamentController controller = tournamentLoader.getController();
            controller.setSession(new TournamentSessionService() {
                @Override public Tournament getTournament() { return tournament; }
                @Override public List<Stadium> getStadiums() { return stadiums; }
            });
            FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/view/main-view.fxml"));
            Parent root = mainLoader.load();
            MainController main = mainLoader.getController();
            main.getContentHost().getChildren().setAll(tournamentView);
            Scene scene = new Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
            if (lightTheme) root.getStyleClass().add("light-theme");
            root.applyCss();
            root.layout();

            SplitPane split = (SplitPane) root.lookup(".overview-split-pane");
            Node divider = split.lookup(".split-pane-divider");
            assertTrue(divider.getBoundsInLocal().getHeight() >= 10,
                    "The divider must have a usable hit area, not just a programmable position");
            Node grip = divider.lookup(".vertical-grabber");
            assertNotNull(grip);
            assertTrue(grip.getBoundsInLocal().getWidth() >= 30);
            assertTrue(grip.getBoundsInLocal().getHeight() > 0);
            VBox groups = (VBox) split.getItems().get(0);
            VBox knockout = (VBox) split.getItems().get(1);

            for (int distance : new int[]{80, -120}) {
                double groupsBefore = groups.getHeight();
                double knockoutBefore = knockout.getHeight();
                Point2D start = grip.localToScene(grip.getBoundsInLocal().getCenterX(),
                        grip.getBoundsInLocal().getCenterY());
                fireMouse(grip, MouseEvent.MOUSE_PRESSED, start, true);
                fireMouse(grip, MouseEvent.MOUSE_DRAGGED, start.add(0, distance), true);
                fireMouse(grip, MouseEvent.MOUSE_RELEASED, start.add(0, distance), false);
                root.layout();
                assertEquals(groupsBefore + distance, groups.getHeight(), 2);
                assertEquals(knockoutBefore - distance, knockout.getHeight(), 2);
            }
            double position = split.getDividerPositions()[0];
            controller.refreshAll();
            root.layout();
            assertEquals(position, split.getDividerPositions()[0], 0.01);
            return null;
        });
        Platform.runLater(scenario);
        scenario.get(30, TimeUnit.SECONDS);
    }

    private static void fireMouse(Node target, javafx.event.EventType<MouseEvent> type,
                                  Point2D point, boolean down) {
        Event.fireEvent(target, new MouseEvent(type, point.getX(), point.getY(), point.getX(), point.getY(),
                MouseButton.PRIMARY, 1, false, false, false, false,
                down, false, false, false, false, true,
                new PickResult(target, point.getX(), point.getY())));
    }
}
