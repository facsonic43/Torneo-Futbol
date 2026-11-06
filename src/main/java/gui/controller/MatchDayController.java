package gui.controller;

import gui.MatchEventsView;
import gui.PenaltyKicksView;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
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
    private boolean waitingPhaseAcknowledgement;
    private TournamentStage reviewedStage;
    private List<Node> normalMatchPaneChildren;
    private Runnable onViewTournamentSummary;
    private Runnable onViewTournamentStats;

    public MatchDayController() {
        executionService = new TournamentExecutionService();
        waitingPenaltyResultAcknowledgement = false;
    }

    @FXML
    private void initialize() {
        normalMatchPaneChildren = List.copyOf(normalMatchPane.getChildren());
    }

    void setNavigationActions(Runnable onViewTournamentSummary, Runnable onViewTournamentStats) {
        this.onViewTournamentSummary = onViewTournamentSummary;
        this.onViewTournamentStats = onViewTournamentStats;
    }

    void setSession(TournamentSessionService session) {
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
        clearAcknowledgementState();
        refresh();
    }

    public Label getCurrentRoundLabel() {
        return currentRoundLabel;
    }

    public Label getPhaseProgressLabel() {
        return phaseProgressLabel;
    }

    public VBox getMatchesBox() {
        return matchesBox;
    }

    public VBox getNormalMatchPane() {
        return normalMatchPane;
    }

    public Label getSelectedMatchTitle() {
        return selectedMatchTitle;
    }

    public Label getSelectedScore() {
        return selectedScore;
    }

    public Label getSelectedDate() {
        return selectedDate;
    }

    public Label getSelectedStadium() {
        return selectedStadium;
    }

    public Label getSelectedReferee() {
        return selectedReferee;
    }

    public Label getSelectedResolution() {
        return selectedResolution;
    }

    public VBox getEventsBox() {
        return eventsBox;
    }

    public Button getPlaySelectedButton() {
        return playSelectedButton;
    }

    public VBox getPenaltyShootoutPane() {
        return penaltyShootoutPane;
    }

    public Label getPenaltyRoundLabel() {
        return penaltyRoundLabel;
    }

    public Label getPenaltyStatusLabel() {
        return penaltyStatusLabel;
    }

    public Label getPenaltyContextLabel() {
        return penaltyContextLabel;
    }

    public Label getPenaltyHomeTeamLabel() {
        return penaltyHomeTeamLabel;
    }

    public Label getPenaltyAwayTeamLabel() {
        return penaltyAwayTeamLabel;
    }

    public Label getPenaltyHomeGoalkeeperLabel() {
        return penaltyHomeGoalkeeperLabel;
    }

    public Label getPenaltyAwayGoalkeeperLabel() {
        return penaltyAwayGoalkeeperLabel;
    }

    public Label getPenaltyHomeScoreLabel() {
        return penaltyHomeScoreLabel;
    }

    public Label getPenaltyAwayScoreLabel() {
        return penaltyAwayScoreLabel;
    }

    public HBox getPenaltyColumnHeaderBox() {
        return penaltyColumnHeaderBox;
    }

    public HBox getPenaltyHomeKicksBox() {
        return penaltyHomeKicksBox;
    }

    public HBox getPenaltyAwayKicksBox() {
        return penaltyAwayKicksBox;
    }

    public VBox getPenaltyKickDetailsBox() {
        return penaltyKickDetailsBox;
    }

    public PenaltyKicksView getPenaltyKicksView() {
        return penaltyKicksView;
    }

    public MatchEventsView getMatchEventsView() {
        return matchEventsView;
    }

    public Button getSimulatePenaltiesButton() {
        return simulatePenaltiesButton;
    }

    public VBox getPenaltyWinnerBox() {
        return penaltyWinnerBox;
    }

    public Label getPenaltyWinnerLabel() {
        return penaltyWinnerLabel;
    }

    public Label getPenaltyWinnerDetailLabel() {
        return penaltyWinnerDetailLabel;
    }

    public Button getSimulateRoundButton() {
        return simulateRoundButton;
    }

    public Button getSimulatePhaseButton() {
        return simulatePhaseButton;
    }

    public Label getLocalStatus() {
        return localStatus;
    }

    public TournamentSessionService getSession() {
        return session;
    }

    public TournamentExecutionService getExecutionService() {
        return executionService;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public Match getSelectedMatch() {
        return selectedMatch;
    }

    public boolean isWaitingPenaltyResultAcknowledgement() {
        return waitingPenaltyResultAcknowledgement;
    }

    public boolean isWaitingPhaseAcknowledgement() {
        return waitingPhaseAcknowledgement;
    }

    public TournamentStage getReviewedStage() {
        return reviewedStage;
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

    private boolean hasPenaltyResult(Match match) {
        return match != null && match.getResolutionCriterion() == ResolutionCriterion.PENALTIES;
    }

    private void setResolution(String text, String styleClass) {
        selectedResolution.setText(text);
        selectedResolution.getStyleClass().removeAll("ready-chip", "blocked-chip", "purple-chip");
        selectedResolution.getStyleClass().add(styleClass);
    }

    void refreshFromMain() {
        if (session == null) {
            return;
        }
        if (tournament != session.getTournament()) {
            tournament = session.getTournament();
            selectedMatch = null;
            clearAcknowledgementState();
        }
        refresh();
    }

    private void refresh() {
        if (tournament == null) {
            return;
        }
        if ((waitingPhaseAcknowledgement && reviewedStage != null) || isTournamentFinished()) {
            TournamentStage displayedStage = getDisplayedStage();
            List<Match> matches = TournamentExecutionService.getMatchesOfStage(tournament, displayedStage);
            long played = matches.stream().filter(Match::isPlayed).count();
            currentRoundLabel.setText(displayedStage.getLabel() + " - FINISHED");
            phaseProgressLabel.setText(played + " / " + matches.size() + " MATCHES");
        } else {
            currentRoundLabel.setText(executionService.getCurrentRoundName(tournament));
            int played = executionService.getPlayedMatchesInCurrentPhase(tournament);
            int total = executionService.getTotalMatchesInCurrentPhase(tournament);
            int pendingPenalties = executionService.getPendingPenaltyMatches(tournament).size();
            if (pendingPenalties > 0) {
                phaseProgressLabel.setText(played + " / " + total + " MATCHES · " + pendingPenalties + " PENALTIES PENDING");
            } else {
                phaseProgressLabel.setText(played + " / " + total + " MATCHES");
            }
        }
        refreshMatchList();
        if (waitingPhaseAcknowledgement || isTournamentFinished()) {
            renderPhaseAcknowledgement();
        } else {
            refreshSelectedMatch();
        }
        updateActionButtons();
    }

    private boolean isTournamentFinished() {
        return executionService.getCurrentStage(tournament) == TournamentStage.FINISHED;
    }

    private TournamentStage getDisplayedStage() {
        if (waitingPhaseAcknowledgement && reviewedStage != null) {
            return reviewedStage;
        }
        return isTournamentFinished() ? TournamentStage.FINAL : executionService.getCurrentStage(tournament);
    }

    private void refreshMatchList() {
        matchesBox.getChildren().clear();
        List<Match> matches = TournamentExecutionService.getMatchesOfStage(tournament, getDisplayedStage());
        if (matches.isEmpty()) {
            Label empty = new Label("No matches available.");
            empty.getStyleClass().add("muted-text");
            matchesBox.getChildren().add(empty);
            selectedMatch = null;
            return;
        }
        if (selectedMatch == null || !matches.contains(selectedMatch)) {
            selectedMatch = findDefaultMatch(matches);
        }
        for (Match match : matches) {
            matchesBox.getChildren().add(createMatchButton(match));
        }
        if (waitingPhaseAcknowledgement || isTournamentFinished()) {
            Button summaryButton = new Button(isTournamentFinished()
                    ? "TOURNAMENT FINISHED" : "STAGE FINISHED");
            summaryButton.setMaxWidth(Double.MAX_VALUE);
            summaryButton.getStyleClass().add("continue-button");
            summaryButton.setOnAction(event -> renderPhaseAcknowledgement());
            matchesBox.getChildren().add(summaryButton);
        }
    }

    private Match findDefaultMatch(List<Match> matches) {
        if (waitingPhaseAcknowledgement || isTournamentFinished()) {
            return matches.getLast();
        }
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
            refreshSelectedMatch();
        });
        return button;
    }

    private void refreshSelectedMatch() {
        restoreNormalMatchPaneStructure();
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

    private void restoreNormalMatchPaneStructure() {
        if (!normalMatchPane.getChildren().equals(normalMatchPaneChildren)) {
            normalMatchPane.getChildren().setAll(normalMatchPaneChildren);
        }
    }

    private void renderPhaseAcknowledgement() {
        showNormalMatchPane();
        normalMatchPane.getChildren().clear();
        boolean finished = isTournamentFinished();
        VBox panel = new VBox(24);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(panel, Priority.ALWAYS);

        Label title = new Label(finished ? "TOURNAMENT FINISHED" : "STAGE FINISHED");
        title.getStyleClass().add("section-title-large");
        title.setStyle("-fx-font-size: 32px;");
        title.setWrapText(true);
        title.setTextAlignment(TextAlignment.CENTER);
        Label subtitle = new Label(finished
                ? tournament.getChampion().getName() + " are the champions! Review the final from the match list, "
                        + "or explore the tournament summary and statistics."
                : reviewedStage.getLabel() + " is complete. Select a match to review its result and events, "
                        + "or continue when you are ready.");
        subtitle.getStyleClass().add("muted-text");
        subtitle.setStyle("-fx-font-size: 16px;");
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(TextAlignment.CENTER);
        panel.getChildren().addAll(title, subtitle);

        if (finished) {
            Button summaryButton = createAcknowledgementButton("VIEW TOURNAMENT SUMMARY", () -> {
                clearAcknowledgementState();
                onViewTournamentSummary.run();
            });
            summaryButton.setDisable(onViewTournamentSummary == null);
            Button statsButton = createAcknowledgementButton("VIEW TOURNAMENT STATS", () -> {
                clearAcknowledgementState();
                onViewTournamentStats.run();
            });
            statsButton.setDisable(onViewTournamentStats == null);
            panel.getChildren().addAll(summaryButton, statsButton);
        } else {
            panel.getChildren().add(createAcknowledgementButton("CONTINUE TO NEXT STAGE", () -> {
                clearAcknowledgementState();
                selectedMatch = null;
                refresh();
            }));
        }
        normalMatchPane.getChildren().add(panel);
    }

    private Button createAcknowledgementButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("continue-button");
        button.setPrefWidth(420);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(56);
        button.setWrapText(true);
        button.setStyle("-fx-font-size: 15px; -fx-padding: 16px 24px;");
        button.setOnAction(event -> action.run());
        return button;
    }

    private void clearAcknowledgementState() {
        waitingPhaseAcknowledgement = false;
        reviewedStage = null;
        waitingPenaltyResultAcknowledgement = false;
        restoreNormalMatchPaneStructure();
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
        }
    }

    private void updateActionButtons() {
        boolean isWaiting = waitingPhaseAcknowledgement || waitingPenaltyResultAcknowledgement;
        boolean playable = selectedMatch != null && executionService.isMatchPlayable(tournament, selectedMatch);
        playSelectedButton.setDisable(isWaiting || !playable);
        simulatePenaltiesButton.setDisable(isWaiting);
        if (isWaiting) {
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
        if (selectedMatch == null || waitingPhaseAcknowledgement || waitingPenaltyResultAcknowledgement) {
            return;
        }
        try {
            Match playedMatch = selectedMatch;
            TournamentStage previousStage = executionService.getCurrentStage(tournament);
            executionService.playMatch(tournament, playedMatch, session.getStadiums());
            TournamentStage newStage = executionService.getCurrentStage(tournament);
            if (previousStage != newStage) {
                waitingPhaseAcknowledgement = true;
                reviewedStage = previousStage;
            }
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
        if (selectedMatch == null || waitingPhaseAcknowledgement || waitingPenaltyResultAcknowledgement
                || !executionService.requiresPenalties(selectedMatch)) {
            return;
        }
        try {
            Match penaltyMatch = selectedMatch;
            TournamentStage previousStage = executionService.getCurrentStage(tournament);
            executionService.simulatePenalties(tournament, penaltyMatch, session.getStadiums());
            TournamentStage newStage = executionService.getCurrentStage(tournament);
            selectedMatch = penaltyMatch;
            waitingPenaltyResultAcknowledgement = true;
            if (previousStage != newStage) {
                waitingPhaseAcknowledgement = true;
                reviewedStage = previousStage;
                refresh();
                return;
            }
            refreshSelectedMatch();
            currentRoundLabel.setText(tournament.getStageOf(penaltyMatch).getLabel() + " - SHOOTOUT COMPLETE");
            Team winner = penaltyMatch.getWinner();
            if (winner != null) {
                localStatus.setText(winner.getName() + " won the penalty shootout.");
            }
            int remainingPenalties = executionService.getPendingPenaltyMatches(tournament).size();
            if (remainingPenalties > 0) {
                phaseProgressLabel.setText(remainingPenalties + " PENALTY SHOOTOUT(S) PENDING");
            } else {
                phaseProgressLabel.setText("SHOOTOUT COMPLETED");
            }
            updateActionButtons();
        } catch (Exception exception) {
            showError(exception);
        }
    }

    @FXML
    private void handleSimulateRound() {
        if (waitingPhaseAcknowledgement || waitingPenaltyResultAcknowledgement) {
            return;
        }
        try {
            TournamentStage previousStage = executionService.getCurrentStage(tournament);
            int simulated = executionService.simulateCurrentMatchday(tournament, session.getStadiums());
            TournamentStage newStage = executionService.getCurrentStage(tournament);
            if (previousStage != newStage) {
                waitingPhaseAcknowledgement = true;
                reviewedStage = previousStage;
            }
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
        if (waitingPhaseAcknowledgement || waitingPenaltyResultAcknowledgement) {
            return;
        }
        try {
            TournamentStage previousStage = executionService.getCurrentStage(tournament);
            int simulated = executionService.simulateRemainingPhase(tournament, session.getStadiums());
            TournamentStage newStage = executionService.getCurrentStage(tournament);
            if (previousStage != newStage) {
                waitingPhaseAcknowledgement = true;
                reviewedStage = previousStage;
            }
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
