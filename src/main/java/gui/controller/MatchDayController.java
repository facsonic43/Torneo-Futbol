package gui.controller;

import gui.MatchEventsView;
import gui.PenaltyKicksView;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.match.Match;
import model.match.PenaltyTaken;
import model.match.ResolutionCriterion;
import model.participant.Player;
import model.participant.Team;
import model.tournament.Tournament;
import model.tournament.TournamentStage;
import service.TournamentExecutionService;
import service.TournamentSessionService;

import java.util.List;

import static gui.MatchFormat.penaltyContext;

// Controla la ejecución de partidos y fases desde la pantalla Match Day.
// Muestra el partido seleccionado, sus incidencias y el avance de la ronda.
// Cuando queda una tanda pendiente, presenta los pateadores, arqueros y resultados de los penales.

public class MatchDayController {
    @FXML
    private Label currentRoundLabel;

    @FXML
    private Label phaseProgressLabel;

    @FXML
    private VBox matchesBox;
    @FXML
    private VBox normalMatchPane;

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
    private VBox penaltyShootoutPane;

    @FXML
    private Label penaltyRoundLabel;

    @FXML
    private Label penaltyStatusLabel;

    @FXML
    private Label penaltyContextLabel;

    @FXML
    private Label penaltyHomeTeamLabel;

    @FXML
    private Label penaltyAwayTeamLabel;

    @FXML
    private Label penaltyHomeGoalkeeperLabel;

    @FXML
    private Label penaltyAwayGoalkeeperLabel;

    @FXML
    private Label penaltyHomeScoreLabel;

    @FXML
    private Label penaltyAwayScoreLabel;

    @FXML
    private HBox penaltyColumnHeaderBox;

    @FXML
    private HBox penaltyHomeKicksBox;

    @FXML
    private HBox penaltyAwayKicksBox;

    @FXML
    private VBox penaltyKickDetailsBox;
    private PenaltyKicksView penaltyKicksView;
    private MatchEventsView matchEventsView;

    @FXML
    private Button simulatePenaltiesButton;

    @FXML
    private VBox penaltyWinnerBox;

    @FXML
    private Label penaltyWinnerLabel;

    @FXML
    private Label penaltyWinnerDetailLabel;

    @FXML
    private Button continueAfterPenaltiesButton;
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

    private boolean waitingPenaltyResultAcknowledgement;

    public MatchDayController() {
        executionService = new TournamentExecutionService();
        waitingPenaltyResultAcknowledgement = false;
    }

    public void setSession(TournamentSessionService session) {
        this.session = session;
        if (matchEventsView == null) {
            matchEventsView = new MatchEventsView(eventsBox);
        }
        if (penaltyKicksView == null) {
            penaltyKicksView = new PenaltyKicksView(penaltyColumnHeaderBox, penaltyHomeKicksBox, penaltyAwayKicksBox,
                    penaltyKickDetailsBox);
        }
        tournament = session.getTournament();
        selectedMatch = null;
        waitingPenaltyResultAcknowledgement = false;
        refresh();
    }

    public void refreshFromMain() {
        if (session == null) {
            return;
        }
        tournament = session.getTournament();
        selectedMatch = null;
        waitingPenaltyResultAcknowledgement = false;
        refresh();
    }

    private void refresh() {
        if (tournament == null) {
            return;
        }
        currentRoundLabel.setText(executionService.getCurrentRoundName(tournament));
        int played = executionService.getPlayedMatchesInCurrentPhase(tournament);
        int total = executionService.getTotalMatchesInCurrentPhase(tournament);
        int pendingPenalties = executionService.getPendingPenaltyMatches(tournament).size();
        if (pendingPenalties > 0) {
            phaseProgressLabel.setText(played + " / " + total + " MATCHES · " + pendingPenalties + " PENALTIES PENDING");
        } else {
            phaseProgressLabel.setText(played + " / " + total + " MATCHES");
        }
        refreshMatchList();
        updateActionButtons();
    }

    private void refreshMatchList() {
        matchesBox.getChildren().clear();
        List<Match> matches = executionService.getCurrentPhaseMatches(tournament);
        if (matches.isEmpty()) {
            Label empty = new Label("No matches available.");
            empty.getStyleClass().add("muted-text");
            matchesBox.getChildren().add(empty);
            selectedMatch = null;
            refreshSelectedMatch();
            return;
        }
        if (selectedMatch == null || !matches.contains(selectedMatch)) {
            selectedMatch = findDefaultMatch(matches);
        }
        for (Match match : matches) {
            matchesBox.getChildren().add(createMatchButton(match));
        }
        refreshSelectedMatch();
    }

    private Match findDefaultMatch(List<Match> matches) {
        List<Match> pendingPenalties = executionService.getPendingPenaltyMatches(tournament);
        if (!pendingPenalties.isEmpty()) {
            return pendingPenalties.get(0);
        }
        List<Match> playableMatches = executionService.getCurrentPlayableMatches(tournament);
        if (!playableMatches.isEmpty()) {
            return playableMatches.get(0);
        }
        return matches.get(0);
    }

    private Button createMatchButton(Match match) {
        String text = match.getMatchDate() + "   " + match.getHomeTeam().getName() + "  vs  " + match.getAwayTeam()
                .getName() + "     " + getMatchStatus(match);
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("match-list-item");
        if (executionService.requiresPenalties(match)) {
            button.getStyleClass().add("match-list-playable");
        } else if (match.isPlayed()) {
            button.getStyleClass().add("match-list-finished");
        } else if (executionService.isMatchPlayable(tournament, match)) {
            button.getStyleClass().add("match-list-playable");
        } else {
            button.getStyleClass().add("match-list-locked");
        }
        if (match == selectedMatch) {
            button.getStyleClass().add("match-list-selected");
        }
        button.setOnAction(event -> {
            waitingPenaltyResultAcknowledgement = false;
            selectedMatch = match;
            refreshMatchList();
        });
        return button;
    }

    private String getMatchStatus(Match match) {
        if (executionService.requiresPenalties(match)) {
            return "PENALTIES";
        }
        if (match.isPlayed()) {
            if (hasPenaltyResult(match)) {
                return "PENALTIES FINISHED";
            }
            return "FINISHED";
        }
        if (executionService.isMatchPlayable(tournament, match)) {
            return "PLAY";
        }
        return "LOCKED";
    }

    private void refreshSelectedMatch() {
        if (selectedMatch == null) {
            showNormalMatchPane();
            selectedMatchTitle.setText("No match selected");
            selectedScore.setText("-");
            selectedDate.setText("-");
            selectedStadium.setText("-");
            selectedReferee.setText("-");
            setResolution("-", "purple-chip");
            eventsBox.getChildren().clear();
            updateActionButtons();
            return;
        }
        if (executionService.requiresPenalties(selectedMatch) || hasPenaltyResult(selectedMatch)) {
            showPenaltyShootoutPane();
            renderPenaltyShootout(selectedMatch);
        } else {
            showNormalMatchPane();
            renderNormalMatch(selectedMatch);
        }
        updateActionButtons();
    }

    private void renderNormalMatch(Match match) {
        selectedMatchTitle.setText(match.getHomeTeam().getName() + "  VS  " + match.getAwayTeam().getName());
        selectedScore.setText(formatScore(match));
        selectedDate.setText("DATE  " + match.getMatchDate());
        selectedStadium.setText("STADIUM  " + (match.getStadium() == null ? "-" : match.getStadium().getName()));
        selectedReferee.setText("REFEREE  " + (match.getReferee() == null ? "-" : match.getReferee().getName()));
        if (match.isPlayed() && (match.isKnockout() || match.isForfeited())) {
            setResolution("RESOLUTION  " + match.getResolutionCriterion().getDescription(), "ready-chip");
        } else if (match.isPlayed()) {
            setResolution("STATUS  FINISHED", "ready-chip");
        } else if (executionService.isMatchPlayable(tournament, match)) {
            setResolution("STATUS  READY", "ready-chip");
        } else {
            setResolution("STATUS  BLOCKED", "blocked-chip");
        }
        matchEventsView.render(match);
    }

    private String formatScore(Match match) {
        if (!match.isPlayed()) {
            return "VS";
        }
        return match.getHomeGoals() + "  -  " + match.getAwayGoals();
    }

    private void showNormalMatchPane() {
        normalMatchPane.setVisible(true);
        normalMatchPane.setManaged(true);
        penaltyShootoutPane.setVisible(false);
        penaltyShootoutPane.setManaged(false);
    }

    private void showPenaltyShootoutPane() {
        normalMatchPane.setVisible(false);
        normalMatchPane.setManaged(false);
        penaltyShootoutPane.setVisible(true);
        penaltyShootoutPane.setManaged(true);
    }

    private void renderPenaltyShootout(Match match) {
        boolean finished = hasPenaltyResult(match);
        penaltyRoundLabel.setText(tournament.getStageOf(match).getLabel());
        penaltyStatusLabel.setText(finished ? "SHOOTOUT FINISHED" : "PENALTIES REQUIRED");
        penaltyHomeTeamLabel.setText(match.getHomeTeam().getName());
        penaltyAwayTeamLabel.setText(match.getAwayTeam().getName());
        Player homeGoalkeeper = match.getShootoutGoalkeeper(match.getHomeTeam());
        Player awayGoalkeeper = match.getShootoutGoalkeeper(match.getAwayTeam());
        penaltyHomeGoalkeeperLabel.setText("GK: " + (homeGoalkeeper == null ? "-" : homeGoalkeeper.getName()));
        penaltyAwayGoalkeeperLabel.setText("GK: " + (awayGoalkeeper == null ? "-" : awayGoalkeeper.getName()));
        penaltyContextLabel.setText(penaltyContext(match));
        List<PenaltyTaken> homePenalties = match.getPenaltyKicks(match.getHomeTeam());
        List<PenaltyTaken> awayPenalties = match.getPenaltyKicks(match.getAwayTeam());
        int homeScore = match.getHomePenalties()
                == null ? 0 : match.getHomePenalties();
        int awayScore = match.getAwayPenalties()
                == null ? 0 : match.getAwayPenalties();
        penaltyHomeScoreLabel.setText(String.valueOf(homeScore));
        penaltyAwayScoreLabel.setText(String.valueOf(awayScore));
        penaltyKicksView.renderColumns(homePenalties, awayPenalties, finished);
        penaltyKicksView.renderKickDetails(homePenalties, awayPenalties);
        simulatePenaltiesButton.setVisible(!finished);
        simulatePenaltiesButton.setManaged(!finished);
        penaltyWinnerBox.setVisible(finished);
        penaltyWinnerBox.setManaged(finished);
        continueAfterPenaltiesButton.setVisible(finished);
        continueAfterPenaltiesButton.setManaged(finished);
        if (finished) {
            Team winner = match.getWinner();
            if (winner != null) {
                penaltyWinnerLabel.setText(winner.getName());
                if (match == tournament.getFinalMatch()) {
                    penaltyWinnerDetailLabel.setText("CHAMPION · WINS " + homeScore + " - " + awayScore
                            + " ON PENALTIES");
                } else {
                    penaltyWinnerDetailLabel.setText("ADVANCES · WINS " + homeScore + " - " + awayScore
                            + " ON PENALTIES");
                }
            }
            configureContinueButton();
        }
    }

    private boolean hasPenaltyResult(Match match) {
        return match != null && match.getResolutionCriterion() == ResolutionCriterion.PENALTIES;
    }

    private void setResolution(String text, String styleClass) {
        selectedResolution.setText(text);
        selectedResolution.getStyleClass().removeAll("ready-chip", "blocked-chip", "purple-chip");
        selectedResolution.getStyleClass().add(styleClass);
    }

    private void updateActionButtons() {
        boolean playable = selectedMatch != null && executionService.isMatchPlayable(tournament, selectedMatch);
        playSelectedButton.setDisable(!playable);
        if (waitingPenaltyResultAcknowledgement) {
            simulateRoundButton.setDisable(true);
            simulatePhaseButton.setDisable(true);
            return;
        }
        TournamentStage stage = executionService.getCurrentStage(tournament);
        List<Match> playableMatches = executionService.getCurrentPlayableMatches(tournament);
        boolean finished = stage
                == TournamentStage.FINISHED;
        simulateRoundButton.setDisable(finished || playableMatches.isEmpty());
        simulatePhaseButton.setDisable(finished || !executionService.hasUnplayedMatchesInCurrentPhase(tournament));
        if (stage == TournamentStage.GROUP_STAGE) {
            simulateRoundButton.setText("SIMULATE MATCHDAY");
        } else if (stage
                == TournamentStage.QUARTER_FINALS || stage
                == TournamentStage.SEMI_FINALS) {
            simulateRoundButton.setText("SIMULATE CURRENT LEG");
        } else {
            simulateRoundButton.setText("SIMULATE CURRENT ROUND");
        }
    }

    @FXML
    private void handlePlaySelected() {
        if (selectedMatch == null) {
            return;
        }
        try {
            Match playedMatch = selectedMatch;
            executionService.playMatch(tournament, playedMatch, session.getStadiums());
            selectedMatch = playedMatch;
            if (executionService.requiresPenalties(playedMatch)) {
                localStatus.setText("90 minutes finished. " + "The match must now be decided by penalties.");
            } else {
                localStatus.setText("Match finished: " + playedMatch.getHomeTeam().getName() + " "
                        + playedMatch.getHomeGoals() + " - " + playedMatch.getAwayGoals() + " "
                        + playedMatch.getAwayTeam().getName());
            }
            refresh();
        } catch (Exception exception) {
            showError(exception);
        }
    }

    @FXML
    private void handleSimulatePenalties() {
        if (selectedMatch == null || !executionService.requiresPenalties(selectedMatch)) {
            return;
        }
        try {
            Match penaltyMatch = selectedMatch;
            TournamentStage previousStage = executionService.getCurrentStage(tournament);
            executionService.simulatePenalties(tournament, penaltyMatch, session.getStadiums());
            TournamentStage newStage = executionService.getCurrentStage(tournament);
            selectedMatch = penaltyMatch;
            waitingPenaltyResultAcknowledgement = true;
            renderPenaltyShootout(penaltyMatch);
            currentRoundLabel.setText(tournament.getStageOf(penaltyMatch).getLabel() + " - SHOOTOUT COMPLETE");
            Team winner = penaltyMatch.getWinner();
            if (winner != null) {
                localStatus.setText(winner.getName() + " won the penalty shootout.");
            }
            if (previousStage != newStage) {
                phaseProgressLabel.setText("ROUND COMPLETED");
            } else {
                int remainingPenalties = executionService.getPendingPenaltyMatches(tournament).size();
                if (remainingPenalties > 0) {
                    phaseProgressLabel.setText(remainingPenalties + " PENALTY SHOOTOUT(S) PENDING");
                } else {
                    phaseProgressLabel.setText("SHOOTOUT COMPLETED");
                }
            }
            updateActionButtons();
        } catch (Exception exception) {
            showError(exception);
        }
    }

    @FXML
    private void handleContinueAfterPenalties() {
        waitingPenaltyResultAcknowledgement = false;
        selectedMatch = null;
        refresh();
    }

    private void configureContinueButton() {
        List<Match> pending = executionService.getPendingPenaltyMatches(tournament);
        if (!pending.isEmpty()) {
            continueAfterPenaltiesButton.setText("NEXT PENALTY SHOOTOUT");
            return;
        }
        if (executionService.getCurrentStage(tournament) == TournamentStage.FINISHED) {
            continueAfterPenaltiesButton.setText("TOURNAMENT FINISHED");
            return;
        }
        continueAfterPenaltiesButton.setText("CONTINUE TO NEXT ROUND");
    }

    @FXML
    private void handleSimulateRound() {
        try {
            int simulated = executionService.simulateCurrentMatchday(tournament, session.getStadiums());
            int pending = executionService.getPendingPenaltyMatches(tournament).size();
            if (pending > 0) {
                localStatus.setText(simulated + " match(es) simulated. " + pending
                        + " penalty shootout(s) waiting for the user.");
            } else {
                localStatus.setText(simulated + " match(es) simulated.");
            }
            selectedMatch = null;
            refresh();
        } catch (Exception exception) {
            showError(exception);
        }
    }

    @FXML
    private void handleSimulatePhase() {
        try {
            TournamentStage previousStage = executionService.getCurrentStage(tournament);
            int simulated = executionService.simulateRemainingPhase(tournament, session.getStadiums());
            TournamentStage newStage = executionService.getCurrentStage(tournament);
            int pending = executionService.getPendingPenaltyMatches(tournament).size();
            if (pending > 0) {
                localStatus.setText(simulated + " match(es) simulated. " + pending
                        + " penalty shootout(s) must be resolved manually.");
            } else {
                localStatus.setText(simulated + " match(es) simulated. " + previousStage + " -> " + newStage);
            }
            selectedMatch = null;
            refresh();
        } catch (Exception exception) {
            showError(exception);
        }
    }

    private void showError(Exception exception) {
        localStatus.setText("Error: " + exception.getMessage());
        exception.printStackTrace();
    }
}
