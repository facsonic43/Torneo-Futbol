package gui.controller;

import gui.StatisticsReportView;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import model.match.Match;
import model.participant.Player;
import model.participant.Team;
import model.tournament.Group;
import model.tournament.Standing;
import model.tournament.Tournament;
import reports.ParticipantReportExporter;
import service.TournamentSessionService;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import gui.BracketSection;
import gui.BracketView;
import gui.report.BracketReportView;
import gui.report.CoachesReportView;
import gui.report.FairPlayReportView;
import gui.report.MatchesReportView;
import gui.report.MostMinutesReportView;
import gui.report.PlayersReportView;
import gui.report.RefereesReportView;
import gui.report.ReportView;
import gui.report.TeamsReportView;
import gui.report.TopScorersReportView;
import java.util.HashMap;
import java.util.Map;
import static gui.TableCells.createCenterCell;
import static gui.MatchFormat.scoreText;

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

    private final Map<String, ReportView> reportViews = new HashMap<>();

    private final BracketView bracketView = new BracketView();
    private BracketSection bracketSection;

    @FXML
    public void initialize() {
        bracketSection = new BracketSection(bracketView, reportsPane, () -> session.getStage());

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
        refreshReports();
    }

    /* =====================================================
       NAVEGACIÓN INTERNA
       ===================================================== */

    public void showOverview() {
        showPane(overviewPane);
    }

    public void showReports() {
        showPane(reportsPane);
    }

    public void showBracket() {
        showPane(bracketPane);
    }

    private void showPane(Node selectedPane) {
        if (overviewPane != null) {
            overviewPane.setVisible(selectedPane == overviewPane);
            overviewPane.setManaged(selectedPane == overviewPane);
        }

        if (teamsPane != null) {
            teamsPane.setVisible(selectedPane == teamsPane);
            teamsPane.setManaged(selectedPane == teamsPane);
        }

        if (bracketPane != null) {
            bracketPane.setVisible(selectedPane == bracketPane);
            bracketPane.setManaged(selectedPane == bracketPane);
        }

        if (reportsPane != null) {
            reportsPane.setVisible(selectedPane == reportsPane);
            reportsPane.setManaged(selectedPane == reportsPane);
        }
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
        if (overviewQuarterBox == null || overviewSemiBox == null || overviewFinalBox == null) return;

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

            Team winner = finalMatch.isPlayed() ? finalMatch.getWinner() : null;
            Team home = finalMatch.getHomeTeam();
            Team away = finalMatch.getAwayTeam();

            HBox teamsLine = new HBox(6);
            teamsLine.setAlignment(Pos.CENTER_LEFT);

            Label homeLabel = new Label(home.getName());
            Label vsLabel = new Label("vs");
            vsLabel.getStyleClass().add("muted-text");
            Label awayLabel = new Label(away.getName());

            if (winner != null) {
                if (home.equals(winner)) {
                    homeLabel.getStyleClass().add("team-passed");
                    awayLabel.getStyleClass().add("team-eliminated");
                } else {
                    homeLabel.getStyleClass().add("team-eliminated");
                    awayLabel.getStyleClass().add("team-passed");
                }
            } else {
                homeLabel.getStyleClass().add("team-neutral");
                awayLabel.getStyleClass().add("team-neutral");
            }

            teamsLine.getChildren().addAll(homeLabel, vsLabel, awayLabel);

            Label score = new Label(finalMatch.isPlayed() ? scoreText(finalMatch) : "NOT PLAYED YET");
            score.getStyleClass().add(finalMatch.isPlayed() ? "metric-value" : "muted-text");
            score.setStyle("-fx-font-size: 13px;");

            matchCard.getChildren().addAll(teamsLine, score);
            overviewFinalBox.getChildren().add(matchCard);
        }

        // 4. Campeón
        if (overviewChampionName != null) {
            if (tournament.getChampion() != null) {
                overviewChampionName.setText(tournament.getChampion().getName().toUpperCase());
                overviewChampionName.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: -status-qualified-text;");

                File trophyFile = new File("Images/trophy.png");
                if (trophyFile.exists() && overviewTrophyImage != null) {
                    overviewTrophyImage.setImage(new javafx.scene.image.Image(trophyFile.toURI().toString()));
                    overviewTrophyImage.setVisible(true);
                    overviewTrophyImage.setManaged(true);
                }
            } else {
                overviewChampionName.setText("NOT WINNER YET");
                overviewChampionName.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -text-muted;");

                if (overviewTrophyImage != null) {
                    overviewTrophyImage.setVisible(false);
                    overviewTrophyImage.setManaged(false);
                }
            }
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

            VBox seriesBox = new VBox(4);
            seriesBox.getStyleClass().add("bracket-card");

            Team winner = (m2 != null && m2.isPlayed()) ? m2.getWinner() : null;
            Team home = m1.getHomeTeam();
            Team away = m1.getAwayTeam();

            HBox teamsLine = new HBox(6);
            teamsLine.setAlignment(Pos.CENTER_LEFT);

            Label homeLabel = new Label(home.getName());
            Label vsLabel = new Label("vs");
            vsLabel.getStyleClass().add("muted-text");
            Label awayLabel = new Label(away.getName());

            if (winner != null) {
                if (home.equals(winner)) {
                    homeLabel.getStyleClass().add("team-passed");
                    awayLabel.getStyleClass().add("team-eliminated");
                } else {
                    homeLabel.getStyleClass().add("team-eliminated");
                    awayLabel.getStyleClass().add("team-passed");
                }
            } else {
                homeLabel.getStyleClass().add("team-neutral");
                awayLabel.getStyleClass().add("team-neutral");
            }

            teamsLine.getChildren().addAll(homeLabel, vsLabel, awayLabel);

            String s1 = m1.isPlayed() ? (m1.getHomeGoals() + "-" + m1.getAwayGoals()) : "-";
            String s2 = (m2 != null && m2.isPlayed()) ? (m2.getHomeGoals() + "-" + m2.getAwayGoals()) : "-";

            Label res = new Label(m1.isPlayed() ? ("Leg 1: " + s1 + " | Leg 2: " + s2) : "NOT PLAYED YET");
            res.getStyleClass().add(m1.isPlayed() ? "small-text" : "muted-text");

            seriesBox.getChildren().addAll(teamsLine, res);
            container.getChildren().add(seriesBox);
        }
    }

    private void refreshGroups() {
        if (allGroupsContainer == null) return;
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

            HBox header = new HBox();
            header.getStyleClass().add("group-card-header");
            Label title = new Label(group.getName().toUpperCase());
            title.getStyleClass().add("group-card-title");
            header.getChildren().add(title);
            groupCard.getChildren().add(header);

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

            List<Standing> standings = group.getStandings();
            for (int i = 0; i < standings.size(); i++) {
                Standing standing = standings.get(i);
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);

                if (i < 2) {
                    row.getStyleClass().add("standing-qualified");
                } else {
                    row.getStyleClass().add("standing-eliminated");
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
        if (teamList == null) return;

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
        if (playerTable == null) return;

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

    private void refreshTeams() {
        if (tournament == null || teamList == null) {
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
        if (teamName != null) teamName.setText(team.getName());
        if (teamCountry != null) teamCountry.setText(team.getCountry().getName().toUpperCase());
        if (teamRanking != null) teamRanking.setText("#" + team.getRanking() + " CONTINENTAL RANKING");
        if (teamOverall != null) teamOverall.setText(String.format("%.1f", team.getOverall()));

        if (team.getCoach() != null) {
            if (coachName != null) coachName.setText(team.getCoach().getName());

            String titles = team.getCoach().getTitlesObtained() + " titles";
            String country = team.getCoach().getNationality() != null
                    ? team.getCoach().getNationality().getName()
                    : "";

            if (coachDetails != null) coachDetails.setText(country.isEmpty() ? titles : country + " • " + titles);
        } else {
            if (coachName != null) coachName.setText("-");
            if (coachDetails != null) coachDetails.setText("No coach registered");
        }

        if (playerTable != null) {
            playerTable.getItems().setAll(team.getSquad());

            if (!playerTable.getItems().isEmpty()) {
                playerTable.getSelectionModel().selectFirst();
            }
        }
    }

    private void showPlayer(Player player) {
        if (playerName != null) playerName.setText(player.getName());
        if (playerPosition != null) playerPosition.setText(player.getPosition().toString());
        if (playerNationality != null) playerNationality.setText(player.getNationality().getName());
        if (playerDocument != null) playerDocument.setText(player.getIdType() + " " + player.getIdNumber());
        if (playerAge != null) playerAge.setText(player.getAge() + " years");
        if (playerRating != null) playerRating.setText(String.format("%.1f", player.getOverall()));

        if (playerStats != null) {
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
        }

        if (playerDiscipline != null) {
            playerDiscipline.setText(
                    player.getYellowCards()
                            + " yellow | "
                            + player.getRedCards()
                            + " red"
            );
        }

        if (playerAvailability != null) {
            if (player.isSuspended()) {
                playerAvailability.setText("SUSPENDED");
            } else if (player.isInjured()) {
                playerAvailability.setText("INJURED");
            } else {
                playerAvailability.setText("AVAILABLE");
            }
        }

        if (goalkeeperInformation != null) {
            if (player.isGoalkeeper()) {
                Player goalkeeper = player;
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
    }

    /* =====================================================
       REPORTS (VISTAS RICAS Y DINÁMICAS)
       ===================================================== */

    private void configureReports() {
        if (reportList == null) return;

        StatisticsReportView statistics = new StatisticsReportView();
        reportViews.put("Top Scorers", new TopScorersReportView(reportContentHost, reportMetricChip,
                selectedReportSubtitle, selectedReportTitle, reportsPane, statistics));
        reportViews.put("Fair Play", new FairPlayReportView(reportContentHost, reportMetricChip,
                selectedReportSubtitle, selectedReportTitle, reportsPane, statistics));
        reportViews.put("Most Minutes", new MostMinutesReportView(reportContentHost, reportMetricChip,
                selectedReportSubtitle, selectedReportTitle, reportsPane, statistics));
        reportViews.put("Championship Bracket", new BracketReportView(reportContentHost, reportMetricChip,
                selectedReportSubtitle, selectedReportTitle, reportsPane, bracketSection));
        reportViews.put("Matches Summary", new MatchesReportView(reportContentHost, reportMetricChip,
                selectedReportSubtitle, selectedReportTitle, reportsPane));
        reportViews.put("Teams", new TeamsReportView(reportContentHost, reportMetricChip,
                selectedReportSubtitle, selectedReportTitle, reportsPane));
        reportViews.put("Coaches", new CoachesReportView(reportContentHost, reportMetricChip,
                selectedReportSubtitle, selectedReportTitle, reportsPane));
        reportViews.put("Referees", new RefereesReportView(reportContentHost, reportMetricChip,
                selectedReportSubtitle, selectedReportTitle, reportsPane));
        reportViews.put("Players", new PlayersReportView(reportContentHost, reportMetricChip,
                selectedReportSubtitle, selectedReportTitle, reportsPane));

        reportList.getItems().clear();
        reportList.getItems().addAll(
                "Top Scorers",
                "Fair Play",
                "Most Minutes",
                "Championship Bracket",
                "Matches Summary",
                "Teams",
                "Coaches",
                "Referees",
                "Players"
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
        if (reportList == null) return;

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

    public void handleGeneratePdf(Window owner) {
        if (tournament == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save participant report");
        chooser.setInitialFileName("Tournament-Participants.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));
        File destination = chooser.showSaveDialog(owner);
        if (destination == null) return;

        if (!destination.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            destination = new File(destination.getParentFile(), destination.getName() + ".pdf");
        }

        try {
            ParticipantReportExporter.export(tournament, destination.toPath());

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("PDF generated");
            success.setHeaderText("Participant report saved");
            success.setContentText(destination.getAbsolutePath());
            success.initOwner(owner);
            success.showAndWait();
        } catch (IOException | RuntimeException ex) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("PDF export failed");
            error.setHeaderText("Could not generate the participant report");
            error.setContentText(ex.getMessage());
            error.initOwner(owner);
            error.showAndWait();
        }
    }

    @FXML
    public void handleShowReportsPane(ActionEvent event) {
        showPane(reportsPane);
        handleRefreshReport();
    }

    private void generateReport() {
        if (tournament == null || reportList == null) return;

        String selected = reportList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            ReportView view = reportViews.get(selected);
            if (view == null) {
                throw new IllegalArgumentException("Unknown report: " + selected);
            }

            if (reportContentHost != null) {
                reportContentHost.getChildren().clear();
                view.show(tournament);
            }

        } catch (Exception ex) {
            if (reportContentHost != null) {
                Label errorLabel = new Label("Error loading visual report: " + ex.getMessage());
                errorLabel.getStyleClass().add("muted-text");
                reportContentHost.getChildren().setAll(errorLabel);
            }
            ex.printStackTrace();
        }
    }

    private String formatStage() {
        return session.getStage().getLabel();
    }
}
