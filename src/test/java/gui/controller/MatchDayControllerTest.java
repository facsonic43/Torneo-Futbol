package gui.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import main.loader.TournamentData;
import main.loader.TournamentDataLoader;
import model.match.Match;
import model.match.Stadium;
import model.tournament.Tournament;
import model.tournament.TournamentStage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import service.TournamentExecutionService;
import service.TournamentSessionService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Verifica que la UI permita revisar los resultados antes de confirmar el avance,
// restaure el detalle original y mantenga la final disponible tras definir al campeón.

class MatchDayControllerTest {
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
    @ValueSource(strings = {"playSelectedButton", "simulateRoundButton", "simulatePhaseButton"})
    void everySimulationActionWaitsForConfirmation(String action) throws Exception {
        onFxThread(() -> {
            Fixture fixture = new Fixture();
            if (action.equals("playSelectedButton")) {
                while (fixture.execution.getCurrentPhaseMatches(fixture.tournament).stream()
                        .filter(match -> !match.isPlayed()).count() > 1) {
                    fixture.execution.playMatch(fixture.tournament,
                            fixture.execution.getCurrentPlayableMatches(fixture.tournament).getFirst(), fixture.stadiums);
                }
            } else if (action.equals("simulateRoundButton")) {
                fixture.execution.simulateCurrentMatchday(fixture.tournament, fixture.stadiums);
                fixture.execution.simulateCurrentMatchday(fixture.tournament, fixture.stadiums);
            }
            MatchDayController controller = fixture.loadController();
            List<Node> originalStructure = List.copyOf(controller.getNormalMatchPane().getChildren());
            Field field = MatchDayController.class.getDeclaredField(action);
            field.setAccessible(true);
            ((Button) field.get(controller)).fire();

            assertEquals(TournamentStage.QUARTER_FINALS, fixture.execution.getCurrentStage(fixture.tournament));
            assertWaitingForStage(controller, TournamentStage.GROUP_STAGE, 24);
            controller.refreshFromMain();
            assertWaitingForStage(controller, TournamentStage.GROUP_STAGE, 24);

            ((Button) controller.getMatchesBox().getChildren().getFirst()).fire();
            assertEquals(originalStructure, controller.getNormalMatchPane().getChildren());
            assertTrue(controller.getNormalMatchPane().isVisible());
            assertEquals(controller.getSelectedMatch().getHomeGoals() + "  -  "
                    + controller.getSelectedMatch().getAwayGoals(), controller.getSelectedScore().getText());
            assertFalse(controller.getEventsBox().getChildren().isEmpty());
            assertTrue(controller.isWaitingPhaseAcknowledgement());
            assertSimulationDisabled(controller);

            ((Button) controller.getMatchesBox().getChildren().getLast()).fire();
            findButton(controller.getNormalMatchPane(), "CONTINUE TO NEXT STAGE").fire();
            assertFalse(controller.isWaitingPhaseAcknowledgement());
            assertFalse(controller.isWaitingPenaltyResultAcknowledgement());
            assertNull(controller.getReviewedStage());
            assertEquals(8, controller.getMatchesBox().getChildren().size());
            assertEquals(originalStructure, controller.getNormalMatchPane().getChildren());
            assertFalse(controller.getPlaySelectedButton().isDisabled());
        });
    }

    @ParameterizedTest
    @EnumSource(value = TournamentStage.class, names = {"QUARTER_FINALS", "SEMI_FINALS"})
    void knockoutStagesRemainAvailableUntilConfirmed(TournamentStage stage) throws Exception {
        onFxThread(() -> {
            Fixture fixture = new Fixture();
            fixture.advanceTo(stage);
            MatchDayController controller = fixture.loadController();
            controller.getSimulatePhaseButton().fire();
            while (!fixture.execution.getPendingPenaltyMatches(fixture.tournament).isEmpty()) {
                Match pending = fixture.execution.getPendingPenaltyMatches(fixture.tournament).getFirst();
                selectMatch(controller, fixture.tournament, stage, pending);
                controller.getSimulatePenaltiesButton().fire();
            }
            int total = stage == TournamentStage.QUARTER_FINALS ? 8 : 4;
            assertWaitingForStage(controller, stage, total);
            findButton(controller.getNormalMatchPane(), "CONTINUE TO NEXT STAGE").fire();
            assertFalse(controller.isWaitingPhaseAcknowledgement());
            assertNotEquals(stage, fixture.execution.getCurrentStage(fixture.tournament));
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void finalCanBeReviewedAndBothDestinationsCanBeReusedIndependently(boolean penalties) throws Exception {
        onFxThread(() -> {
            Fixture fixture = new Fixture();
            fixture.advanceTo(TournamentStage.FINAL);
            Match finalMatch = fixture.tournament.getFinalMatch();
            if (penalties) {
                finalMatch.setHomeGoals(1);
                finalMatch.setAwayGoals(1);
                finalMatch.setPlayed(true);
                finalMatch.setStartingLineups(finalMatch.getHomeTeam().getSquad().subList(0, 11),
                        finalMatch.getAwayTeam().getSquad().subList(0, 11));
            }
            MatchDayController controller = fixture.loadController();
            if (penalties) {
                controller.getSimulatePenaltiesButton().fire();
            } else {
                controller.getPlaySelectedButton().fire();
                if (fixture.execution.requiresPenalties(finalMatch)) {
                    controller.getSimulatePenaltiesButton().fire();
                }
            }
            assertNotNull(fixture.tournament.getChampion());
            assertWaitingForStage(controller, TournamentStage.FINAL, 1);
            assertEquals(List.of(finalMatch),
                    TournamentExecutionService.getMatchesOfStage(fixture.tournament, TournamentStage.FINAL));
            ((Button) controller.getMatchesBox().getChildren().getFirst()).fire();
            assertSame(finalMatch, controller.getSelectedMatch());
            assertTrue(penalties ? controller.getPenaltyShootoutPane().isVisible()
                    : controller.getNormalMatchPane().isVisible() || controller.getPenaltyShootoutPane().isVisible());
            assertTrue(controller.isWaitingPhaseAcknowledgement());
            ((Button) controller.getMatchesBox().getChildren().getLast()).fire();
            String[] destinations = penalties
                    ? new String[]{"stats", "stats", "summary", "summary", "stats"}
                    : new String[]{"summary", "summary", "stats", "stats", "summary"};
            for (String destination : destinations) {
                Button summary = findButton(controller.getNormalMatchPane(), "VIEW TOURNAMENT SUMMARY");
                Button stats = findButton(controller.getNormalMatchPane(), "VIEW TOURNAMENT STATS");
                assertNotNull(summary);
                assertNotNull(stats);
                assertFalse(summary.isDisabled());
                assertFalse(stats.isDisabled());
                (destination.equals("stats") ? stats : summary).fire();
                assertEquals(destination, fixture.destination);
                assertFalse(controller.isWaitingPhaseAcknowledgement());
                assertFalse(controller.isWaitingPenaltyResultAcknowledgement());
                assertNull(controller.getReviewedStage());

                controller.refreshFromMain();
                assertEquals("FINAL - FINISHED", controller.getCurrentRoundLabel().getText());
                assertEquals("1 / 1 MATCHES", controller.getPhaseProgressLabel().getText());
                assertEquals(2, controller.getMatchesBox().getChildren().size());
                assertSimulationDisabled(controller);
                ((Button) controller.getMatchesBox().getChildren().getFirst()).fire();
                assertSame(finalMatch, controller.getSelectedMatch());
                ((Button) controller.getMatchesBox().getChildren().getLast()).fire();
            }

            MatchDayController reopened = fixture.loadController();
            assertFalse(reopened.isWaitingPhaseAcknowledgement());
            assertNotNull(findButton(reopened.getNormalMatchPane(), "VIEW TOURNAMENT SUMMARY"));
            assertNotNull(findButton(reopened.getNormalMatchPane(), "VIEW TOURNAMENT STATS"));
            assertSame(finalMatch, reopened.getSelectedMatch());
        });
    }

    @Test
    void penaltyAcknowledgementBlocksAllSimulationUntilAnotherSelection() throws Exception {
        onFxThread(() -> {
            Fixture fixture = new Fixture();
            fixture.advanceTo(TournamentStage.QUARTER_FINALS);
            fixture.execution.simulateCurrentMatchday(fixture.tournament, fixture.stadiums);
            Match pending = fixture.tournament.getQuarterFinalSecondLegs().getFirst();
            Match firstLeg = fixture.tournament.getQuarterFinalFirstLegs().getFirst();
            pending.setHomeGoals(firstLeg.getHomeGoals());
            pending.setAwayGoals(firstLeg.getAwayGoals());
            pending.setPlayed(true);
            pending.setStartingLineups(pending.getHomeTeam().getSquad().subList(0, 11),
                    pending.getAwayTeam().getSquad().subList(0, 11));
            assertTrue(fixture.execution.requiresPenalties(pending));
            MatchDayController controller = fixture.loadController();
            controller.getSimulatePenaltiesButton().fire();
            assertTrue(controller.isWaitingPenaltyResultAcknowledgement());
            assertFalse(controller.isWaitingPhaseAcknowledgement());
            assertTrue(controller.getPenaltyWinnerBox().isVisible());
            assertSimulationDisabled(controller);
            Match next = fixture.execution.getCurrentPlayableMatches(fixture.tournament).getFirst();
            selectMatch(controller, fixture.tournament, TournamentStage.QUARTER_FINALS, next);
            assertFalse(controller.isWaitingPenaltyResultAcknowledgement());
            assertFalse(controller.getPlaySelectedButton().isDisabled());
        });
    }

    private static void assertWaitingForStage(MatchDayController controller, TournamentStage stage, int count) {
        assertTrue(controller.isWaitingPhaseAcknowledgement());
        assertEquals(stage, controller.getReviewedStage());
        assertEquals(stage.getLabel() + " - FINISHED", controller.getCurrentRoundLabel().getText());
        assertEquals(count + " / " + count + " MATCHES", controller.getPhaseProgressLabel().getText());
        assertEquals(count + 1, controller.getMatchesBox().getChildren().size());
        assertEquals(1, controller.getNormalMatchPane().getChildren().size());
        assertSimulationDisabled(controller);
    }

    private static void assertSimulationDisabled(MatchDayController controller) {
        assertTrue(controller.getPlaySelectedButton().isDisabled());
        assertTrue(controller.getSimulateRoundButton().isDisabled());
        assertTrue(controller.getSimulatePhaseButton().isDisabled());
    }

    private static void selectMatch(MatchDayController controller, Tournament tournament,
                                    TournamentStage stage, Match match) {
        int index = TournamentExecutionService.getMatchesOfStage(tournament, stage).indexOf(match);
        ((Button) controller.getMatchesBox().getChildren().get(index)).fire();
    }

    private static Button findButton(Parent parent, String text) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Button button && button.getText().equals(text)) {
                return button;
            }
            if (child instanceof Parent nested) {
                Button button = findButton(nested, text);
                if (button != null) return button;
            }
        }
        return null;
    }

    private static void onFxThread(Scenario scenario) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            scenario.run();
            return null;
        });
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    private interface Scenario {
        void run() throws Exception;
    }

    private static class Fixture {
        private final Tournament tournament;
        private final List<Stadium> stadiums = new ArrayList<>();
        private final TournamentExecutionService execution = new TournamentExecutionService(20260920L);
        private String destination;

        private Fixture() {
            TournamentData data = new TournamentDataLoader().load("tournament.json");
            tournament = new Tournament(data.getTeams(), data.getReferees());
            for (int i = 1; i <= 16; i++) stadiums.add(new Stadium("UI Stadium " + i, i));
            tournament.drawGroups();
            tournament.generateGroupMatches(stadiums);
        }

        private void advanceTo(TournamentStage target) {
            while (execution.getCurrentStage(tournament) != target) {
                execution.simulateRemainingPhase(tournament, stadiums);
                while (!execution.getPendingPenaltyMatches(tournament).isEmpty()) {
                    execution.simulatePenalties(tournament,
                            execution.getPendingPenaltyMatches(tournament).getFirst(), stadiums);
                }
            }
        }

        private MatchDayController loadController() throws Exception {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/match-day-view.fxml"));
            loader.load();
            MatchDayController controller = loader.getController();
            Field service = MatchDayController.class.getDeclaredField("executionService");
            service.setAccessible(true);
            service.set(controller, execution);
            controller.setNavigationActions(() -> destination = "summary", () -> destination = "stats");
            controller.setSession(new TournamentSessionService() {
                @Override public Tournament getTournament() { return tournament; }
                @Override public List<Stadium> getStadiums() { return stadiums; }
            });
            return controller;
        }
    }
}
