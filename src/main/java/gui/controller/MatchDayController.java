package gui.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.match.Event;
import model.match.FirstLegMatch;
import model.match.Match;
import model.match.PenaltyTaken;
import model.match.SecondLegMatch;
import model.participant.Player;
import model.participant.PlayerParticipation;
import model.participant.Position;
import model.participant.Team;
import model.tournament.Tournament;
import model.tournament.TournamentStage;
import service.TournamentExecutionService;
import service.TournamentSessionService;

import java.util.ArrayList;
import java.util.List;

/*
 * Controla la pantalla Match Day.
 * Permite ejecutar partidos y fases del campeonato.
 *
 * Cuando un partido necesita definirse por penales, la misma pantalla
 * cambia automáticamente a un tablero de tanda de penales.
 * También muestra los pateadores, sus resultados y los arqueros.
 */
public class MatchDayController {

    @FXML
    private Label currentRoundLabel;

    @FXML
    private Label phaseProgressLabel;

    @FXML
    private VBox matchesBox;

    /*
     * PANEL NORMAL DEL PARTIDO
     */

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

    /*
     * PANEL DE PENALES
     */

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

    /*
     * ACCIONES GENERALES
     */

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

        executionService =
                new TournamentExecutionService();

        waitingPenaltyResultAcknowledgement =
                false;
    }

    public void setSession(
            TournamentSessionService session) {

        this.session =
                session;

        tournament =
                session.getTournament();

        selectedMatch =
                null;

        waitingPenaltyResultAcknowledgement =
                false;

        refresh();
    }

    /*
     * Refresca Match Day cuando se vuelve a esta sección.
     */
    public void refreshFromMain() {

        if (session == null) {
            return;
        }

        tournament =
                session.getTournament();

        selectedMatch =
                null;

        waitingPenaltyResultAcknowledgement =
                false;

        refresh();
    }

    /*
     * Refresca toda la pantalla.
     */
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

        int pendingPenalties =
                executionService
                        .getPendingPenaltyMatches(
                                tournament
                        )
                        .size();

        if (pendingPenalties > 0) {

            phaseProgressLabel.setText(
                    played
                            + " / "
                            + total
                            + " MATCHES · "
                            + pendingPenalties
                            + " PENALTIES PENDING"
            );

        } else {

            phaseProgressLabel.setText(
                    played
                            + " / "
                            + total
                            + " MATCHES"
            );
        }

        refreshMatchList();
        updateActionButtons();
    }

    /*
     * Carga los partidos de la fase actual.
     */
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

            matchesBox
                    .getChildren()
                    .add(
                            createMatchButton(
                                    match
                            )
                    );
        }

        refreshSelectedMatch();
    }

    /*
     * Prioriza tandas pendientes y luego partidos disponibles.
     */
    private Match findDefaultMatch(
            List<Match> matches) {

        List<Match> pendingPenalties =
                executionService
                        .getPendingPenaltyMatches(
                                tournament
                        );

        if (!pendingPenalties.isEmpty()) {

            return pendingPenalties.get(
                    0
            );
        }

        List<Match> playableMatches =
                executionService
                        .getCurrentPlayableMatches(
                                tournament
                        );

        if (!playableMatches.isEmpty()) {

            return playableMatches.get(
                    0
            );
        }

        return matches.get(
                0
        );
    }

    /*
     * Genera el botón correspondiente a un partido.
     */
    private Button createMatchButton(
            Match match) {

        String text =
                match.getMatchDate()
                        + "   "
                        + match.getHomeTeam()
                        .getName()
                        + "  vs  "
                        + match.getAwayTeam()
                        .getName()
                        + "     "
                        + getMatchStatus(
                        match
                );

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

        if (executionService
                .requiresPenalties(
                        match
                )) {

            button.getStyleClass()
                    .add(
                            "match-list-playable"
                    );

        } else if (match.isPlayed()) {

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

                    waitingPenaltyResultAcknowledgement =
                            false;

                    selectedMatch =
                            match;

                    refreshMatchList();
                }
        );

        return button;
    }

    private String getMatchStatus(
            Match match) {

        if (executionService
                .requiresPenalties(
                        match
                )) {

            return "PENALTIES";
        }

        if (match.isPlayed()) {

            if (hasPenaltyResult(
                    match
            )) {

                return "PENALTIES FINISHED";
            }

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

    /*
     * Decide si se muestra el partido normal o la tanda.
     */
    private void refreshSelectedMatch() {

        if (selectedMatch == null) {

            showNormalMatchPane();

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

            setResolution(
                    "-",
                    "purple-chip"
            );

            eventsBox
                    .getChildren()
                    .clear();

            updateActionButtons();

            return;
        }

        if (executionService
                .requiresPenalties(
                        selectedMatch
                )
                || hasPenaltyResult(
                selectedMatch
        )) {

            showPenaltyShootoutPane();

            renderPenaltyShootout(
                    selectedMatch
            );

        } else {

            showNormalMatchPane();

            renderNormalMatch(
                    selectedMatch
            );
        }

        updateActionButtons();
    }

    /*
     * Muestra la información normal del partido.
     */
    private void renderNormalMatch(
            Match match) {

        selectedMatchTitle.setText(
                match.getHomeTeam()
                        .getName()
                        + "  VS  "
                        + match.getAwayTeam()
                        .getName()
        );

        selectedScore.setText(
                formatScore(
                        match
                )
        );

        selectedDate.setText(
                "DATE  "
                        + match.getMatchDate()
        );

        selectedStadium.setText(
                "STADIUM  "
                        + (
                        match.getStadium() == null
                                ? "-"
                                : match
                                .getStadium()
                                .getName()
                )
        );

        selectedReferee.setText(
                "REFEREE  "
                        + (
                        match.getReferee() == null
                                ? "-"
                                : match
                                .getReferee()
                                .getName()
                )
        );

        if (match.isPlayed()
                && match.isKnockout()) {

            setResolution(
                    "RESOLUTION  "
                            + match
                            .getResolutionCriteria(),
                    "ready-chip"
            );

        } else if (match.isPlayed()) {

            setResolution(
                    "STATUS  FINISHED",
                    "ready-chip"
            );

        } else if (executionService
                .isMatchPlayable(
                        tournament,
                        match
                )) {

            setResolution(
                    "STATUS  READY",
                    "ready-chip"
            );

        } else {

            setResolution(
                    "STATUS  BLOCKED",
                    "blocked-chip"
            );
        }

        renderMatchEvents(
                match
        );
    }

    private String formatScore(
            Match match) {

        if (!match.isPlayed()) {
            return "VS";
        }

        return match.getHomeGoals()
                + "  -  "
                + match.getAwayGoals();
    }

    /*
     * Muestra las incidencias normales del partido.
     */
    private void renderMatchEvents(
            Match match) {

        eventsBox
                .getChildren()
                .clear();

        if (match.getEvents()
                .isEmpty()) {

            Label empty =
                    new Label(
                            match.isPlayed()
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

            return;
        }

        for (Event event :
                match.getEvents()) {

            if (!(event
                    instanceof PenaltyTaken)) {

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
    }

    private void showNormalMatchPane() {

        normalMatchPane.setVisible(
                true
        );

        normalMatchPane.setManaged(
                true
        );

        penaltyShootoutPane.setVisible(
                false
        );

        penaltyShootoutPane.setManaged(
                false
        );
    }

    private void showPenaltyShootoutPane() {

        normalMatchPane.setVisible(
                false
        );

        normalMatchPane.setManaged(
                false
        );

        penaltyShootoutPane.setVisible(
                true
        );

        penaltyShootoutPane.setManaged(
                true
        );
    }

    /*
     * Muestra el tablero completo de la tanda.
     */
    private void renderPenaltyShootout(
            Match match) {

        boolean finished =
                hasPenaltyResult(
                        match
                );

        penaltyRoundLabel.setText(
                getMatchRoundName(
                        match
                )
        );

        penaltyStatusLabel.setText(
                finished
                        ? "SHOOTOUT FINISHED"
                        : "PENALTIES REQUIRED"
        );

        penaltyHomeTeamLabel.setText(
                match.getHomeTeam()
                        .getName()
        );

        penaltyAwayTeamLabel.setText(
                match.getAwayTeam()
                        .getName()
        );

        /*
         * Mostramos los arqueros que participan de la tanda.
         */
        Player homeGoalkeeper =
                findShootoutGoalkeeper(
                        match,
                        match.getHomeTeam()
                );

        Player awayGoalkeeper =
                findShootoutGoalkeeper(
                        match,
                        match.getAwayTeam()
                );

        penaltyHomeGoalkeeperLabel.setText(
                "GK: "
                        + (
                        homeGoalkeeper == null
                                ? "-"
                                : homeGoalkeeper.getName()
                )
        );

        penaltyAwayGoalkeeperLabel.setText(
                "GK: "
                        + (
                        awayGoalkeeper == null
                                ? "-"
                                : awayGoalkeeper.getName()
                )
        );

        penaltyContextLabel.setText(
                buildPenaltyContext(
                        match
                )
        );

        List<PenaltyTaken> homePenalties =
                getPenaltyEvents(
                        match,
                        match.getHomeTeam()
                );

        List<PenaltyTaken> awayPenalties =
                getPenaltyEvents(
                        match,
                        match.getAwayTeam()
                );

        int homeScore =
                match.getHomePenalties()
                        == null
                        ? 0
                        : match.getHomePenalties();

        int awayScore =
                match.getAwayPenalties()
                        == null
                        ? 0
                        : match.getAwayPenalties();

        penaltyHomeScoreLabel.setText(
                String.valueOf(
                        homeScore
                )
        );

        penaltyAwayScoreLabel.setText(
                String.valueOf(
                        awayScore
                )
        );

        renderPenaltyColumns(
                homePenalties,
                awayPenalties,
                finished
        );

        /*
         * El detalle con nombres aparece después de ejecutar la tanda.
         */
        renderPenaltyKickDetails(
                homePenalties,
                awayPenalties
        );

        simulatePenaltiesButton.setVisible(
                !finished
        );

        simulatePenaltiesButton.setManaged(
                !finished
        );

        penaltyWinnerBox.setVisible(
                finished
        );

        penaltyWinnerBox.setManaged(
                finished
        );

        continueAfterPenaltiesButton.setVisible(
                finished
        );

        continueAfterPenaltiesButton.setManaged(
                finished
        );

        if (finished) {

            Team winner =
                    match.getWinner();

            if (winner != null) {

                penaltyWinnerLabel.setText(
                        winner.getName()
                );

                if (match
                        == tournament.getFinalMatch()) {

                    penaltyWinnerDetailLabel.setText(
                            "CHAMPION · WINS "
                                    + homeScore
                                    + " - "
                                    + awayScore
                                    + " ON PENALTIES"
                    );

                } else {

                    penaltyWinnerDetailLabel.setText(
                            "ADVANCES · WINS "
                                    + homeScore
                                    + " - "
                                    + awayScore
                                    + " ON PENALTIES"
                    );
                }
            }

            configureContinueButton();
        }
    }

    /*
     * Genera las columnas 1 a 5 y las necesarias para muerte súbita.
     */
    private void renderPenaltyColumns(
            List<PenaltyTaken> homePenalties,
            List<PenaltyTaken> awayPenalties,
            boolean finished) {

        penaltyColumnHeaderBox
                .getChildren()
                .clear();

        penaltyHomeKicksBox
                .getChildren()
                .clear();

        penaltyAwayKicksBox
                .getChildren()
                .clear();

        int columns =
                Math.max(
                        5,
                        Math.max(
                                homePenalties.size(),
                                awayPenalties.size()
                        )
                );

        for (int i = 0;
             i < columns;
             i++) {

            String header;

            if (i < 5) {

                header =
                        String.valueOf(
                                i + 1
                        );

            } else {

                header =
                        "SD"
                                + (i - 4);
            }

            penaltyColumnHeaderBox
                    .getChildren()
                    .add(
                            createPenaltyHeader(
                                    header
                            )
                    );

            penaltyHomeKicksBox
                    .getChildren()
                    .add(
                            createPenaltyCell(
                                    i < homePenalties.size()
                                            ? homePenalties.get(i)
                                            : null,
                                    finished
                            )
                    );

            penaltyAwayKicksBox
                    .getChildren()
                    .add(
                            createPenaltyCell(
                                    i < awayPenalties.size()
                                            ? awayPenalties.get(i)
                                            : null,
                                    finished
                            )
                    );
        }
    }

    /*
     * Muestra de manera visible quién convirtió o falló cada penal.
     */
    private void renderPenaltyKickDetails(
            List<PenaltyTaken> homePenalties,
            List<PenaltyTaken> awayPenalties) {

        penaltyKickDetailsBox
                .getChildren()
                .clear();

        int rounds =
                Math.max(
                        homePenalties.size(),
                        awayPenalties.size()
                );

        /*
         * Antes de ejecutar la tanda no mostramos detalles vacíos.
         */
        if (rounds == 0) {

            penaltyKickDetailsBox.setVisible(
                    false
            );

            penaltyKickDetailsBox.setManaged(
                    false
            );

            return;
        }

        penaltyKickDetailsBox.setVisible(
                true
        );

        penaltyKickDetailsBox.setManaged(
                true
        );

        for (int i = 0;
             i < rounds;
             i++) {

            String roundName =
                    i < 5
                            ? "ROUND " + (i + 1)
                            : "SUDDEN DEATH " + (i - 4);

            Label roundLabel =
                    new Label(
                            roundName
                    );

            roundLabel.getStyleClass()
                    .add(
                            "small-label"
                    );

            VBox roundBox =
                    new VBox(
                            5
                    );

            roundBox.getStyleClass()
                    .add(
                            "event-row"
                    );

            roundBox
                    .getChildren()
                    .add(
                            roundLabel
                    );

            if (i < homePenalties.size()) {

                roundBox
                        .getChildren()
                        .add(
                                createPenaltyDetailLabel(
                                        homePenalties.get(i)
                                )
                        );
            }

            if (i < awayPenalties.size()) {

                roundBox
                        .getChildren()
                        .add(
                                createPenaltyDetailLabel(
                                        awayPenalties.get(i)
                                )
                        );
            }

            penaltyKickDetailsBox
                    .getChildren()
                    .add(
                            roundBox
                    );
        }
    }

    /*
     * Genera una línea como:
     * LONABARCE · Juan Perez     ✓ SCORED
     */
    private Label createPenaltyDetailLabel(
            PenaltyTaken penalty) {

        String result =
                penalty.isScored()
                        ? "✓ SCORED"
                        : "✕ MISSED";

        String text =
                penalty.getTeam()
                        .getName()
                        + " · "
                        + penalty
                        .getPlayer()
                        .getName()
                        + "     "
                        + result;

        Label label =
                new Label(
                        text
                );

        label.setWrapText(
                true
        );

        if (penalty.isScored()) {

            label.getStyleClass()
                    .add(
                            "ready-chip"
                    );

        } else {

            label.getStyleClass()
                    .add(
                            "blocked-chip"
                    );
        }

        return label;
    }

    private Label createPenaltyHeader(
            String text) {

        Label label =
                new Label(
                        text
                );

        label.setAlignment(
                Pos.CENTER
        );

        label.setMinSize(
                42,
                24
        );

        label.setPrefSize(
                42,
                24
        );

        label.setMaxSize(
                42,
                24
        );

        label.getStyleClass()
                .add(
                        "small-label"
                );

        return label;
    }

    /*
     * Genera el indicador visual verde, rojo o pendiente.
     */
    private Label createPenaltyCell(
            PenaltyTaken penalty,
            boolean shootoutFinished) {

        Label cell =
                new Label();

        cell.setAlignment(
                Pos.CENTER
        );

        cell.setMinSize(
                42,
                38
        );

        cell.setPrefSize(
                42,
                38
        );

        cell.setMaxSize(
                42,
                38
        );

        if (penalty == null) {

            cell.setText(
                    shootoutFinished
                            ? "—"
                            : "•"
            );

            cell.getStyleClass()
                    .add(
                            "purple-chip"
                    );

            return cell;
        }

        if (penalty.isScored()) {

            cell.setText(
                    "✓"
            );

            cell.getStyleClass()
                    .add(
                            "ready-chip"
                    );

            cell.setTooltip(
                    new Tooltip(
                            penalty
                                    .getPlayer()
                                    .getName()
                                    + " - SCORED"
                    )
            );

        } else {

            cell.setText(
                    "✕"
            );

            cell.getStyleClass()
                    .add(
                            "blocked-chip"
                    );

            cell.setTooltip(
                    new Tooltip(
                            penalty
                                    .getPlayer()
                                    .getName()
                                    + " - MISSED"
                    )
            );
        }

        return cell;
    }

    /*
     * Obtiene los penales ejecutados por un equipo.
     */
    private List<PenaltyTaken> getPenaltyEvents(
            Match match,
            Team team) {

        List<PenaltyTaken> penalties =
                new ArrayList<>();

        for (Event event :
                match.getEvents()) {

            if (event
                    instanceof PenaltyTaken) {

                PenaltyTaken penalty =
                        (PenaltyTaken) event;

                if (penalty.getTeam()
                        == team) {

                    penalties.add(
                            penalty
                    );
                }
            }
        }

        return penalties;
    }

    /*
     * Obtiene los jugadores que terminaron el encuentro en cancha.
     */
    private List<Player> getPlayersOnPitch(
            Match match,
            Team team) {

        List<Player> players =
                new ArrayList<>();

        for (PlayerParticipation participation :
                match.getPlayerParticipations()) {

            if (participation.getTeam()
                    == team
                    && participation.getExitMinute()
                    >= 90) {

                players.add(
                        participation.getPlayer()
                );
            }
        }

        /*
         * Respaldo para partidos antiguos o partidas guardadas
         * que no tengan participaciones registradas.
         */
        if (players.isEmpty()) {

            if (team
                    == match.getHomeTeam()) {

                players.addAll(
                        match.getHomeStarters()
                );

            } else {

                players.addAll(
                        match.getAwayStarters()
                );
            }
        }

        return players;
    }

    /*
     * Busca el arquero que terminó el partido.
     * Si no queda un arquero natural, devuelve el jugador de mayor
     * overall que actúa como arquero de emergencia.
     *
     * Esto utiliza la misma idea que la simulación de la tanda.
     */
    private Player findShootoutGoalkeeper(
            Match match,
            Team team) {

        List<Player> players =
                getPlayersOnPitch(
                        match,
                        team
                );

        if (players.isEmpty()) {
            return null;
        }

        for (Player player :
                players) {

            if (player.getPosition()
                    == Position.GOALKEEPER) {

                return player;
            }
        }

        Player emergencyGoalkeeper =
                players.get(0);

        for (Player player :
                players) {

            if (player.getOverall()
                    > emergencyGoalkeeper.getOverall()) {

                emergencyGoalkeeper =
                        player;
            }
        }

        return emergencyGoalkeeper;
    }

    /*
     * Texto breve que explica por qué se llegó a los penales.
     */
    private String buildPenaltyContext(
            Match match) {

        if (match
                instanceof SecondLegMatch) {

            SecondLegMatch secondLeg =
                    (SecondLegMatch) match;

            FirstLegMatch firstLeg =
                    secondLeg.getFirstLeg();

            return "First leg: "
                    + firstLeg
                    .getHomeTeam()
                    .getName()
                    + " "
                    + firstLeg.getHomeGoals()
                    + " - "
                    + firstLeg.getAwayGoals()
                    + " "
                    + firstLeg
                    .getAwayTeam()
                    .getName()
                    + "   ·   Second leg: "
                    + secondLeg
                    .getHomeTeam()
                    .getName()
                    + " "
                    + secondLeg.getHomeGoals()
                    + " - "
                    + secondLeg.getAwayGoals()
                    + " "
                    + secondLeg
                    .getAwayTeam()
                    .getName()
                    + "   ·   Series remains tied after tournament criteria.";
        }

        return match.getHomeTeam()
                .getName()
                + " "
                + match.getHomeGoals()
                + " - "
                + match.getAwayGoals()
                + " "
                + match.getAwayTeam()
                .getName()
                + "   ·   The final is level after 90 minutes.";
    }

    /*
     * Obtiene el nombre de la ronda del partido.
     */
    private String getMatchRoundName(
            Match match) {

        if (match
                == tournament.getFinalMatch()) {

            return "FINAL";
        }

        if (tournament
                .getSemiFinalFirstLegs()
                .contains(
                        match
                )
                || tournament
                .getSemiFinalSecondLegs()
                .contains(
                        match
                )) {

            return "SEMI-FINALS";
        }

        if (tournament
                .getQuarterFinalFirstLegs()
                .contains(
                        match
                )
                || tournament
                .getQuarterFinalSecondLegs()
                .contains(
                        match
                )) {

            return "QUARTER-FINALS";
        }

        return "GROUP STAGE";
    }

    private boolean hasPenaltyResult(
            Match match) {

        return match != null
                && match.getHomePenalties()
                != null
                && match.getAwayPenalties()
                != null;
    }

    private void setResolution(
            String text,
            String styleClass) {

        selectedResolution.setText(
                text
        );

        selectedResolution
                .getStyleClass()
                .removeAll(
                        "ready-chip",
                        "blocked-chip",
                        "purple-chip"
                );

        selectedResolution
                .getStyleClass()
                .add(
                        styleClass
                );
    }

    /*
     * Configura los botones de acuerdo con el estado del torneo.
     */
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

        if (waitingPenaltyResultAcknowledgement) {

            simulateRoundButton.setDisable(
                    true
            );

            simulatePhaseButton.setDisable(
                    true
            );

            return;
        }

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
                        || !executionService
                        .hasUnplayedMatchesInCurrentPhase(
                                tournament
                        )
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

    /*
     * Simula un partido seleccionado.
     */
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

            selectedMatch =
                    playedMatch;

            if (executionService
                    .requiresPenalties(
                            playedMatch
                    )) {

                localStatus.setText(
                        "90 minutes finished. "
                                + "The match must now be decided by penalties."
                );

            } else {

                localStatus.setText(
                        "Match finished: "
                                + playedMatch
                                .getHomeTeam()
                                .getName()
                                + " "
                                + playedMatch.getHomeGoals()
                                + " - "
                                + playedMatch.getAwayGoals()
                                + " "
                                + playedMatch
                                .getAwayTeam()
                                .getName()
                );
            }

            refresh();

        } catch (Exception exception) {

            showError(
                    exception
            );
        }
    }

    /*
     * Ejecuta manualmente la tanda.
     */
    @FXML
    private void handleSimulatePenalties() {

        if (selectedMatch == null
                || !executionService
                .requiresPenalties(
                        selectedMatch
                )) {

            return;
        }

        try {

            Match penaltyMatch =
                    selectedMatch;

            TournamentStage previousStage =
                    executionService
                            .getCurrentStage(
                                    tournament
                            );

            executionService
                    .simulatePenalties(
                            tournament,
                            penaltyMatch,
                            session.getStadiums()
                    );

            TournamentStage newStage =
                    executionService
                            .getCurrentStage(
                                    tournament
                            );

            selectedMatch =
                    penaltyMatch;

            waitingPenaltyResultAcknowledgement =
                    true;

            renderPenaltyShootout(
                    penaltyMatch
            );

            currentRoundLabel.setText(
                    getMatchRoundName(
                            penaltyMatch
                    )
                            + " - SHOOTOUT COMPLETE"
            );

            Team winner =
                    penaltyMatch.getWinner();

            if (winner != null) {

                localStatus.setText(
                        winner.getName()
                                + " won the penalty shootout."
                );
            }

            if (previousStage
                    != newStage) {

                phaseProgressLabel.setText(
                        "ROUND COMPLETED"
                );

            } else {

                int remainingPenalties =
                        executionService
                                .getPendingPenaltyMatches(
                                        tournament
                                )
                                .size();

                if (remainingPenalties > 0) {

                    phaseProgressLabel.setText(
                            remainingPenalties
                                    + " PENALTY SHOOTOUT(S) PENDING"
                    );

                } else {

                    phaseProgressLabel.setText(
                            "SHOOTOUT COMPLETED"
                    );
                }
            }

            updateActionButtons();

        } catch (Exception exception) {

            showError(
                    exception
            );
        }
    }

    /*
     * Continúa luego de observar el resultado de la tanda.
     */
    @FXML
    private void handleContinueAfterPenalties() {

        waitingPenaltyResultAcknowledgement =
                false;

        selectedMatch =
                null;

        refresh();
    }

    /*
     * Configura el texto del botón posterior a la tanda.
     */
    private void configureContinueButton() {

        List<Match> pending =
                executionService
                        .getPendingPenaltyMatches(
                                tournament
                        );

        if (!pending.isEmpty()) {

            continueAfterPenaltiesButton.setText(
                    "NEXT PENALTY SHOOTOUT"
            );

            return;
        }

        if (executionService
                .getCurrentStage(
                        tournament
                )
                == TournamentStage.FINISHED) {

            continueAfterPenaltiesButton.setText(
                    "TOURNAMENT FINISHED"
            );

            return;
        }

        continueAfterPenaltiesButton.setText(
                "CONTINUE TO NEXT ROUND"
        );
    }

    /*
     * Simula la jornada actualmente disponible.
     */
    @FXML
    private void handleSimulateRound() {

        try {

            int simulated =
                    executionService
                            .simulateCurrentMatchday(
                                    tournament,
                                    session.getStadiums()
                            );

            int pending =
                    executionService
                            .getPendingPenaltyMatches(
                                    tournament
                            )
                            .size();

            if (pending > 0) {

                localStatus.setText(
                        simulated
                                + " match(es) simulated. "
                                + pending
                                + " penalty shootout(s) waiting for the user."
                );

            } else {

                localStatus.setText(
                        simulated
                                + " match(es) simulated."
                );
            }

            selectedMatch =
                    null;

            refresh();

        } catch (Exception exception) {

            showError(
                    exception
            );
        }
    }

    /*
     * Simula los partidos restantes de la fase.
     */
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

            int pending =
                    executionService
                            .getPendingPenaltyMatches(
                                    tournament
                            )
                            .size();

            if (pending > 0) {

                localStatus.setText(
                        simulated
                                + " match(es) simulated. "
                                + pending
                                + " penalty shootout(s) must be resolved manually."
                );

            } else {

                localStatus.setText(
                        simulated
                                + " match(es) simulated. "
                                + previousStage
                                + " -> "
                                + newStage
                );
            }

            selectedMatch =
                    null;

            refresh();

        } catch (Exception exception) {

            showError(
                    exception
            );
        }
    }

    private void showError(
            Exception exception) {

        localStatus.setText(
                "Error: "
                        + exception.getMessage()
        );

        exception.printStackTrace();
    }
}