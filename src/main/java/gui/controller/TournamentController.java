package gui.controller;

import gui.PlayerDirectoryEntry;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.match.GroupMatch;
import model.match.Match;
import model.participant.Goalkeeper;
import model.participant.Player;
import model.participant.Position;
import model.participant.Referee;
import model.participant.Team;
import model.tournament.Group;
import model.tournament.Standing;
import model.tournament.Tournament;
import reports.ReportService;
import service.TournamentDashboardService;
import service.TournamentSessionService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/*
 * Controla las pantallas informativas del campeonato:
 * overview, equipos, jugadores, cuadro eliminatorio y reportes.
 * No ejecuta partidos ni modifica la base de datos.
 */
public class TournamentController {

    /*
     * PANELES PRINCIPALES
     */
    @FXML
    private VBox overviewPane;

    @FXML
    private HBox teamsPane;

    @FXML
    private VBox bracketPane;

    @FXML
    private HBox reportsPane;


    /*
     * OVERVIEW
     */

    @FXML
    private VBox overviewQuarterBox;

    @FXML
    private VBox overviewSemiBox;

    @FXML
    private VBox overviewFinalBox;

    @FXML
    private Label overviewChampionName;

    @FXML
    private HBox allGroupsContainer;


    /*
     * TEAMS
     */
    @FXML
    private ListView<Team> teamList;

    @FXML
    private TableView<Player> playerTable;

    @FXML
    private TableColumn<Player, String> playerNameColumn;

    @FXML
    private TableColumn<Player, String> playerPositionColumn;

    @FXML
    private TableColumn<Player, String> playerOverallColumn;

    @FXML
    private TableColumn<Player, String> playerMatchesColumn;

    @FXML
    private TableColumn<Player, String> playerMinutesColumn;

    @FXML
    private TableColumn<Player, String> playerGoalsColumn;

    @FXML
    private Label teamName;

    @FXML
    private Label teamCountry;

    @FXML
    private Label teamRanking;

    @FXML
    private Label teamOverall;

    @FXML
    private Label coachName;

    @FXML
    private Label coachDetails;

    @FXML
    private Label playerName;

    @FXML
    private Label playerPosition;

    @FXML
    private Label playerNationality;

    @FXML
    private Label playerDocument;

    @FXML
    private Label playerAge;

    @FXML
    private Label playerRating;

    @FXML
    private Label playerStats;

    @FXML
    private Label playerDiscipline;

    @FXML
    private Label playerAvailability;

    @FXML
    private Label goalkeeperInformation;

    /*
     * BRACKET
     */
    @FXML
    private VBox qualifiedBox;

    @FXML
    private VBox quarterFinalBox;

    @FXML
    private VBox semiFinalBox;

    @FXML
    private VBox finalBox;

    @FXML
    private Label championName;


    /*
     * REPORTS
     */
    @FXML
    private ListView<String> reportList;

    @FXML
    private StackPane reportContentHost;

    @FXML
    private Label selectedReportSubtitle;

    @FXML
    private Label reportMetricChip;

    @FXML
    private Label selectedReportTitle;


    private TournamentSessionService session;
    private Tournament tournament;

    private TournamentDashboardService dashboardService;
    private ReportService reportService;

    @FXML
    public void initialize() {
        dashboardService = new TournamentDashboardService();
        reportService = new ReportService();

        configureTeamList();
        configurePlayerTable();
        configureReports();
    }

    public void setSession(TournamentSessionService session) {
        this.session = session;
        tournament = session.getTournament();
        refreshAll();
    }

    public void refreshAll() {
        if (session == null) {
            return;
        }

        tournament = session.getTournament();

        refreshOverview();
        refreshTeams();
        refreshBracket();
        refreshReports();
    }


    /* =====================================================
       NAVEGACIÓN INTERNA
       ===================================================== */

    public void showOverview() {
        showPane(overviewPane);
    }

    public void showTeams() {
        showPane(teamsPane);
    }

    public void showBracket() {
        showPane(bracketPane);
    }

    public void showReports() {
        showPane(reportsPane);
    }

    private void showPane(Node selectedPane) {
        overviewPane.setVisible(selectedPane == overviewPane);
        overviewPane.setManaged(selectedPane == overviewPane);

        teamsPane.setVisible(selectedPane == teamsPane);
        teamsPane.setManaged(selectedPane == teamsPane);

        bracketPane.setVisible(selectedPane == bracketPane);
        bracketPane.setManaged(selectedPane == bracketPane);

        reportsPane.setVisible(selectedPane == reportsPane);
        reportsPane.setManaged(selectedPane == reportsPane);
    }


    /* =====================================================
       OVERVIEW
       ===================================================== */

    private void refreshOverview() {
        if (tournament == null) {
            return;
        }

        refreshGroups();
        refreshOverviewKnockoutStages();
    }

    private void refreshOverviewKnockoutStages() {
        overviewQuarterBox.getChildren().clear();
        overviewSemiBox.getChildren().clear();
        overviewFinalBox.getChildren().clear();

        // 1. Cuartos
        List<? extends Match> qFirst = tournament.getQuarterFinalFirstLegs();
        List<? extends Match> qSecond = tournament.getQuarterFinalSecondLegs();
        populateOverviewRound(overviewQuarterBox, qFirst, qSecond);

        // 2. Semis
        List<? extends Match> sFirst = tournament.getSemiFinalFirstLegs();
        List<? extends Match> sSecond = tournament.getSemiFinalSecondLegs();
        populateOverviewRound(overviewSemiBox, sFirst, sSecond);

        // 3. Final
        Match finalMatch = tournament.getFinalMatch();
        if (finalMatch == null) {
            Label notPlayed = new Label("NOT PLAYED YET");
            notPlayed.getStyleClass().add("muted-text");
            overviewFinalBox.getChildren().add(notPlayed);
        } else {
            VBox matchCard = new VBox(4);
            matchCard.getStyleClass().add("bracket-card");

            Label teams = new Label(finalMatch.getHomeTeam().getName() + " vs " + finalMatch.getAwayTeam().getName());
            teams.getStyleClass().add("player-name-cell");

            Label score = new Label(finalMatch.isPlayed() ? scoreText(finalMatch) : "NOT PLAYED YET");
            score.getStyleClass().add(finalMatch.isPlayed() ? "metric-value" : "muted-text");
            score.setStyle("-fx-font-size: 13px;");

            matchCard.getChildren().addAll(teams, score);
            overviewFinalBox.getChildren().add(matchCard);
        }

        // 4. Campeón
        if (tournament.getChampion() != null) {
            overviewChampionName.setText(tournament.getChampion().getName().toUpperCase());
            overviewChampionName.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: -accent-gold;");
        } else {
            overviewChampionName.setText("NOT WINNER YET");
            overviewChampionName.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: -text-muted;");
        }
    }

    private void populateOverviewRound(VBox container, List<? extends Match> firstLegs, List<? extends Match> secondLegs) {
        if (firstLegs == null || firstLegs.isEmpty()) {
            Label notPlayed = new Label("NOT PLAYED YET");
            notPlayed.getStyleClass().add("muted-text");
            container.getChildren().add(notPlayed);
            return;
        }

        for (int i = 0; i < firstLegs.size(); i++) {
            Match m1 = firstLegs.get(i);
            Match m2 = (secondLegs != null && i < secondLegs.size()) ? secondLegs.get(i) : null;

            VBox seriesBox = new VBox(2);
            seriesBox.getStyleClass().add("bracket-card");

            Label matchLabel = new Label(m1.getHomeTeam().getName() + " vs " + m1.getAwayTeam().getName());
            matchLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 11px;");

            String s1 = m1.isPlayed() ? (m1.getHomeGoals() + "-" + m1.getAwayGoals()) : "-";
            String s2 = (m2 != null && m2.isPlayed()) ? (m2.getHomeGoals() + "-" + m2.getAwayGoals()) : "-";

            Label res = new Label(m1.isPlayed() ? ("Leg 1: " + s1 + " | Leg 2: " + s2) : "NOT PLAYED YET");
            res.getStyleClass().add(m1.isPlayed() ? "small-text" : "muted-text");

            seriesBox.getChildren().addAll(matchLabel, res);
            container.getChildren().add(seriesBox);
        }
    }    private void refreshGroups() {
        if (allGroupsContainer == null) {
            return;
        }

        allGroupsContainer.getChildren().clear();

        if (tournament == null || tournament.getGroups().isEmpty()) {
            Label empty = new Label("No groups available");
            empty.getStyleClass().add("muted-text");
            allGroupsContainer.getChildren().add(empty);
            return;
        }

        for (Group group : tournament.getGroups()) {
            VBox groupCard = new VBox(10);
            groupCard.getStyleClass().add("group-card");

            // Título del Grupo
            HBox header = new HBox();
            header.getStyleClass().add("group-card-header");
            Label title = new Label(group.getName().toUpperCase());
            title.getStyleClass().add("group-card-title");
            header.getChildren().add(title);
            groupCard.getChildren().add(header);

            // Cabecera de la tabla del grupo: # | TEAM | PTS | P | GD
            HBox tableHeader = new HBox(8);
            tableHeader.setAlignment(Pos.CENTER_LEFT);
            tableHeader.getChildren().addAll(
                    createHeadLabel("#", 20),
                    createHeadLabel("TEAM", 130),
                    createHeadLabel("PTS", 32),
                    createHeadLabel("P", 28),
                    createHeadLabel("GD", 32)
            );
            groupCard.getChildren().add(tableHeader);

            // Filas de Posiciones
            List<Standing> standings = group.getStandings();
            for (int i = 0; i < standings.size(); i++) {
                Standing standing = standings.get(i);

                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);

                // Clasifican los 2 primeros (resaltados con verde/acento)
                if (i < 2) {
                    row.getStyleClass().add("standing-qualified");
                } else {
                    row.getStyleClass().add("standing-normal");
                }

                Label rankLabel = createColumn(String.valueOf(i + 1), 20);
                rankLabel.setAlignment(Pos.CENTER);

                Label teamLabel = createColumn(standing.getTeam().getName(), 130);
                teamLabel.setStyle("-fx-font-weight: 700;");

                Label ptsLabel = createColumn(String.valueOf(standing.getPoints()), 32);
                ptsLabel.setAlignment(Pos.CENTER);
                ptsLabel.setStyle("-fx-font-weight: 900;");

                Label playedLabel = createColumn(String.valueOf(standing.getPlayed()), 28);
                playedLabel.setAlignment(Pos.CENTER);

                Label gdLabel = createColumn(formatGoalDifference(standing.getGoalDifference()), 32);
                gdLabel.setAlignment(Pos.CENTER);

                row.getChildren().addAll(rankLabel, teamLabel, ptsLabel, playedLabel, gdLabel);
                groupCard.getChildren().add(row);
            }

            allGroupsContainer.getChildren().add(groupCard);
        }
    }

    private Label createHeadLabel(String text, double width) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.getStyleClass().add("group-table-head");
        if (!text.equals("TEAM")) {
            label.setAlignment(Pos.CENTER);
        }
        return label;
    }



    private Label createColumn(String text, double width) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        return label;
    }

    private String formatGoalDifference(int value) {
        if (value > 0) {
            return "+" + value;
        }
        return String.valueOf(value);
    }


    /* =====================================================
       TEAMS
       ===================================================== */

    private void configureTeamList() {
        teamList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Team team, boolean empty) {
                super.updateItem(team, empty);
                if (empty || team == null) {
                    setText(null);
                    setGraphic(null);
                    setDisable(true);
                } else {
                    setText(team.getName());
                    setDisable(false);
                }
            }
        });

        teamList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldTeam, newTeam) -> {
                    if (newTeam != null) {
                        showTeam(newTeam);
                    }
                }
        );
    }

    private void configurePlayerTable() {
        playerNameColumn.setCellValueFactory(
                value -> new SimpleStringProperty(value.getValue().getName())
        );
        playerNameColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    getStyleClass().add("player-name-cell");
                }
            }
        });

        playerPositionColumn.setCellValueFactory(
                value -> new SimpleStringProperty(value.getValue().getPosition().toString())
        );
        playerPositionColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("pos-badge");

                    String posUpper = item.toUpperCase();
                    if (posUpper.contains("GOALKEEPER") || posUpper.contains("GK") || posUpper.contains("ARQUERO")) {
                        badge.getStyleClass().add("pos-gk");
                    } else if (posUpper.contains("DEFENDER") || posUpper.contains("DEF") || posUpper.contains("DEFENSA")) {
                        badge.getStyleClass().add("pos-def");
                    } else if (posUpper.contains("MIDFIELDER") || posUpper.contains("MID") || posUpper.contains("MEDIO")) {
                        badge.getStyleClass().add("pos-mid");
                    } else {
                        badge.getStyleClass().add("pos-fwd");
                    }

                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        playerOverallColumn.setCellValueFactory(
                value -> new SimpleStringProperty(String.format("%.1f", value.getValue().getOverall()))
        );
        playerOverallColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("ovr-badge");
                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        playerMatchesColumn.setCellValueFactory(
                value -> new SimpleStringProperty(String.valueOf(value.getValue().getMatchesPlayed()))
        );
        playerMatchesColumn.setCellFactory(col -> createCenterCell());

        playerMinutesColumn.setCellValueFactory(
                value -> new SimpleStringProperty(String.valueOf(value.getValue().getMinutesPlayed()))
        );
        playerMinutesColumn.setCellFactory(col -> createCenterCell());

        playerGoalsColumn.setCellValueFactory(
                value -> new SimpleStringProperty(String.valueOf(value.getValue().getGoals()))
        );
        playerGoalsColumn.setCellFactory(col -> createCenterCell());

        playerTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldPlayer, newPlayer) -> {
                    if (newPlayer != null) {
                        showPlayer(newPlayer);
                    }
                }
        );
    }

    private TableCell<Player, String> createCenterCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    getStyleClass().add("table-stat-cell");
                }
            }
        };
    }

    private void refreshTeams() {
        if (tournament == null) {
            return;
        }

        Team previousSelection = teamList.getSelectionModel().getSelectedItem();

        teamList.getItems().setAll(tournament.getTeams());
        teamList.getItems().sort(Comparator.comparing(Team::getName, String.CASE_INSENSITIVE_ORDER));

        if (previousSelection != null && teamList.getItems().contains(previousSelection)) {
            teamList.getSelectionModel().select(previousSelection);
        } else if (!teamList.getItems().isEmpty()) {
            teamList.getSelectionModel().selectFirst();
        }
    }

    private void showTeam(Team team) {
        teamName.setText(team.getName());
        teamCountry.setText(team.getCountry().getName().toUpperCase());
        teamRanking.setText("#" + team.getRanking() + " CONTINENTAL RANKING");
        teamOverall.setText(String.format("%.1f", team.getOverall()));

        if (team.getCoach() != null) {
            coachName.setText(team.getCoach().getName());

            String titles = team.getCoach().getTitlesObtained() + " titles";
            String country = team.getCoach().getNationality() != null
                    ? team.getCoach().getNationality().getName()
                    : "";

            coachDetails.setText(country.isEmpty() ? titles : country + " • " + titles);
        } else {
            coachName.setText("-");
            coachDetails.setText("No coach registered");
        }

        playerTable.getItems().setAll(team.getSquad());

        if (!playerTable.getItems().isEmpty()) {
            playerTable.getSelectionModel().selectFirst();
        }
    }

    private void showPlayer(Player player) {
        playerName.setText(player.getName());
        playerPosition.setText(player.getPosition().toString());
        playerNationality.setText(player.getNationality().getName());
        playerDocument.setText(player.getIdType() + " " + player.getIdNumber());
        playerAge.setText(player.getAge() + " years");
        playerRating.setText(String.format("%.1f", player.getOverall()));

        playerStats.setText(
                player.getMatchesPlayed()
                        + " matches | "
                        + player.getMinutesPlayed()
                        + " min | "
                        + player.getGoals()
                        + " goals | "
                        + player.getAssists()
                        + " assists"
        );

        playerDiscipline.setText(
                player.getYellowCards()
                        + " yellow | "
                        + player.getRedCards()
                        + " red"
        );

        if (player.isSuspended()) {
            playerAvailability.setText("SUSPENDED");
        } else if (player.isInjured()) {
            playerAvailability.setText("INJURED");
        } else {
            playerAvailability.setText("AVAILABLE");
        }

        if (player instanceof Goalkeeper) {
            Goalkeeper goalkeeper = (Goalkeeper) player;
            double average = 0.0;

            if (goalkeeper.getMatchesPlayed() > 0) {
                average = (double) goalkeeper.getGoalsConceded() / goalkeeper.getMatchesPlayed();
            }

            goalkeeperInformation.setText(
                    goalkeeper.getGoalsConceded()
                            + " goals conceded | "
                            + String.format("%.2f", average)
                            + " per match"
            );
            goalkeeperInformation.setVisible(true);
            goalkeeperInformation.setManaged(true);
        } else {
            goalkeeperInformation.setVisible(false);
            goalkeeperInformation.setManaged(false);
        }
    }


    /* =====================================================
       PLAYERS DIRECTORY
       ===================================================== */

    private <T> TableCell<T, Integer> createIntegerCenterCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                    setAlignment(Pos.CENTER);
                    getStyleClass().add("table-stat-cell");
                }
            }
        };
    }
    /* =====================================================
       BRACKET
       ===================================================== */

    private void refreshBracket() {
        if (tournament == null) {
            return;
        }

        qualifiedBox.getChildren().clear();
        quarterFinalBox.getChildren().clear();
        semiFinalBox.getChildren().clear();
        finalBox.getChildren().clear();

        refreshQualifiedTeams();

        refreshTwoLegRound(
                tournament.getQuarterFinalFirstLegs(),
                tournament.getQuarterFinalSecondLegs(),
                quarterFinalBox
        );

        refreshTwoLegRound(
                tournament.getSemiFinalFirstLegs(),
                tournament.getSemiFinalSecondLegs(),
                semiFinalBox
        );

        refreshFinal();

        if (tournament.getChampion() == null) {
            championName.setText("NOT DEFINED");
        } else {
            championName.setText(tournament.getChampion().getName());
        }
    }

    private void refreshQualifiedTeams() {
        for (Group group : tournament.getGroups()) {
            VBox card = createBracketCard();

            Label title = new Label(group.getName());
            title.getStyleClass().add("bracket-caption");
            card.getChildren().add(title);

            if (!isGroupFinished(group)) {
                Label pending = new Label("Group stage in progress");
                pending.getStyleClass().add("muted-text");
                card.getChildren().add(pending);
            } else {
                List<Team> qualified = group.getQualifiedTeams();
                for (int i = 0; i < qualified.size() && i < 2; i++) {
                    Label team = new Label((i + 1) + ". " + qualified.get(i).getName());
                    team.getStyleClass().add("bracket-team");
                    card.getChildren().add(team);
                }
            }

            qualifiedBox.getChildren().add(card);
        }
    }

    private boolean isGroupFinished(Group group) {
        if (group.getMatches().isEmpty()) {
            return false;
        }

        for (GroupMatch match : group.getMatches()) {
            if (!match.isPlayed()) {
                return false;
            }
        }

        return true;
    }

    private void refreshTwoLegRound(
            List<? extends Match> firstLegs,
            List<? extends Match> secondLegs,
            VBox container) {

        if (firstLegs.isEmpty()) {
            Label pending = new Label("Not generated yet");
            pending.getStyleClass().add("muted-text");
            container.getChildren().add(pending);
            return;
        }

        for (int i = 0; i < firstLegs.size(); i++) {
            Match first = firstLegs.get(i);
            VBox card = createBracketCard();

            Label teams = new Label(
                    first.getHomeTeam().getName()
                            + "\nvs\n"
                            + first.getAwayTeam().getName()
            );
            teams.getStyleClass().add("bracket-team");

            Label firstResult = new Label("1st leg: " + scoreText(first));
            firstResult.getStyleClass().add("muted-text");

            card.getChildren().addAll(teams, firstResult);

            if (i < secondLegs.size()) {
                Match second = secondLegs.get(i);
                Label secondResult = new Label("2nd leg: " + scoreText(second));
                secondResult.getStyleClass().add("muted-text");

                card.getChildren().add(secondResult);

                if (second.isPlayed() && second.getWinner() != null) {
                    Label winner = new Label("WINNER: " + second.getWinner().getName());
                    winner.getStyleClass().add("green-chip");
                    card.getChildren().add(winner);
                }
            }

            container.getChildren().add(card);
        }
    }

    private void refreshFinal() {
        Match finalMatch = tournament.getFinalMatch();

        if (finalMatch == null) {
            Label pending = new Label("Not generated yet");
            pending.getStyleClass().add("muted-text");
            finalBox.getChildren().add(pending);
            return;
        }

        VBox card = createBracketCard();

        Label teams = new Label(
                finalMatch.getHomeTeam().getName()
                        + "\nvs\n"
                        + finalMatch.getAwayTeam().getName()
        );
        teams.getStyleClass().add("bracket-team");

        Label result = new Label(scoreText(finalMatch));
        result.getStyleClass().add("bracket-score");

        card.getChildren().addAll(teams, result);
        finalBox.getChildren().add(card);
    }

    private VBox createBracketCard() {
        VBox card = new VBox(6);
        card.getStyleClass().add("bracket-card");
        return card;
    }

    private String scoreText(Match match) {
        if (!match.isPlayed()) {
            return "Pending";
        }

        String result = match.getHomeGoals() + " - " + match.getAwayGoals();

        if (match.getHomePenalties() != null) {
            result += " (" + match.getHomePenalties() + " - " + match.getAwayPenalties() + " pens)";
        }

        return result;
    }


    /* =====================================================
       REPORTS (VISTAS RICAS Y DINÁMICAS)
       ===================================================== */

    private void configureReports() {
        reportList.getItems().addAll(
                "I - Top Scorers",
                "II - Fair Play",
                "III - Most Minutes",
                "IV - Championship Bracket",
                "V - Teams",
                "VI - Referees",
                "VII - Players"
        );

        reportList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        generateReport();
                    }
                }
        );
    }

    private void refreshReports() {
        if (reportList.getSelectionModel().getSelectedItem() == null) {
            reportList.getSelectionModel().selectFirst();
        } else {
            generateReport();
        }
    }

    @FXML
    private void handleRefreshReport() {
        generateReport();
    }

    private void generateReport() {
        if (tournament == null) return;

        String selected = reportList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        reportContentHost.getChildren().clear();

        try {
            if (selected.startsWith("I -")) {
                showScorersReportView();
            } else if (selected.startsWith("II -")) {
                showFairPlayReportView();
            } else if (selected.startsWith("III -")) {
                showMinutesReportView();
            } else if (selected.startsWith("IV -")) {
                showBracketSummaryView();
            } else if (selected.startsWith("V -")) {
                showTeamsSummaryView();
            } else if (selected.startsWith("VI -")) {
                showRefereesReportView();
            } else {
                showPlayersSummaryView();
            }
        } catch (Exception ex) {
            Label errorLabel = new Label("Error loading visual report: " + ex.getMessage());
            errorLabel.getStyleClass().add("muted-text");
            reportContentHost.getChildren().add(errorLabel);
            ex.printStackTrace();
        }
    }

    // --- REPORTE I: TOP SCORERS ---
    private void showScorersReportView() {
        selectedReportTitle.setText("Top Goalscorers");
        selectedReportSubtitle.setText("Players ranked by total goals scored in the tournament");

        List<PlayerDirectoryEntry> scorers = new ArrayList<>();
        for (Team team : tournament.getTeams()) {
            for (Player player : team.getSquad()) {
                if (player.getGoals() > 0) {
                    scorers.add(new PlayerDirectoryEntry(player, team));
                }
            }
        }
        scorers.sort(Comparator.comparingInt(PlayerDirectoryEntry::getGoals).reversed()
                .thenComparing(PlayerDirectoryEntry::getMinutesPlayed));

        reportMetricChip.setText(scorers.size() + " SCORERS");

        TableView<PlayerDirectoryEntry> table = new TableView<>();
        table.getStyleClass().add("dark-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<PlayerDirectoryEntry, Number> rankCol = new TableColumn<>("#");
        rankCol.setPrefWidth(45);
        rankCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(table.getItems().indexOf(cell.getValue()) + 1));
        rankCol.setCellFactory(col -> createRankBadgeCell());

        TableColumn<PlayerDirectoryEntry, String> playerCol = new TableColumn<>("PLAYER");
        playerCol.setPrefWidth(200);
        playerCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPlayerName()));
        playerCol.setCellFactory(col -> createBoldCell());

        TableColumn<PlayerDirectoryEntry, String> teamCol = new TableColumn<>("TEAM");
        teamCol.setPrefWidth(180);
        teamCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTeamName()));

        TableColumn<PlayerDirectoryEntry, Integer> matchesCol = new TableColumn<>("MP");
        matchesCol.setPrefWidth(70);
        matchesCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getMatchesPlayed()));
        matchesCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<PlayerDirectoryEntry, Integer> minutesCol = new TableColumn<>("MIN");
        minutesCol.setPrefWidth(80);
        minutesCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getMinutesPlayed()));
        minutesCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<PlayerDirectoryEntry, Integer> goalsCol = new TableColumn<>("GOALS");
        goalsCol.setPrefWidth(90);
        goalsCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getGoals()));
        goalsCol.setCellFactory(col -> createHighlightStatCell());

        table.getColumns().addAll(rankCol, playerCol, teamCol, matchesCol, minutesCol, goalsCol);
        table.getItems().setAll(scorers);

        reportContentHost.getChildren().add(table);
    }

    // --- REPORTE II: FAIR PLAY ---
    private void showFairPlayReportView() {
        selectedReportTitle.setText("Fair Play Ranking");
        selectedReportSubtitle.setText("Teams ordered by lowest disciplinary points (Yellow: 1 pt, Red: 3 pts)");

        TableView<TeamFairPlayEntry> table = new TableView<>();
        table.getStyleClass().add("dark-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TeamFairPlayEntry, Number> rankCol = new TableColumn<>("#");
        rankCol.setPrefWidth(45);
        rankCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(table.getItems().indexOf(cell.getValue()) + 1));
        rankCol.setCellFactory(col -> createRankBadgeCell());

        TableColumn<TeamFairPlayEntry, String> teamCol = new TableColumn<>("TEAM");
        teamCol.setPrefWidth(220);
        teamCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().team.getName()));
        teamCol.setCellFactory(col -> createBoldCell());

        TableColumn<TeamFairPlayEntry, Integer> yellowCol = new TableColumn<>("YELLOW CARDS");
        yellowCol.setPrefWidth(120);
        yellowCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().yellowCards));
        yellowCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setGraphic(null); }
                else {
                    Label l = new Label(String.valueOf(val));
                    l.getStyleClass().add("card-yellow-chip");
                    setGraphic(l); setText(null); setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<TeamFairPlayEntry, Integer> redCol = new TableColumn<>("RED CARDS");
        redCol.setPrefWidth(110);
        redCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().redCards));
        redCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setGraphic(null); }
                else {
                    Label l = new Label(String.valueOf(val));
                    l.getStyleClass().add("card-red-chip");
                    setGraphic(l); setText(null); setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<TeamFairPlayEntry, Integer> scoreCol = new TableColumn<>("PENALTY POINTS");
        scoreCol.setPrefWidth(130);
        scoreCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().totalPoints));
        scoreCol.setCellFactory(col -> createIntegerCenterCell());

        table.getColumns().addAll(rankCol, teamCol, yellowCol, redCol, scoreCol);

        List<TeamFairPlayEntry> entries = new ArrayList<>();
        for (Team team : tournament.getTeams()) {
            int y = 0, r = 0;
            for (Player p : team.getSquad()) {
                y += p.getYellowCards();
                r += p.getRedCards();
            }
            entries.add(new TeamFairPlayEntry(team, y, r, (y * 1) + (r * 3)));
        }
        entries.sort(Comparator.comparingInt(e -> e.totalPoints));
        table.getItems().setAll(entries);

        reportMetricChip.setText(entries.size() + " TEAMS");
        reportContentHost.getChildren().add(table);
    }

    // --- REPORTE III: MOST MINUTES ---
    private void showMinutesReportView() {
        selectedReportTitle.setText("Most Minutes Played");
        selectedReportSubtitle.setText("Players with highest physical endurance and match presence");

        List<PlayerDirectoryEntry> list = new ArrayList<>();
        for (Team team : tournament.getTeams()) {
            for (Player p : team.getSquad()) {
                if (p.getMinutesPlayed() > 0) {
                    list.add(new PlayerDirectoryEntry(p, team));
                }
            }
        }
        list.sort(Comparator.comparingInt(PlayerDirectoryEntry::getMinutesPlayed).reversed());

        TableView<PlayerDirectoryEntry> table = new TableView<>();
        table.getStyleClass().add("dark-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<PlayerDirectoryEntry, Number> rankCol = new TableColumn<>("#");
        rankCol.setPrefWidth(45);
        rankCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(table.getItems().indexOf(cell.getValue()) + 1));
        rankCol.setCellFactory(col -> createRankBadgeCell());

        TableColumn<PlayerDirectoryEntry, String> playerCol = new TableColumn<>("PLAYER");
        playerCol.setPrefWidth(210);
        playerCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPlayerName()));
        playerCol.setCellFactory(col -> createBoldCell());

        TableColumn<PlayerDirectoryEntry, String> teamCol = new TableColumn<>("TEAM");
        teamCol.setPrefWidth(180);
        teamCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTeamName()));

        TableColumn<PlayerDirectoryEntry, Integer> matchesCol = new TableColumn<>("MATCHES");
        matchesCol.setPrefWidth(90);
        matchesCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getMatchesPlayed()));
        matchesCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<PlayerDirectoryEntry, Integer> minutesCol = new TableColumn<>("MINUTES PLAYED");
        minutesCol.setPrefWidth(140);
        minutesCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getMinutesPlayed()));
        minutesCol.setCellFactory(col -> createHighlightStatCell());

        table.getColumns().addAll(rankCol, playerCol, teamCol, matchesCol, minutesCol);
        table.getItems().setAll(list);

        reportMetricChip.setText(list.size() + " PLAYERS");
        reportContentHost.getChildren().add(table);
    }

    // --- REPORTE IV: CHAMPIONSHIP BRACKET ---
    private void showBracketSummaryView() {
        selectedReportTitle.setText("Championship Bracket Summary");
        selectedReportSubtitle.setText("Executive breakdown of qualifiers, knockout series, and tournament outcome");

        VBox contentBox = new VBox(16);
        contentBox.setFillWidth(true);

        HBox summaryHeader = new HBox(16);
        summaryHeader.getStyleClass().add("info-card");
        summaryHeader.setAlignment(Pos.CENTER_LEFT);

        VBox champBox = new VBox(3);
        Label champCaption = new Label("TOURNAMENT CHAMPION");
        champCaption.getStyleClass().add("small-text");
        Label champValue = new Label(tournament.getChampion() != null ? tournament.getChampion().getName().toUpperCase() : "NOT DEFINED YET");
        champValue.getStyleClass().add("champion-text");
        champBox.getChildren().addAll(champCaption, champValue);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox stageBox = new VBox(3);
        Label stageCaption = new Label("CURRENT STATUS");
        stageCaption.getStyleClass().add("small-text");
        Label stageValue = new Label(formatStage());
        stageValue.getStyleClass().add("metric-value");
        stageValue.setStyle("-fx-font-size: 15px;");
        stageBox.getChildren().addAll(stageCaption, stageValue);

        summaryHeader.getChildren().addAll(champBox, spacer, stageBox);
        contentBox.getChildren().add(summaryHeader);

        TableView<Standing> qualifiersTable = new TableView<>();
        qualifiersTable.getStyleClass().add("dark-table");
        qualifiersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        qualifiersTable.setPrefHeight(260);

        TableColumn<Standing, String> groupCol = new TableColumn<>("GROUP");
        groupCol.setPrefWidth(90);
        groupCol.setCellValueFactory(cell -> {
            for (Group g : tournament.getGroups()) {
                if (g.getStandings().contains(cell.getValue())) {
                    return new SimpleStringProperty(g.getName().toUpperCase());
                }
            }
            return new SimpleStringProperty("-");
        });
        groupCol.setCellFactory(col -> createBoldCell());

        TableColumn<Standing, String> teamCol = new TableColumn<>("QUALIFIED CLUB");
        teamCol.setPrefWidth(220);
        teamCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTeam().getName()));
        teamCol.setCellFactory(col -> createBoldCell());

        TableColumn<Standing, Integer> ptsCol = new TableColumn<>("PTS");
        ptsCol.setPrefWidth(65);
        ptsCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getPoints()));
        ptsCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<Standing, Integer> gdCol = new TableColumn<>("GD");
        gdCol.setPrefWidth(65);
        gdCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getGoalDifference()));
        gdCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<Standing, String> statusCol = new TableColumn<>("SEED");
        statusCol.setPrefWidth(120);
        statusCol.setCellValueFactory(cell -> {
            for (Group g : tournament.getGroups()) {
                int index = g.getStandings().indexOf(cell.getValue());
                if (index == 0) return new SimpleStringProperty("Group Winner (1st)");
                if (index == 1) return new SimpleStringProperty("Runner-up (2nd)");
            }
            return new SimpleStringProperty("Qualified");
        });

        qualifiersTable.getColumns().addAll(groupCol, teamCol, ptsCol, gdCol, statusCol);

        List<Standing> confirmedQualifiers = new ArrayList<>();
        for (Group g : tournament.getGroups()) {
            if (isGroupFinished(g) && g.getStandings().size() >= 2) {
                confirmedQualifiers.add(g.getStandings().get(0));
                confirmedQualifiers.add(g.getStandings().get(1));
            }
        }
        qualifiersTable.getItems().setAll(confirmedQualifiers);

        Label tableTitle = new Label("CONFIRMED GROUP STAGE QUALIFIERS");
        tableTitle.getStyleClass().add("section-caption");
        contentBox.getChildren().addAll(tableTitle, qualifiersTable);

        ScrollPane scroll = new ScrollPane(contentBox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("main-scroll");

        reportMetricChip.setText(confirmedQualifiers.size() + " QUALIFIERS");
        reportContentHost.getChildren().add(scroll);
    }

    // --- REPORTE V: TEAMS ---
    private void showTeamsSummaryView() {
        selectedReportTitle.setText("Participating Teams Analysis");
        selectedReportSubtitle.setText("Comprehensive technical and demographic overview of all clubs");

        TableView<TeamSummaryEntry> table = new TableView<>();
        table.getStyleClass().add("dark-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TeamSummaryEntry, String> teamCol = new TableColumn<>("TEAM");
        teamCol.setPrefWidth(180);
        teamCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTeamName()));
        teamCol.setCellFactory(col -> createBoldCell());

        TableColumn<TeamSummaryEntry, String> countryCol = new TableColumn<>("COUNTRY");
        countryCol.setPrefWidth(120);
        countryCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCountryName()));

        TableColumn<TeamSummaryEntry, Integer> rankCol = new TableColumn<>("RANK");
        rankCol.setPrefWidth(70);
        rankCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getRanking()));
        rankCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<TeamSummaryEntry, String> ovrCol = new TableColumn<>("OVR");
        ovrCol.setPrefWidth(75);
        ovrCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.1f", cell.getValue().getOverall())));
        ovrCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setGraphic(null); }
                else {
                    Label b = new Label(val);
                    b.getStyleClass().add("ovr-badge");
                    setGraphic(b); setText(null); setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<TeamSummaryEntry, String> coachCol = new TableColumn<>("HEAD COACH");
        coachCol.setPrefWidth(160);
        coachCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCoachName()));

        TableColumn<TeamSummaryEntry, Integer> squadCol = new TableColumn<>("SQUAD");
        squadCol.setPrefWidth(70);
        squadCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getSquadSize()));
        squadCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<TeamSummaryEntry, String> ageCol = new TableColumn<>("AVG AGE");
        ageCol.setPrefWidth(80);
        ageCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.1f", cell.getValue().getSquadAverageAge())));
        ageCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) setText(null);
                else { setText(val); setAlignment(Pos.CENTER); getStyleClass().add("table-stat-cell"); }
            }
        });

        TableColumn<TeamSummaryEntry, Integer> gfCol = new TableColumn<>("GF");
        gfCol.setPrefWidth(60);
        gfCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getTotalGoalsScored()));
        gfCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<TeamSummaryEntry, Integer> gaCol = new TableColumn<>("GA");
        gaCol.setPrefWidth(60);
        gaCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getTotalGoalsConceded()));
        gaCol.setCellFactory(col -> createIntegerCenterCell());

        table.getColumns().addAll(teamCol, countryCol, rankCol, ovrCol, coachCol, squadCol, ageCol, gfCol, gaCol);

        List<TeamSummaryEntry> list = new ArrayList<>();
        for (Team t : tournament.getTeams()) {
            int squadSize = t.getSquad().size();
            double totalAge = 0;
            int gf = 0;
            int ga = 0;

            for (Player p : t.getSquad()) {
                totalAge += p.getAge();
                gf += p.getGoals();
                if (p instanceof Goalkeeper) {
                    ga += ((Goalkeeper) p).getGoalsConceded();
                }
            }
            double avgAge = squadSize > 0 ? (totalAge / squadSize) : 0;
            list.add(new TeamSummaryEntry(t, squadSize, avgAge, gf, ga));
        }

        list.sort(Comparator.comparing(TeamSummaryEntry::getRanking));
        table.getItems().setAll(list);

        reportMetricChip.setText(list.size() + " CLUBS");
        reportContentHost.getChildren().add(table);
    }

    // --- REPORTE VI: REFEREES ---
    private void showRefereesReportView() {
        selectedReportTitle.setText("Match Officials Registry");
        selectedReportSubtitle.setText("Registered referees, identification details, career experience and assignments");

        TableView<RefereeReportEntry> table = new TableView<>();
        table.getStyleClass().add("dark-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<RefereeReportEntry, Number> rankCol = new TableColumn<>("#");
        rankCol.setPrefWidth(45);
        rankCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(table.getItems().indexOf(cell.getValue()) + 1));
        rankCol.setCellFactory(col -> createRankBadgeCell());

        TableColumn<RefereeReportEntry, String> nameCol = new TableColumn<>("REFEREE");
        nameCol.setPrefWidth(180);
        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        nameCol.setCellFactory(col -> createBoldCell());

        TableColumn<RefereeReportEntry, String> natCol = new TableColumn<>("COUNTRY");
        natCol.setPrefWidth(110);
        natCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNationality()));

        TableColumn<RefereeReportEntry, Integer> expCol = new TableColumn<>("EXP (YRS)");
        expCol.setPrefWidth(80);
        expCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getYearsOfficiated()));
        expCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<RefereeReportEntry, Integer> matchesCol = new TableColumn<>("MATCHES");
        matchesCol.setPrefWidth(85);
        matchesCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getMatchesOfficiated()));
        matchesCol.setCellFactory(col -> createHighlightStatCell());

        TableColumn<RefereeReportEntry, String> birthCol = new TableColumn<>("BIRTH DATE");
        birthCol.setPrefWidth(110);
        birthCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getBirthDate() != null ? cell.getValue().getBirthDate().toString() : "-"));
        birthCol.setCellFactory(col -> createCenterStringCell());

        TableColumn<RefereeReportEntry, String> idTypeCol = new TableColumn<>("ID TYPE");
        idTypeCol.setPrefWidth(85);
        idTypeCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getIdType() != null ? cell.getValue().getIdType().toString() : "-"));
        idTypeCol.setCellFactory(col -> createCenterStringCell());

        TableColumn<RefereeReportEntry, String> idNumCol = new TableColumn<>("ID NUMBER");
        idNumCol.setPrefWidth(110);
        idNumCol.setCellValueFactory(cell -> new SimpleStringProperty(
                String.valueOf(cell.getValue().getIdNumber()))); // <--- Convertido a String
        idNumCol.setCellFactory(col -> createCenterStringCell());

        table.getColumns().addAll(rankCol, nameCol, natCol, expCol, matchesCol, birthCol, idTypeCol, idNumCol);

        List<RefereeReportEntry> list = new ArrayList<>();
        if (tournament.getReferees() != null) {
            for (Referee ref : tournament.getReferees()) {
                String nat = ref.getNationality() != null ? ref.getNationality().getName() : "-";
                list.add(new RefereeReportEntry(
                        ref.getName(),
                        nat,
                        ref.getBirthDate(),
                        ref.getIdType() != null ? ref.getIdType().toString() : "-",
                        ref.getIdNumber(),
                        ref.getYearsOfficiated(),
                        ref.getMatchesOfficiated()
                ));
            }
        }

        list.sort(Comparator.comparingInt(RefereeReportEntry::getMatchesOfficiated).reversed());
        table.getItems().setAll(list);

        reportMetricChip.setText(list.size() + " OFFICIALS");
        reportContentHost.getChildren().add(table);
    }

    // --- REPORTE VII: PLAYERS ---
    private void showPlayersSummaryView() {
        selectedReportTitle.setText("Tournament Players Directory");
        selectedReportSubtitle.setText("Master database of registered athletes with filters, identity records and match stats");

        VBox layout = new VBox(12);
        layout.setFillWidth(true);

        // Barra de Filtros
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("info-card");

        ComboBox<Position> posFilter = new ComboBox<>();
        posFilter.setPromptText("ALL POSITIONS");
        posFilter.getItems().setAll(Position.values());
        posFilter.setPrefWidth(160);

        ComboBox<Team> teamFilter = new ComboBox<>();
        teamFilter.setPromptText("ALL TEAMS");
        teamFilter.getItems().setAll(tournament.getTeams());
        teamFilter.getItems().sort(Comparator.comparing(Team::getName, String.CASE_INSENSITIVE_ORDER));
        teamFilter.setPrefWidth(200);

        teamFilter.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Team item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName());
            }
        });
        teamFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Team item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName());
            }
        });

        Button btnReset = new Button("RESET FILTERS");
        btnReset.getStyleClass().add("secondary-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label();
        countLabel.getStyleClass().add("purple-chip");

        filterBar.getChildren().addAll(
                new Label("POSITION:"), posFilter,
                new Label("TEAM:"), teamFilter,
                btnReset,
                spacer,
                countLabel
        );

        // Tabla de Jugadores
        TableView<PlayerDirectoryEntry> table = new TableView<>();
        table.getStyleClass().add("dark-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<PlayerDirectoryEntry, String> playerCol = new TableColumn<>("PLAYER");
        playerCol.setPrefWidth(170);
        playerCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPlayerName()));
        playerCol.setCellFactory(col -> createBoldCell());

        TableColumn<PlayerDirectoryEntry, String> posCol = new TableColumn<>("POS");
        posCol.setPrefWidth(80);
        posCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPosition().toString()));
        posCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("pos-badge");
                    String posUpper = item.toUpperCase();
                    if (posUpper.contains("GOALKEEPER") || posUpper.contains("GK") || posUpper.contains("ARQUERO")) {
                        badge.getStyleClass().add("pos-gk");
                    } else if (posUpper.contains("DEFENDER") || posUpper.contains("DEF") || posUpper.contains("DEFENSA")) {
                        badge.getStyleClass().add("pos-def");
                    } else if (posUpper.contains("MIDFIELDER") || posUpper.contains("MID") || posUpper.contains("MEDIO")) {
                        badge.getStyleClass().add("pos-mid");
                    } else {
                        badge.getStyleClass().add("pos-fwd");
                    }
                    setGraphic(badge); setText(null); setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<PlayerDirectoryEntry, String> teamCol = new TableColumn<>("TEAM");
        teamCol.setPrefWidth(150);
        teamCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTeamName()));

        TableColumn<PlayerDirectoryEntry, Integer> matchesCol = new TableColumn<>("MP");
        matchesCol.setPrefWidth(60);
        matchesCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getMatchesPlayed()));
        matchesCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<PlayerDirectoryEntry, Integer> minCol = new TableColumn<>("MIN");
        minCol.setPrefWidth(65);
        minCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getMinutesPlayed()));
        minCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<PlayerDirectoryEntry, Integer> goalsCol = new TableColumn<>("GLS");
        goalsCol.setPrefWidth(60);
        goalsCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getGoals()));
        goalsCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<PlayerDirectoryEntry, Integer> concededCol = new TableColumn<>("GA");
        concededCol.setPrefWidth(60);
        concededCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getGoalsConceded()));


        TableColumn<PlayerDirectoryEntry, String> birthCol = new TableColumn<>("BIRTH DATE");
        birthCol.setPrefWidth(100);
        birthCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getPlayer().getBirthDate() != null ? cell.getValue().getPlayer().getBirthDate().toString() : "-"));
        birthCol.setCellFactory(col -> createCenterStringCell());

        TableColumn<PlayerDirectoryEntry, String> idTypeCol = new TableColumn<>("ID TYPE");
        idTypeCol.setPrefWidth(80);
        idTypeCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getPlayer().getIdType() != null ? cell.getValue().getPlayer().getIdType().toString() : "-"));
        idTypeCol.setCellFactory(col -> createCenterStringCell());

        TableColumn<PlayerDirectoryEntry, String> idNumCol = new TableColumn<>("ID NUMBER");
        idNumCol.setPrefWidth(100);
        idNumCol.setCellValueFactory(cell -> new SimpleStringProperty(
                String.valueOf(cell.getValue().getPlayer().getIdNumber())));
        idNumCol.setCellFactory(col -> createCenterStringCell());

        concededCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText("-"); setAlignment(Pos.CENTER); getStyleClass().add("table-stat-cell");
                } else {
                    setText(String.valueOf(item)); setAlignment(Pos.CENTER); getStyleClass().add("table-stat-cell");
                }
            }
        });

        table.getColumns().addAll(playerCol, posCol, teamCol, matchesCol, minCol, goalsCol, concededCol, birthCol, idTypeCol, idNumCol);

        // Carga maestra y configuración de filtros
        ObservableList<PlayerDirectoryEntry> masterList = FXCollections.observableArrayList();
        for (Team t : tournament.getTeams()) {
            for (Player p : t.getSquad()) {
                masterList.add(new PlayerDirectoryEntry(p, t));
            }
        }

        FilteredList<PlayerDirectoryEntry> filteredList = new FilteredList<>(masterList, p -> true);
        SortedList<PlayerDirectoryEntry> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedList);

        Runnable updateFilter = () -> {
            Position pSel = posFilter.getValue();
            Team tSel = teamFilter.getValue();
            filteredList.setPredicate(entry -> {
                boolean matchPos = (pSel == null) || entry.getPosition() == pSel;
                boolean matchTeam = (tSel == null) || (entry.getTeam() != null && entry.getTeam().equals(tSel));
                return matchPos && matchTeam;
            });
            countLabel.setText(filteredList.size() + " PLAYERS");
            reportMetricChip.setText(filteredList.size() + " PLAYERS");
        };

        posFilter.valueProperty().addListener((obs, o, n) -> updateFilter.run());
        teamFilter.valueProperty().addListener((obs, o, n) -> updateFilter.run());

        btnReset.setOnAction(e -> {
            posFilter.setValue(null);
            teamFilter.setValue(null);
            updateFilter.run();
        });

        updateFilter.run();

        layout.getChildren().addAll(filterBar, table);
        reportContentHost.getChildren().add(layout);
    }

    // Helper para celdas de texto centradas
    private <T> TableCell<T, String> createCenterStringCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                } else {
                    setText(val);
                    setAlignment(Pos.CENTER);
                    getStyleClass().add("table-stat-cell");
                }
            }
        };
    }
    private String formatStage() {
        switch (session.getStage()) {
            case GROUP_STAGE:
                return "GROUP STAGE";
            case QUARTER_FINALS:
                return "QUARTER-FINALS";
            case SEMI_FINALS:
                return "SEMI-FINALS";
            case FINAL:
                return "FINAL";
            case FINISHED:
                return "FINISHED";
            default:
                return "NOT STARTED";
        }
    }


    /* =====================================================
       HELPERS & DTOs PARA REPORTES
       ===================================================== */

    private static class TeamFairPlayEntry {
        final Team team;
        final int yellowCards;
        final int redCards;
        final int totalPoints;
        TeamFairPlayEntry(Team team, int y, int r, int pts) {
            this.team = team; this.yellowCards = y; this.redCards = r; this.totalPoints = pts;
        }
    }

    public static class RefereeReportEntry {
        private final String name;
        private final String nationality;
        private final java.time.LocalDate birthDate;
        private final String idType;
        private final int idNumber;
        private final int yearsOfficiated;
        private final int matchesOfficiated;

        public RefereeReportEntry(String name, String nationality, java.time.LocalDate birthDate,
                                  String idType, int idNumber, int yearsOfficiated, int matchesOfficiated) {
            this.name = name;
            this.nationality = nationality;
            this.birthDate = birthDate;
            this.idType = idType;
            this.idNumber = idNumber;
            this.yearsOfficiated = yearsOfficiated;
            this.matchesOfficiated = matchesOfficiated;
        }

        public String getName() { return name; }
        public String getNationality() { return nationality; }
        public java.time.LocalDate getBirthDate() { return birthDate; }
        public String getIdType() { return idType; }
        public int getIdNumber() { return idNumber; }
        public int getYearsOfficiated() { return yearsOfficiated; }
        public int getMatchesOfficiated() { return matchesOfficiated; }
    }    public static class TeamSummaryEntry {
        private final Team team;
        private final int squadSize;
        private final double squadAverageAge;
        private final int totalGoalsScored;
        private final int totalGoalsConceded;

        public TeamSummaryEntry(Team team, int squadSize, double avgAge, int gf, int ga) {
            this.team = team;
            this.squadSize = squadSize;
            this.squadAverageAge = avgAge;
            this.totalGoalsScored = gf;
            this.totalGoalsConceded = ga;
        }

        public String getTeamName() { return team.getName(); }
        public String getCountryName() { return team.getCountry() != null ? team.getCountry().getName() : "-"; }
        public int getRanking() { return team.getRanking(); }
        public double getOverall() { return team.getOverall(); }
        public String getCoachName() { return team.getCoach() != null ? team.getCoach().getName() : "-"; }
        public int getSquadSize() { return squadSize; }
        public double getSquadAverageAge() { return squadAverageAge; }
        public int getTotalGoalsScored() { return totalGoalsScored; }
        public int getTotalGoalsConceded() { return totalGoalsConceded; }
    }

    private <T> TableCell<T, Number> createRankBadgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null); setGraphic(null);
                } else {
                    int rank = val.intValue();
                    Label b = new Label(String.valueOf(rank));
                    b.getStyleClass().add("rank-badge");
                    if (rank == 1) b.getStyleClass().add("rank-gold");
                    else if (rank == 2) b.getStyleClass().add("rank-silver");
                    else if (rank == 3) b.getStyleClass().add("rank-bronze");
                    else b.getStyleClass().add("rank-normal");
                    setGraphic(b); setText(null); setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private <T> TableCell<T, String> createBoldCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); }
                else { setText(val); getStyleClass().add("player-name-cell"); }
            }
        };
    }

    private <T> TableCell<T, Integer> createHighlightStatCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Integer val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); }
                else {
                    setText(String.valueOf(val));
                    setAlignment(Pos.CENTER);
                    getStyleClass().add("metric-value");
                    setStyle("-fx-font-size: 14px;");
                }
            }
        };
    }
}