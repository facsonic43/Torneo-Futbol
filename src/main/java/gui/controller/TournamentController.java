package gui.controller;

import gui.PlayerDirectoryEntry;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.image.ImageView;
import model.match.Event;
import model.match.Formation;
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
import java.util.function.IntFunction;

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
    private ImageView overviewTrophyImage;

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
    private StackPane bracketHost;

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

    private final BracketView bracketView = new BracketView();

    @FXML
    public void initialize() {
        dashboardService = new TournamentDashboardService();
        reportService = new ReportService();

        bracketHost.getChildren().add(bracketView);

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
            overviewChampionName.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: -accent-gold;");

            // Carga de la imagen del trofeo
            java.io.File trophyFile = new java.io.File("Images/trophy.png");
            if (trophyFile.exists()) {
                overviewTrophyImage.setImage(new javafx.scene.image.Image(trophyFile.toURI().toString()));
                overviewTrophyImage.setVisible(true);
                overviewTrophyImage.setManaged(true);
            }
        } else {
            overviewChampionName.setText("NOT WINNER YET");
            overviewChampionName.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -text-muted;");

            overviewTrophyImage.setVisible(false);
            overviewTrophyImage.setManaged(false);
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
    }

    private void refreshGroups() {
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
            VBox groupCard = new VBox(12);
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

    // Cruces de cuartos según el enunciado: {zonaA, posA, zonaB, posB} (índices base 0).
    // I: 1° Z1 vs 2° Z4 | II: 1° Z2 vs 2° Z3 | III: 1° Z3 vs 2° Z1 | IV: 1° Z4 vs 2° Z2
    private static final int[][] QUARTER_SEEDS = {
            {0, 0, 3, 1},
            {1, 0, 2, 1},
            {2, 0, 0, 1},
            {3, 0, 1, 1}
    };

    // Numeración de las series: cuartos I-IV, semifinales V-VI.
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI"};

    private void refreshBracket() {
        if (tournament == null) {
            return;
        }

        Team champion = tournament.getChampion();

        bracketView.setMatches(
                buildRound(
                        tournament.getQuarterFinalFirstLegs(),
                        tournament.getQuarterFinalSecondLegs(),
                        4, 0, "QUARTER-FINAL", champion,
                        this::quarterPlaceholder),
                buildRound(
                        tournament.getSemiFinalFirstLegs(),
                        tournament.getSemiFinalSecondLegs(),
                        2, 4, "SEMI-FINAL", champion,
                        this::semiPlaceholder),
                buildFinalCard(champion)
        );

        bracketView.setActiveRound(activeRound());

        championName.setText(champion == null ? "NOT DEFINED" : champion.getName());
    }

    /* Ronda en juego según la etapa del torneo (-1 si no hay ronda eliminatoria activa). */
    private int activeRound() {
        switch (session.getStage()) {
            case QUARTER_FINALS:
                return 0;
            case SEMI_FINALS:
                return 1;
            case FINAL:
                return 2;
            default:
                return -1;
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

    /* Una tarjeta por cruce; si el cruce no existe todavía, una tarjeta bloqueada. */
    private List<Node> buildRound(
            List<? extends Match> firstLegs,
            List<? extends Match> secondLegs,
            int slots,
            int romanOffset,
            String roundTitle,
            Team champion,
            IntFunction<Node> placeholder) {

        List<Node> cards = new ArrayList<>();

        for (int i = 0; i < slots; i++) {
            if (firstLegs != null && i < firstLegs.size()) {
                Match second = (secondLegs != null && i < secondLegs.size())
                        ? secondLegs.get(i)
                        : null;
                cards.add(twoLegCard(
                        roundTitle + " " + ROMAN[romanOffset + i],
                        firstLegs.get(i),
                        second,
                        champion));
            } else {
                cards.add(placeholder.apply(i));
            }
        }

        return cards;
    }


    /* ---------- Tarjetas con partidos definidos ---------- */

    /*
     * Cruce de ida y vuelta. Estados:
     *  - READY:  los equipos ya están definidos pero la serie no terminó.
     *  - PLAYED: la serie terminó (se resalta el ganador y el criterio).
     */
    private Node twoLegCard(String title, Match first, Match second, Team champion) {
        Team a = first.getHomeTeam();
        Team b = first.getAwayTeam();

        Team winner = (second != null && second.isPlayed()) ? second.getWinner() : null;
        boolean decided = winner != null;
        String criterion = decided ? tieCriterion(first, second, a, b) : null;

        VBox card = new VBox(4,
                teamRow(a.getName(), a.equals(winner), legGoals(first, a), legGoals(second, a)),
                teamRow(b.getName(), b.equals(winner), legGoals(first, b), legGoals(second, b)),
                infoLine("L1", first)
        );

        if (second != null) {
            card.getChildren().add(infoLine("L2", second));
        }

        if (decided) {
            Label criterionLabel = new Label(criterion);
            criterionLabel.getStyleClass().add("bracket-criterion");
            card.getChildren().add(criterionLabel);
        }

        card.getStyleClass().addAll("bracket-card", decided ? "bracket-card-played" : "bracket-card-ready");

        // Marca la tarjeta si el campeón ganó esta serie (para pintar su camino en dorado).
        card.getProperties().put(BracketView.CHAMPION_PATH, decided && winner.equals(champion));

        makeClickable(card, title, first, second,
                decided ? "WINNER: " + winner.getName() + " · " + criterion : null);

        return card;
    }

    private Node buildFinalCard(Team champion) {
        Match finalMatch = tournament.getFinalMatch();

        if (finalMatch == null) {
            return lockedCard("WINNER SF 1", "WINNER SF 2");
        }

        Team a = finalMatch.getHomeTeam();
        Team b = finalMatch.getAwayTeam();

        boolean played = finalMatch.isPlayed();
        String criterion = played ? finalCriterion(finalMatch, a, b) : null;

        VBox card = new VBox(4,
                teamRow(a.getName(), a.equals(champion), finalScore(finalMatch, a)),
                teamRow(b.getName(), b.equals(champion), finalScore(finalMatch, b)),
                infoLine("MATCH", finalMatch)
        );

        if (played) {
            Label criterionLabel = new Label(criterion);
            criterionLabel.getStyleClass().add("bracket-criterion");
            card.getChildren().add(criterionLabel);
        }

        card.getStyleClass().addAll("bracket-card", played ? "bracket-card-played" : "bracket-card-ready");

        // Centramos verticalmente los elementos internos de la tarjeta
        card.setAlignment(Pos.CENTER_LEFT);

        makeClickable(card, "FINAL", finalMatch, null,
                (played && champion != null) ? "CHAMPION: " + champion.getName() + " · " + criterion : null);

        return card;
    }

    /* Fila de un equipo: nombre + una celda por cada marcador. */
    private HBox teamRow(String name, boolean winner, String... scores) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("bracket-team");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        HBox row = new HBox(8, nameLabel);
        row.setAlignment(Pos.CENTER_LEFT);

        for (String score : scores) {
            Label scoreLabel = new Label(score);
            scoreLabel.setMinWidth(22);
            scoreLabel.setAlignment(Pos.CENTER);
            scoreLabel.getStyleClass().add("bracket-score-cell");
            row.getChildren().add(scoreLabel);
        }

        if (winner) {
            row.getStyleClass().add("bracket-team-winner");
        }

        return row;
    }

    /* Línea compacta con fecha, estadio y árbitro; el texto completo va en un tooltip. */
    private Label infoLine(String tag, Match match) {
        Label line = new Label(
                tag + " · " + matchDate(match)
                        + " · " + matchStadium(match)
                        + " · REF " + matchReferee(match)
        );
        line.getStyleClass().add("bracket-info");
        line.setMaxWidth(Double.MAX_VALUE);
        line.setTooltip(new Tooltip(line.getText()));
        return line;
    }

    private void makeClickable(Node card, String title, Match first, Match second, String outcome) {
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(event -> showMatchDetails(title, first, second, outcome));
    }


    /* ---------- Resultados y criterio que definió al ganador ---------- */

    private int goalsOf(Match match, Team team) {
        return match.getHomeTeam().equals(team) ? match.getHomeGoals() : match.getAwayGoals();
    }

    private int goalsAgainst(Match match, Team team) {
        return match.getHomeTeam().equals(team) ? match.getAwayGoals() : match.getHomeGoals();
    }

    /* Goles de un equipo en un partido (sin importar si fue local o visitante). */
    private String legGoals(Match match, Team team) {
        if (match == null || !match.isPlayed()) {
            return "-";
        }
        return String.valueOf(goalsOf(match, team));
    }

    /* Puntos obtenidos en un partido: 3 gana, 1 empata, 0 pierde. */
    private int legPoints(Match match, Team team) {
        if (match == null || !match.isPlayed()) {
            return 0;
        }

        int scored = goalsOf(match, team);
        int conceded = goalsAgainst(match, team);

        if (scored > conceded) {
            return 3;
        }
        return scored == conceded ? 1 : 0;
    }

    private String penalties(Match match, Team team) {
        Integer value = match.getHomeTeam().equals(team)
                ? match.getHomePenalties()
                : match.getAwayPenalties();
        return value == null ? "-" : String.valueOf(value);
    }

    /*
     * Criterio de desempate en series de ida y vuelta, en el orden del reglamento:
     * puntos de ambos partidos, diferencia de gol (goles de visitante doble) y penales.
     */
    private String tieCriterion(Match first, Match second, Team a, Team b) {
        if (second.getHomePenalties() != null) {
            return "BY PENALTIES (" + penalties(second, a) + "-" + penalties(second, b) + ")";
        }

        int pointsA = legPoints(first, a) + legPoints(second, a);
        int pointsB = legPoints(first, b) + legPoints(second, b);

        if (pointsA != pointsB) {
            return "BY AGGREGATE";
        }

        return "BY GOAL DIFFERENCE";
    }

    private String finalCriterion(Match match, Team a, Team b) {
        if (match.getHomePenalties() != null) {
            return "BY PENALTIES (" + penalties(match, a) + "-" + penalties(match, b) + ")";
        }
        return "IN 90 MIN";
    }

    private String finalScore(Match match, Team team) {
        if (!match.isPlayed()) {
            return "-";
        }

        boolean home = match.getHomeTeam().equals(team);
        String goals = String.valueOf(home ? match.getHomeGoals() : match.getAwayGoals());
        Integer pens = home ? match.getHomePenalties() : match.getAwayPenalties();

        return pens != null ? goals + " (" + pens + ")" : goals;
    }


    /* ---------- Tarjetas bloqueadas (cruces que todavía no existen) ---------- */

    private Node lockedCard(String top, String bottom) {
        Label tag = new Label("LOCKED");
        tag.getStyleClass().add("bracket-lock-tag");

        VBox card = new VBox(4, teamRow(top, false), teamRow(bottom, false), tag);
        card.getStyleClass().addAll("bracket-card", "bracket-card-locked");
        return card;
    }

    private Node quarterPlaceholder(int index) {
        int[] seed = QUARTER_SEEDS[index];
        return lockedCard(seedLabel(seed[0], seed[1]), seedLabel(seed[2], seed[3]));
    }

    private Node semiPlaceholder(int index) {
        int firstQf = index * 2 + 1;
        int secondQf = index * 2 + 2;

        return lockedCard(
                "WINNER QF " + firstQf,
                "WINNER QF " + secondQf
        );
    }

    /* Nombre real del clasificado si la zona ya terminó; si no, "1st Group A", etc. */
    private String seedLabel(int groupIndex, int position) {
        List<Group> groups = tournament.getGroups();

        if (groupIndex >= groups.size()) {
            return ordinal(position) + " Group " + (char) ('A' + groupIndex);
        }

        Group group = groups.get(groupIndex);

        if (isGroupFinished(group)) {
            List<Team> qualified = group.getQualifiedTeams();
            if (position < qualified.size()) {
                return qualified.get(position).getName();
            }
        }

        return ordinal(position) + " " + groupDisplayName(group, groupIndex);
    }

    private String ordinal(int position) {
        return position == 0 ? "1st" : "2nd";
    }

    private String groupDisplayName(Group group, int index) {
        String name = group.getName();

        if (name == null || name.isBlank()) {
            return "Group " + (char) ('A' + index);
        }

        String lower = name.toLowerCase();
        return (lower.contains("group") || lower.contains("zone")) ? name : "Group " + name;
    }


    /* ---------- Detalle del partido (click en una tarjeta) ---------- */

    private void showMatchDetails(String title, Match first, Match second, String outcome) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setMinWidth(440);

        // El diálogo es otra ventana: hay que pasarle los estilos y el tema actual.
        if (bracketPane.getScene() != null) {
            dialog.initOwner(bracketPane.getScene().getWindow());
            pane.getStylesheets().addAll(bracketPane.getScene().getStylesheets());

            if (bracketPane.getScene().getRoot().getStyleClass().contains("light-theme")) {
                pane.getStyleClass().add("light-theme");
            }
        }
        pane.getStyleClass().add("app-root");

        VBox content = new VBox(12);
        content.getStyleClass().add("page-content");

        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        content.getChildren().add(heading);

        content.getChildren().add(legDetails(second == null ? "MATCH" : "1ST LEG", first));

        if (second != null) {
            content.getChildren().add(legDetails("2ND LEG", second));
        }

        if (outcome != null) {
            Label outcomeChip = new Label(outcome);
            outcomeChip.getStyleClass().add("green-chip");
            content.getChildren().add(outcomeChip);
        }

        pane.setContent(content);
        dialog.showAndWait();
    }

    private VBox legDetails(String heading, Match match) {
        Label headingLabel = new Label(heading);
        headingLabel.getStyleClass().add("small-text");

        Label teams = new Label(match.getHomeTeam().getName() + "  vs  " + match.getAwayTeam().getName());
        teams.getStyleClass().add("selected-match-title");

        Label score = new Label(scoreText(match));
        score.getStyleClass().add("metric-value");

        VBox box = new VBox(6,
                headingLabel,
                teams,
                score,
                detailRow("DATE", matchDate(match)),
                detailRow("STADIUM", matchStadium(match)),
                detailRow("REFEREE", matchReferee(match))
        );
        box.getStyleClass().add("info-card");
        return box;
    }

    private HBox detailRow(String caption, String value) {
        Label captionLabel = new Label(caption);
        captionLabel.setMinWidth(80);
        captionLabel.getStyleClass().add("small-text");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("info-value");

        return new HBox(10, captionLabel, valueLabel);
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


    /* ---------- ADAPTADORES DEL MODELO ----------
     * Fecha, estadio y árbitro de un partido. Si en tu clase Match los getters
     * se llaman distinto (getDate, getStadium, getReferee), se ajusta solo acá.
     */

    private String matchDate(Match match) {
        return match.getMatchDate() == null ? "TBD" : String.valueOf(match.getMatchDate());
    }

    private String matchStadium(Match match) {
        return match.getStadium() == null ? "TBD" : match.getStadium().getName();
    }

    private String matchReferee(Match match) {
        return match.getReferee() == null ? "TBD" : match.getReferee().getName();
    }


    /* =====================================================
       REPORTS (VISTAS RICAS Y DINÁMICAS)
       ===================================================== */

    private void configureReports() {
        reportList.getItems().addAll(
                "I - Top Scorers",
                "II - Fair Play",
                "III - Most Minutes",
                "IV - Matches Summary",
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
                showMatchesReportView();
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

    // --- REPORT IV: MATCHES SUMMARY & INCIDENCES ---
    private void showMatchesReportView() {
        selectedReportTitle.setText("Matches Log & Incidences");
        selectedReportSubtitle.setText("Comprehensive log of played matches, starting lineups, formations and match events");

        // 1. Get all played matches and sort them by match date
        List<Match> playedMatches = new ArrayList<>();
        if (tournament.getAllMatches() != null) {
            for (Match m : tournament.getAllMatches()) {
                if (m.isPlayed()) {
                    playedMatches.add(m);
                }
            }
        }
        playedMatches.sort(Comparator.comparing(Match::getMatchDate, Comparator.nullsLast(Comparator.naturalOrder())));

        reportMetricChip.setText(playedMatches.size() + " PLAYED MATCHES");

        if (playedMatches.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            Label emptyLabel = new Label("No played matches available yet.");
            emptyLabel.getStyleClass().add("muted-text");
            emptyBox.getChildren().add(emptyLabel);
            reportContentHost.getChildren().add(emptyBox);
            return;
        }

        VBox mainLayout = new VBox(12);
        mainLayout.setFillWidth(true);

        // --- FILTER BAR ---
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("info-card");

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

        ComboBox<String> stageFilter = new ComboBox<>();
        stageFilter.setPromptText("ALL STAGES");
        stageFilter.getItems().setAll("GROUP STAGE", "QUARTER-FINALS", "SEMI-FINALS", "FINAL");
        stageFilter.setPrefWidth(180);

        Button btnReset = new Button("RESET FILTERS");
        btnReset.getStyleClass().add("secondary-button");

        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);

        Label filterCountChip = new Label();
        filterCountChip.getStyleClass().add("purple-chip");

        filterBar.getChildren().addAll(
                new Label("TEAM:"), teamFilter,
                new Label("STAGE:"), stageFilter,
                btnReset,
                filterSpacer,
                filterCountChip
        );

        // --- CONTENT (LIST + DETAILS) ---
        HBox contentLayout = new HBox(16);
        contentLayout.setFillHeight(true);
        VBox.setVgrow(contentLayout, Priority.ALWAYS);

        // Left column: Matches list
        VBox leftSide = new VBox(10);
        leftSide.setPrefWidth(300);

        ListView<Match> playedList = new ListView<>();
        playedList.getStyleClass().add("dark-list");
        VBox.setVgrow(playedList, Priority.ALWAYS);

        // Reactive filter configuration
        ObservableList<Match> masterList = FXCollections.observableArrayList(playedMatches);
        FilteredList<Match> filteredList = new FilteredList<>(masterList, m -> true);
        playedList.setItems(filteredList);

        playedList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Match m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox card = new VBox(2);
                    Label teams = new Label(m.getHomeTeam().getName() + " vs " + m.getAwayTeam().getName());
                    teams.setStyle("-fx-font-weight: 800; -fx-font-size: 12px;");

                    String scoreStr = m.getHomeGoals() + " - " + m.getAwayGoals();
                    if (m.getHomePenalties() != null) {
                        scoreStr += " (" + m.getHomePenalties() + "-" + m.getAwayPenalties() + " pen)";
                    }

                    Label score = new Label(scoreStr + " · " + (m.getMatchDate() != null ? m.getMatchDate().toString() : "TBD"));
                    score.getStyleClass().add("small-text");

                    card.getChildren().addAll(teams, score);
                    setGraphic(card);
                    setText(null);
                }
            }
        });

        leftSide.getChildren().add(playedList);

        // Right column: Dynamic match detail
        VBox rightSide = new VBox(12);
        rightSide.getStyleClass().add("info-card");
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        playedList.getSelectionModel().selectedItemProperty().addListener((obs, oldM, selectedMatch) -> {
            if (selectedMatch != null) {
                renderMatchDetails(rightSide, selectedMatch);
            }
        });

        // Filter update logic
        Runnable updateFilters = () -> {
            Team tSel = teamFilter.getValue();
            String sSel = stageFilter.getValue();

            filteredList.setPredicate(match -> {
                boolean matchTeam = (tSel == null) || match.getHomeTeam().equals(tSel) || match.getAwayTeam().equals(tSel);
                boolean matchStage = true;

                if (sSel != null) {
                    String stageName = getMatchStageName(match).toUpperCase();

                    // Comparación exacta según el valor seleccionado en el ComboBox
                    switch (sSel.toUpperCase()) {
                        case "FINAL":
                            matchStage = stageName.equals("FINAL");
                            break;
                        case "SEMI-FINALS":
                        case "SEMIFINAL":
                            matchStage = stageName.equals("SEMI-FINALS");
                            break;
                        case "QUARTER-FINALS":
                        case "CUARTOS DE FINAL":
                            matchStage = stageName.equals("QUARTER-FINALS");
                            break;
                        case "GROUP STAGE":
                        case "FASE DE GRUPOS":
                            matchStage = stageName.equals("GROUP STAGE");
                            break;
                        default:
                            matchStage = stageName.contains(sSel.toUpperCase());
                            break;
                    }
                }

                return matchTeam && matchStage;
            });

            filterCountChip.setText(filteredList.size() + " MATCHES");

            if (!filteredList.isEmpty()) {
                playedList.getSelectionModel().selectFirst();
            } else {
                rightSide.getChildren().clear();
                Label noResults = new Label("No matches found matching the active filters.");
                noResults.getStyleClass().add("muted-text");
                rightSide.getChildren().add(noResults);
            }
        };

        teamFilter.valueProperty().addListener((obs, o, n) -> updateFilters.run());
        stageFilter.valueProperty().addListener((obs, o, n) -> updateFilters.run());

        btnReset.setOnAction(e -> {
            teamFilter.setValue(null);
            stageFilter.setValue(null);
            updateFilters.run();
        });

        updateFilters.run();

        contentLayout.getChildren().addAll(leftSide, rightSide);
        mainLayout.getChildren().addAll(filterBar, contentLayout);

        reportContentHost.getChildren().add(mainLayout);
    }

    private void renderMatchDetails(VBox container, Match match) {
        container.getChildren().clear();

        // 1. Scoreboard Header & Details
        VBox scoreHeader = new VBox(6);
        scoreHeader.setAlignment(Pos.CENTER);
        scoreHeader.setStyle("-fx-background-color: -bg-panel; -fx-padding: 14; -fx-background-radius: 8; -fx-border-color: -border-card; -fx-border-radius: 8;");

        Label teamsTitle = new Label(match.getHomeTeam().getName().toUpperCase() + "   VS   " + match.getAwayTeam().getName().toUpperCase());
        teamsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: -text-main;");

        String scoreText = match.getHomeGoals() + "  -  " + match.getAwayGoals();
        if (match.getHomePenalties() != null) {
            scoreText += "  (" + match.getHomePenalties() + " - " + match.getAwayPenalties() + " pen)";
        }
        Label scoreLabel = new Label(scoreText);
        scoreLabel.getStyleClass().add("metric-value");

        String stageStr = getMatchStageName(match);
        String infoStr = "Stage: " + stageStr
                + "  |  Date: " + (match.getMatchDate() != null ? match.getMatchDate().toString() : "TBD")
                + "  |  Stadium: " + (match.getStadium() != null ? match.getStadium().getName() : "TBD")
                + "  |  Referee: " + (match.getReferee() != null ? match.getReferee().getName() : "TBD");

        Label matchInfo = new Label(infoStr);
        matchInfo.getStyleClass().add("small-text");

        scoreHeader.getChildren().addAll(teamsTitle, scoreLabel, matchInfo);

        // 2. Buttons to show starting lineups in a modal
        HBox startersButtonsBox = new HBox(12);
        startersButtonsBox.setAlignment(Pos.CENTER);

        Button btnHomeStarters = new Button("SHOW " + match.getHomeTeam().getName().toUpperCase() + " STARTERS");
        btnHomeStarters.getStyleClass().add("secondary-button");
        btnHomeStarters.setOnAction(e -> showStartersModal(match.getHomeTeam(), match.getHomeFormation(), match.getHomeStarters()));

        Button btnAwayStarters = new Button("SHOW " + match.getAwayTeam().getName().toUpperCase() + " STARTERS");
        btnAwayStarters.getStyleClass().add("secondary-button");
        btnAwayStarters.setOnAction(e -> showStartersModal(match.getAwayTeam(), match.getAwayFormation(), match.getAwayStarters()));

        startersButtonsBox.getChildren().addAll(btnHomeStarters, btnAwayStarters);

        // 3. Incidences & Timeline List
        VBox eventsBox = new VBox(6);
        VBox.setVgrow(eventsBox, Priority.ALWAYS);

        Label eventsTitle = new Label("MATCH INCIDENCES & TIMELINE");
        eventsTitle.getStyleClass().add("section-caption");

        ListView<String> eventsList = new ListView<>();
        eventsList.getStyleClass().add("dark-list");
        VBox.setVgrow(eventsList, Priority.ALWAYS);

        List<Event> sortedEvents = new ArrayList<>(match.getEvents());
        sortedEvents.sort(Comparator.comparingInt(Event::getMinute));

        if (sortedEvents.isEmpty()) {
            eventsList.getItems().add("No match incidences recorded.");
        } else {
            for (Event ev : sortedEvents) {
                eventsList.getItems().add(ev.getDescription());
            }
        }

        eventsBox.getChildren().addAll(eventsTitle, eventsList);

        container.getChildren().addAll(scoreHeader, startersButtonsBox, eventsBox);
    }

    // Modal dialog to display squad lineup and formation strategy
    private void showStartersModal(Team team, Formation formation, List<Player> starters) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Starting Lineup - " + team.getName());
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setMinWidth(420);

        if (reportsPane.getScene() != null) {
            dialog.initOwner(reportsPane.getScene().getWindow());
            pane.getStylesheets().addAll(reportsPane.getScene().getStylesheets());

            if (reportsPane.getScene().getRoot().getStyleClass().contains("light-theme")) {
                pane.getStyleClass().add("light-theme");
            }
        }
        pane.getStyleClass().add("app-root");

        VBox content = new VBox(12);
        content.getStyleClass().add("page-content");

        Label heading = new Label(team.getName().toUpperCase());
        heading.getStyleClass().add("section-title");

        String formName = (formation != null) ? formation.getLabel() : "Standard Strategy";
        Label formationLabel = new Label("Tactical Formation: " + formName);
        formationLabel.getStyleClass().add("accent-text");

        ListView<String> startersList = new ListView<>();
        startersList.getStyleClass().add("dark-list");
        startersList.setPrefHeight(320);

        if (starters == null || starters.isEmpty()) {
            startersList.getItems().add("No starting players recorded.");
        } else {
            // Sort tactical order: GK -> DEF -> MID -> FWD
            List<Player> sortedStarters = new ArrayList<>(starters);
            sortedStarters.sort(Comparator.comparingInt(p -> getPositionOrder(p.getPosition())));

            for (Player p : sortedStarters) {
                startersList.getItems().add("[" + p.getPosition() + "]  " + p.getName());
            }
        }

        content.getChildren().addAll(heading, formationLabel, startersList);
        pane.setContent(content);

        dialog.showAndWait();
    }

    // Tactical hierarchy sorting: 1. Goalkeeper, 2. Defender, 3. Midfielder, 4. Forward
    private int getPositionOrder(Position pos) {
        if (pos == null) return 99;
        String name = pos.name().toUpperCase();
        if (name.contains("GOALKEEPER") || name.contains("GK") || name.contains("ARQUERO")) return 1;
        if (name.contains("DEFENDER") || name.contains("DEF") || name.contains("DEFENSA")) return 2;
        if (name.contains("MIDFIELDER") || name.contains("MID") || name.contains("MEDIO")) return 3;
        if (name.contains("FORWARD") || name.contains("FW") || name.contains("DELANTERO")) return 4;
        return 5;
    }

    // Helper method to resolve match stage title
    private String getMatchStageName(Match match) {
        if (match == null) return "Unknown Stage";

        // 1. Final
        if (match.equals(tournament.getFinalMatch())) {
            return "Final";
        }

        // 2. Semifinales (Ida o Vuelta)
        if ((tournament.getSemiFinalFirstLegs() != null && tournament.getSemiFinalFirstLegs().contains(match))
                || (tournament.getSemiFinalSecondLegs() != null && tournament.getSemiFinalSecondLegs().contains(match))) {
            return "Semi-Finals";
        }

        // 3. Cuartos de Final (Ida o Vuelta)
        if ((tournament.getQuarterFinalFirstLegs() != null && tournament.getQuarterFinalFirstLegs().contains(match))
                || (tournament.getQuarterFinalSecondLegs() != null && tournament.getQuarterFinalSecondLegs().contains(match))) {
            return "Quarter-Finals";
        }

        // 4. Si es Knockout por flag pero no cayó en las anteriores
        if (match.isKnockout()) {
            return "Knockout Stage";
        }

        return "Group Stage";
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
    }

    public static class TeamSummaryEntry {
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


    /* =====================================================
       BRACKET VIEW (clase de vista anidada)

       Grilla de 8 filas iguales: cada cuarto ocupa 2 filas,
       cada semi 4 y la final 8, todos centrados verticalmente,
       así cada partido queda en el medio de los dos que lo
       alimentan. Las líneas conectoras se dibujan en una capa
       transparente usando las posiciones reales de las tarjetas.

       Orden esperado: cuartos I, II, III, IV; (0,1) alimentan
       la semi 0 y (2,3) alimentan la semi 1.

       Si una tarjeta tiene la propiedad CHAMPION_PATH en true,
       el tramo que sale de ella se repinta en dorado.
       ===================================================== */

    private static class BracketView extends StackPane {

        static final String CHAMPION_PATH = "championPath";

        private static final int BODY_ROWS = 8;
        private static final double ROW_HEIGHT = 68;
        private static final String[] HEADER_TITLES = {"QUARTER FINALS", "SEMIFINALS", "FINAL"};

        private final GridPane grid = new GridPane();
        private final Pane linesLayer = new Pane();
        private final Label[] headers = new Label[HEADER_TITLES.length];

        private List<Node> quarters = List.of();
        private List<Node> semis = List.of();
        private Node finalMatch;
        private int activeRound = -1;
        private boolean redrawPending;

        BracketView() {
            grid.setHgap(56); // espacio donde se dibujan las líneas
            grid.setVgap(8);
            grid.setAlignment(Pos.CENTER);

            for (int i = 0; i < 3; i++) {
                ColumnConstraints col = new ColumnConstraints();
                col.setPercentWidth(100.0 / 3);
                col.setHgrow(Priority.ALWAYS);
                grid.getColumnConstraints().add(col);
            }

            // Fila 0: títulos de ronda. Filas 1..8: cuerpo del bracket (todas iguales).
            RowConstraints header = new RowConstraints();
            header.setMinHeight(28);
            header.setPrefHeight(28);
            header.setMaxHeight(28);
            grid.getRowConstraints().add(header);

            for (int i = 0; i < BODY_ROWS; i++) {
                RowConstraints row = new RowConstraints();
                row.setMinHeight(ROW_HEIGHT);
                row.setPrefHeight(ROW_HEIGHT);
                row.setVgrow(Priority.ALWAYS);
                grid.getRowConstraints().add(row);
            }

            linesLayer.setMouseTransparent(true);
            linesLayer.setPickOnBounds(false);
            getChildren().addAll(grid, linesLayer);

            // Redibujar los conectores cuando cambia el tamaño disponible.
            widthProperty().addListener((obs, oldValue, newValue) -> requestRedraw());
            heightProperty().addListener((obs, oldValue, newValue) -> requestRedraw());
        }

        /* Reemplaza el contenido del bracket y lo redibuja. */
        void setMatches(List<? extends Node> quarters, List<? extends Node> semis, Node finalMatch) {
            this.quarters = List.copyOf(quarters);
            this.semis = List.copyOf(semis);
            this.finalMatch = finalMatch;
            rebuild();
        }

        /* Resalta el título de la ronda en juego: 0 cuartos, 1 semis, 2 final, -1 ninguna. */
        void setActiveRound(int round) {
            this.activeRound = round;
            applyActiveRound();
        }

        private void applyActiveRound() {
            for (int i = 0; i < headers.length; i++) {
                if (headers[i] == null) {
                    continue;
                }
                headers[i].getStyleClass().remove("bracket-stage-header-active");
                if (i == activeRound) {
                    headers[i].getStyleClass().add("bracket-stage-header-active");
                }
            }
        }

        private void rebuild() {
            grid.getChildren().clear();

            for (int i = 0; i < HEADER_TITLES.length; i++) {
                addHeader(i);
            }
            applyActiveRound();

            place(quarters, 0, 2);
            place(semis, 1, 4);
            if (finalMatch != null) {
                place(List.of(finalMatch), 2, 8);
            }

            requestRedraw();
        }

        private void addHeader(int column) {
            Label label = new Label(HEADER_TITLES[column]);
            label.setMaxWidth(Double.MAX_VALUE);
            label.getStyleClass().add("bracket-stage-header");
            headers[column] = label;
            grid.add(label, column, 0);
        }

        /* Coloca los nodos en una columna; cada uno ocupa 'span' filas, centrado. */
        private void place(List<? extends Node> nodes, int column, int span) {
            for (int i = 0; i < nodes.size(); i++) {
                Node node = nodes.get(i);
                if (node instanceof Region) {
                    Region reg = (Region) node;
                    reg.setMaxWidth(Double.MAX_VALUE);

                    // Fijamos un alto máximo idéntico para todas las tarjetas (el tamaño estándar de cuartos)
                    reg.setMaxHeight(125);
                    reg.setPrefHeight(125);
                }

                grid.add(node, column, 1 + i * span, 1, span);
                GridPane.setValignment(node, VPos.CENTER);
            }
        }

        /* Agrupa varios pedidos de redibujo en uno solo, después del layout. */
        private void requestRedraw() {
            if (redrawPending) {
                return;
            }
            redrawPending = true;
            Platform.runLater(() -> {
                redrawPending = false;
                applyCss();
                layout();
                drawLines();
            });
        }

        private void drawLines() {
            linesLayer.getChildren().clear();

            if (quarters.size() != 4 || semis.size() != 2 || finalMatch == null) {
                return;
            }

            connect(quarters.get(0), quarters.get(1), semis.get(0));
            connect(quarters.get(2), quarters.get(3), semis.get(1));
            connect(semis.get(0), semis.get(1), finalMatch);
        }

        /*
         * Conector en codo: [a] y [b] salen hacia una línea vertical que llega a [target].
         * Si alguno de los dos es parte del camino del campeón, ese tramo se repinta en dorado.
         */
        private void connect(Node a, Node b, Node target) {
            Bounds ba = toLayer(a);
            Bounds bb = toLayer(b);
            Bounds bt = toLayer(target);

            double midX = (ba.getMaxX() + bt.getMinX()) / 2;
            double targetY = bt.getCenterY();

            Path base = new Path(
                    new MoveTo(ba.getMaxX(), ba.getCenterY()),
                    new LineTo(midX, ba.getCenterY()),
                    new LineTo(midX, bb.getCenterY()),
                    new LineTo(bb.getMaxX(), bb.getCenterY()),
                    new MoveTo(midX, targetY),
                    new LineTo(bt.getMinX(), targetY));

            base.setFill(Color.TRANSPARENT);
            base.getStyleClass().add("bracket-line");
            linesLayer.getChildren().add(base);

            for (Node feeder : new Node[]{a, b}) {
                if (!isChampionPath(feeder)) {
                    continue;
                }

                Bounds bf = toLayer(feeder);

                Path gold = new Path(
                        new MoveTo(bf.getMaxX(), bf.getCenterY()),
                        new LineTo(midX, bf.getCenterY()),
                        new LineTo(midX, targetY),
                        new LineTo(bt.getMinX(), targetY));

                gold.setFill(Color.TRANSPARENT);
                gold.getStyleClass().add("bracket-line-champion");
                linesLayer.getChildren().add(gold);
            }
        }

        private boolean isChampionPath(Node node) {
            return Boolean.TRUE.equals(node.getProperties().get(CHAMPION_PATH));
        }

        private Bounds toLayer(Node node) {
            return linesLayer.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
        }
    }
}