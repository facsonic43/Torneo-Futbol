package gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.match.Event;
import model.match.Match;
import model.tournament.Tournament;
import model.tournament.TournamentStage;
import service.TournamentExecutionService;
import service.TournamentSessionService;

import java.util.List;

/*
 * Controla la ejecución manual del campeonato.
 * Permite seleccionar un partido, ejecutar la ronda actual
 * o completar toda la fase respetando el orden cronológico.
 */
public class MatchDayController {

    @FXML
    private Label currentRoundLabel;

    @FXML
    private Label phaseProgressLabel;

    @FXML
    private VBox matchesBox;

    @FXML
    private Label selectedMatchTitle;

    @FXML
    private Label selectedScore;

    @FXML
    private Label selectedDate;

    @FXML
    private Label selectedStadium;

    @FXML
    private Label selectedReferee;

    @FXML
    private Label selectedResolution;

    @FXML
    private VBox eventsBox;

    @FXML
    private Button playSelectedButton;

    @FXML
    private Button simulateRoundButton;

    @FXML
    private Button simulatePhaseButton;

    @FXML
    private Label localStatus;

    private TournamentSessionService session;
    private TournamentExecutionService executionService;
    private Tournament tournament;
    private Match selectedMatch;

    public MatchDayController() {

        executionService =
                new TournamentExecutionService();
    }

    public void setSession(
            TournamentSessionService session) {

        this.session =
                session;

        tournament =
                session.getTournament();

        selectedMatch =
                null;

        refresh();
    }

    /*
     * MainController lo usa cada vez que el usuario
     * vuelve a ingresar a Match Day.
     */
    public void refreshFromMain() {

        if (session == null) {
            return;
        }

        tournament =
                session.getTournament();

        refresh();
    }

    private void refresh() {

        if (tournament == null) {
            return;
        }

        currentRoundLabel.setText(
                executionService
                        .getCurrentRoundName(
                                tournament
                        )
        );

        int played =
                executionService
                        .getPlayedMatchesInCurrentPhase(
                                tournament
                        );

        int total =
                executionService
                        .getTotalMatchesInCurrentPhase(
                                tournament
                        );

        phaseProgressLabel.setText(
                played
                        + " / "
                        + total
                        + " MATCHES"
        );

        refreshMatchList();

        updateActionButtons();
    }

    private void refreshMatchList() {

        matchesBox
                .getChildren()
                .clear();

        List<Match> matches =
                executionService
                        .getCurrentPhaseMatches(
                                tournament
                        );

        if (matches.isEmpty()) {

            Label empty =
                    new Label(
                            "No matches available."
                    );

            empty.getStyleClass()
                    .add(
                            "muted-text"
                    );

            matchesBox
                    .getChildren()
                    .add(
                            empty
                    );

            selectedMatch =
                    null;

            refreshSelectedMatch();

            return;
        }

        if (selectedMatch == null
                || !matches.contains(
                selectedMatch
        )) {

            selectedMatch =
                    findDefaultMatch(
                            matches
                    );
        }

        for (Match match :
                matches) {

            Button button =
                    createMatchButton(
                            match
                    );

            matchesBox
                    .getChildren()
                    .add(
                            button
                    );
        }

        refreshSelectedMatch();
    }

    private Match findDefaultMatch(
            List<Match> matches) {

        List<Match> playable =
                executionService
                        .getCurrentPlayableMatches(
                                tournament
                        );

        if (!playable.isEmpty()) {

            return playable.get(
                    0
            );
        }

        return matches.get(
                0
        );
    }

    private Button createMatchButton(
            Match match) {

        String status =
                getMatchStatus(
                        match
                );

        String text =
                match.getMatchDate()
                        + "   "
                        + match.getHomeTeam()
                        .getName()
                        + "  vs  "
                        + match.getAwayTeam()
                        .getName()
                        + "     "
                        + status;

        Button button =
                new Button(
                        text
                );

        button.setMaxWidth(
                Double.MAX_VALUE
        );

        button.getStyleClass()
                .add(
                        "match-list-item"
                );

        if (match.isPlayed()) {

            button.getStyleClass()
                    .add(
                            "match-list-finished"
                    );

        } else if (executionService
                .isMatchPlayable(
                        tournament,
                        match
                )) {

            button.getStyleClass()
                    .add(
                            "match-list-playable"
                    );

        } else {

            button.getStyleClass()
                    .add(
                            "match-list-locked"
                    );
        }

        if (match == selectedMatch) {

            button.getStyleClass()
                    .add(
                            "match-list-selected"
                    );
        }

        button.setOnAction(
                event -> {

                    selectedMatch =
                            match;

                    refreshMatchList();
                }
        );

        return button;
    }

    private String getMatchStatus(
            Match match) {

        if (match.isPlayed()) {

            return "FINISHED";
        }

        if (executionService
                .isMatchPlayable(
                        tournament,
                        match
                )) {

            return "PLAY";
        }

        return "LOCKED";
    }

    private void refreshSelectedMatch() {

        eventsBox
                .getChildren()
                .clear();

        if (selectedMatch == null) {

            selectedMatchTitle.setText(
                    "No match selected"
            );

            selectedScore.setText(
                    "-"
            );

            selectedDate.setText(
                    "-"
            );

            selectedStadium.setText(
                    "-"
            );

            selectedReferee.setText(
                    "-"
            );

            selectedResolution.setText(
                    "-"
            );

            updateActionButtons();

            return;
        }

        selectedMatchTitle.setText(
                selectedMatch
                        .getHomeTeam()
                        .getName()
                        + "  VS  "
                        + selectedMatch
                        .getAwayTeam()
                        .getName()
        );

        if (selectedMatch.isPlayed()) {

            selectedScore.setText(
                    selectedMatch
                            .getHomeGoals()
                            + "  -  "
                            + selectedMatch
                            .getAwayGoals()
            );

        } else {

            selectedScore.setText(
                    "VS"
            );
        }

        selectedDate.setText(
                "DATE  "
                        + selectedMatch
                        .getMatchDate()
        );

        if (selectedMatch
                .getStadium()
                != null) {

            selectedStadium.setText(
                    "STADIUM  "
                            + selectedMatch
                            .getStadium()
                            .getName()
            );

        } else {

            selectedStadium.setText(
                    "STADIUM  -"
            );
        }

        if (selectedMatch
                .getReferee()
                != null) {

            selectedReferee.setText(
                    "REFEREE  "
                            + selectedMatch
                            .getReferee()
                            .getName()
            );

        } else {

            selectedReferee.setText(
                    "REFEREE  -"
            );
        }

        if (selectedMatch.isPlayed()
                && selectedMatch.isKnockout()) {

            selectedResolution.setText(
                    "RESOLUTION  "
                            + selectedMatch
                            .getResolutionCriteria()
            );

        } else if (selectedMatch.isPlayed()) {

            selectedResolution.setText(
                    "STATUS  FINISHED"
            );

        } else if (executionService
                .isMatchPlayable(
                        tournament,
                        selectedMatch
                )) {

            selectedResolution.setText(
                    "STATUS  READY"
            );

        } else {

            selectedResolution.setText(
                    "STATUS  LOCKED"
            );
        }

        if (selectedMatch
                .getHomePenalties()
                != null) {

            Label penalties =
                    new Label(
                            "PENALTIES  "
                                    + selectedMatch
                                    .getHomePenalties()
                                    + " - "
                                    + selectedMatch
                                    .getAwayPenalties()
                    );

            penalties.getStyleClass()
                    .add(
                            "penalty-result"
                    );

            eventsBox
                    .getChildren()
                    .add(
                            penalties
                    );
        }

        if (selectedMatch
                .getEvents()
                .isEmpty()) {

            Label empty =
                    new Label(
                            selectedMatch.isPlayed()
                                    ? "No events registered."
                                    : "Match has not been played yet."
                    );

            empty.getStyleClass()
                    .add(
                            "muted-text"
                    );

            eventsBox
                    .getChildren()
                    .add(
                            empty
                    );

        } else {

            for (Event event :
                    selectedMatch
                            .getEvents()) {

                Label eventLabel =
                        new Label(
                                event.getDescription()
                        );

                eventLabel.setWrapText(
                        true
                );

                eventLabel.getStyleClass()
                        .add(
                                "event-row"
                        );

                eventsBox
                        .getChildren()
                        .add(
                                eventLabel
                        );
            }
        }

        updateActionButtons();
    }

    private void updateActionButtons() {

        boolean playable =
                selectedMatch != null
                        && executionService
                        .isMatchPlayable(
                                tournament,
                                selectedMatch
                        );

        playSelectedButton.setDisable(
                !playable
        );

        TournamentStage stage =
                executionService
                        .getCurrentStage(
                                tournament
                        );

        List<Match> playableMatches =
                executionService
                        .getCurrentPlayableMatches(
                                tournament
                        );

        boolean finished =
                stage
                        == TournamentStage.FINISHED;

        simulateRoundButton.setDisable(
                finished
                        || playableMatches.isEmpty()
        );

        simulatePhaseButton.setDisable(
                finished
        );

        if (stage
                == TournamentStage.GROUP_STAGE) {

            simulateRoundButton.setText(
                    "SIMULATE MATCHDAY"
            );

        } else if (stage
                == TournamentStage.QUARTER_FINALS
                || stage
                == TournamentStage.SEMI_FINALS) {

            simulateRoundButton.setText(
                    "SIMULATE CURRENT LEG"
            );

        } else {

            simulateRoundButton.setText(
                    "SIMULATE CURRENT ROUND"
            );
        }
    }

    @FXML
    private void handlePlaySelected() {

        if (selectedMatch == null) {
            return;
        }

        try {

            Match playedMatch =
                    selectedMatch;

            executionService.playMatch(
                    tournament,
                    playedMatch,
                    session.getStadiums()
            );

            localStatus.setText(
                    "Match finished: "
                            + playedMatch
                            .getHomeTeam()
                            .getName()
                            + " "
                            + playedMatch
                            .getHomeGoals()
                            + " - "
                            + playedMatch
                            .getAwayGoals()
                            + " "
                            + playedMatch
                            .getAwayTeam()
                            .getName()
            );

            selectedMatch =
                    playedMatch;

            refresh();

        } catch (Exception exception) {

            localStatus.setText(
                    "Error: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    @FXML
    private void handleSimulateRound() {

        try {

            int simulated =
                    executionService
                            .simulateCurrentMatchday(
                                    tournament,
                                    session.getStadiums()
                            );

            localStatus.setText(
                    simulated
                            + " match(es) simulated."
            );

            selectedMatch =
                    null;

            refresh();

        } catch (Exception exception) {

            localStatus.setText(
                    "Error: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    @FXML
    private void handleSimulatePhase() {

        try {

            TournamentStage previousStage =
                    executionService
                            .getCurrentStage(
                                    tournament
                            );

            int simulated =
                    executionService
                            .simulateRemainingPhase(
                                    tournament,
                                    session.getStadiums()
                            );

            TournamentStage newStage =
                    executionService
                            .getCurrentStage(
                                    tournament
                            );

            localStatus.setText(
                    simulated
                            + " match(es) simulated. "
                            + previousStage
                            + " -> "
                            + newStage
            );

            selectedMatch =
                    null;

            refresh();

        } catch (Exception exception) {

            localStatus.setText(
                    "Error: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }
}