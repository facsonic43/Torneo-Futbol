package gui.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.match.GroupMatch;
import model.match.Match;
import model.participant.Goalkeeper;
import model.participant.Player;
import model.participant.Position;
import model.participant.Team;
import model.tournament.Group;
import model.tournament.Standing;
import model.tournament.Tournament;
import report.ReportService;
import service.TournamentDashboardService;
import service.TournamentSessionService;

import java.util.Comparator;
import java.util.List;

/*
 * Controla las pantallas informativas del campeonato:
 * overview, equipos, cuadro eliminatorio y reportes.
 *
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
    private VBox groupButtonsBox;

    @FXML
    private VBox standingsBox;

    @FXML
    private Label overviewStage;

    @FXML
    private Label overviewMatches;

    @FXML
    private Label overviewChampion;

    @FXML
    private Label overviewScorer;

    @FXML
    private Label overviewMinutes;

    @FXML
    private Label overviewFairPlay;

    @FXML
    private Label selectedGroupTitle;


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
    private ComboBox<Position> positionFilter;

    @FXML
    private TextArea reportArea;

    @FXML
    private Label selectedReportTitle;


    private TournamentSessionService session;
    private Tournament tournament;

    private TournamentDashboardService dashboardService;
    private ReportService reportService;

    private int selectedGroupIndex;

    @FXML
    public void initialize() {

        dashboardService =
                new TournamentDashboardService();

        reportService =
                new ReportService();

        selectedGroupIndex =
                0;

        configureTeamList();

        configurePlayerTable();

        configureReports();
    }

    public void setSession(
            TournamentSessionService session) {

        this.session =
                session;

        tournament =
                session.getTournament();

        refreshAll();
    }

    public void refreshAll() {

        if (session == null) {
            return;
        }

        tournament =
                session.getTournament();

        refreshOverview();

        refreshTeams();

        refreshBracket();

        refreshReports();
    }


    /*
     * =====================================================
     * NAVEGACIÓN INTERNA
     * =====================================================
     */

    public void showOverview() {

        showPane(
                overviewPane
        );
    }

    public void showTeams() {

        showPane(
                teamsPane
        );
    }

    public void showBracket() {

        showPane(
                bracketPane
        );
    }

    public void showReports() {

        showPane(
                reportsPane
        );
    }

    private void showPane(
            Node selectedPane) {

        overviewPane.setVisible(
                selectedPane == overviewPane
        );

        overviewPane.setManaged(
                selectedPane == overviewPane
        );

        teamsPane.setVisible(
                selectedPane == teamsPane
        );

        teamsPane.setManaged(
                selectedPane == teamsPane
        );

        bracketPane.setVisible(
                selectedPane == bracketPane
        );

        bracketPane.setManaged(
                selectedPane == bracketPane
        );

        reportsPane.setVisible(
                selectedPane == reportsPane
        );

        reportsPane.setManaged(
                selectedPane == reportsPane
        );
    }


    /*
     * =====================================================
     * OVERVIEW
     * =====================================================
     */

    private void refreshOverview() {

        if (tournament == null) {
            return;
        }

        overviewStage.setText(
                formatStage()
        );

        int played =
                dashboardService
                        .getPlayedMatches(
                                tournament
                        );

        overviewMatches.setText(
                played
                        + " / 37"
        );

        if (tournament
                .getChampion()
                == null) {

            overviewChampion.setText(
                    "Not defined"
            );

        } else {

            overviewChampion.setText(
                    tournament
                            .getChampion()
                            .getName()
            );
        }

        Player scorer =
                dashboardService
                        .getTopScorer(
                                tournament
                        );

        if (scorer == null) {

            overviewScorer.setText(
                    "No scorer yet"
            );

        } else {

            overviewScorer.setText(
                    scorer.getName()
                            + " - "
                            + scorer.getGoals()
                            + " goals"
            );
        }

        Player minutesPlayer =
                dashboardService
                        .getMostMinutesPlayer(
                                tournament
                        );

        if (minutesPlayer == null) {

            overviewMinutes.setText(
                    "No matches played"
            );

        } else {

            overviewMinutes.setText(
                    minutesPlayer.getName()
                            + " - "
                            + minutesPlayer
                            .getMinutesPlayed()
                            + " min"
            );
        }

        Team fairPlay =
                dashboardService
                        .getFairPlayTeam(
                                tournament
                        );

        overviewFairPlay.setText(
                fairPlay == null
                        ? "-"
                        : fairPlay.getName()
        );

        refreshGroups();
    }

    private void refreshGroups() {

        groupButtonsBox
                .getChildren()
                .clear();

        List<Group> groups =
                tournament.getGroups();

        if (groups.isEmpty()) {

            standingsBox
                    .getChildren()
                    .clear();

            return;
        }

        if (selectedGroupIndex
                >= groups.size()) {

            selectedGroupIndex =
                    0;
        }

        for (int i = 0;
             i < groups.size();
             i++) {

            Group group =
                    groups.get(i);

            Button button =
                    new Button(
                            group.getName()
                    );

            button.setMaxWidth(
                    Double.MAX_VALUE
            );

            button.getStyleClass()
                    .add(
                            "group-button"
                    );

            if (i == selectedGroupIndex) {

                button.getStyleClass()
                        .add(
                                "group-button-active"
                        );
            }

            int index =
                    i;

            button.setOnAction(
                    event -> {

                        selectedGroupIndex =
                                index;

                        refreshGroups();
                    }
            );

            groupButtonsBox
                    .getChildren()
                    .add(
                            button
                    );
        }

        refreshGroupTable();
    }

    private void refreshGroupTable() {

        standingsBox
                .getChildren()
                .clear();

        if (tournament
                .getGroups()
                .isEmpty()) {

            return;
        }

        Group group =
                tournament
                        .getGroups()
                        .get(
                                selectedGroupIndex
                        );

        selectedGroupTitle.setText(
                group.getName()
                        .toUpperCase()
        );

        List<Standing> standings =
                group.getStandings();

        for (int i = 0;
             i < standings.size();
             i++) {

            Standing standing =
                    standings.get(i);

            HBox row =
                    new HBox(
                            10
                    );

            row.setAlignment(
                    Pos.CENTER_LEFT
            );

            if (i < 2) {

                row.getStyleClass()
                        .add(
                                "standing-qualified"
                        );

            } else {

                row.getStyleClass()
                        .add(
                                "standing-normal"
                        );
            }

            row.getChildren()
                    .addAll(
                            createColumn(
                                    String.valueOf(
                                            i + 1
                                    ),
                                    35
                            ),
                            createColumn(
                                    standing
                                            .getTeam()
                                            .getName(),
                                    210
                            ),
                            createColumn(
                                    String.valueOf(
                                            standing
                                                    .getPoints()
                                    ),
                                    55
                            ),
                            createColumn(
                                    String.valueOf(
                                            standing
                                                    .getPlayed()
                                    ),
                                    55
                            ),
                            createColumn(
                                    String.valueOf(
                                            standing
                                                    .getScored()
                                    ),
                                    55
                            ),
                            createColumn(
                                    String.valueOf(
                                            standing
                                                    .getConceded()
                                    ),
                                    55
                            ),
                            createColumn(
                                    formatGoalDifference(
                                            standing
                                                    .getGoalDifference()
                                    ),
                                    55
                            )
                    );

            standingsBox
                    .getChildren()
                    .add(
                            row
                    );
        }
    }

    private Label createColumn(
            String text,
            double width) {

        Label label =
                new Label(
                        text
                );

        label.setPrefWidth(
                width
        );

        return label;
    }

    private String formatGoalDifference(
            int value) {

        if (value > 0) {

            return "+"
                    + value;
        }

        return String.valueOf(
                value
        );
    }


    /*
     * =====================================================
     * TEAMS
     * =====================================================
     */

    private void configureTeamList() {

        /*
         * Evita mostrar model.participant.Team@...
         * sin modificar la clase de dominio Team.
         */
        teamList.setCellFactory(
                listView ->
                        new ListCell<>() {

                            @Override
                            protected void updateItem(
                                    Team team,
                                    boolean empty) {

                                super.updateItem(
                                        team,
                                        empty
                                );

                                if (empty
                                        || team == null) {

                                    setText(
                                            null
                                    );

                                } else {

                                    setText(
                                            team.getName()
                                    );
                                }
                            }
                        }
        );

        teamList
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable,
                         oldTeam,
                         newTeam) -> {

                            if (newTeam != null) {

                                showTeam(
                                        newTeam
                                );
                            }
                        }
                );
    }

    private void configurePlayerTable() {

        playerNameColumn
                .setCellValueFactory(
                        value ->
                                new SimpleStringProperty(
                                        value
                                                .getValue()
                                                .getName()
                                )
                );

        playerPositionColumn
                .setCellValueFactory(
                        value ->
                                new SimpleStringProperty(
                                        value
                                                .getValue()
                                                .getPosition()
                                                .toString()
                                )
                );

        playerOverallColumn
                .setCellValueFactory(
                        value ->
                                new SimpleStringProperty(
                                        String.format(
                                                "%.1f",
                                                value
                                                        .getValue()
                                                        .getOverall()
                                        )
                                )
                );

        playerMatchesColumn
                .setCellValueFactory(
                        value ->
                                new SimpleStringProperty(
                                        String.valueOf(
                                                value
                                                        .getValue()
                                                        .getMatchesPlayed()
                                        )
                                )
                );

        playerMinutesColumn
                .setCellValueFactory(
                        value ->
                                new SimpleStringProperty(
                                        String.valueOf(
                                                value
                                                        .getValue()
                                                        .getMinutesPlayed()
                                        )
                                )
                );

        playerGoalsColumn
                .setCellValueFactory(
                        value ->
                                new SimpleStringProperty(
                                        String.valueOf(
                                                value
                                                        .getValue()
                                                        .getGoals()
                                        )
                                )
                );

        playerTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable,
                         oldPlayer,
                         newPlayer) -> {

                            if (newPlayer != null) {

                                showPlayer(
                                        newPlayer
                                );
                            }
                        }
                );
    }

    private void refreshTeams() {

        if (tournament == null) {
            return;
        }

        Team previousSelection =
                teamList
                        .getSelectionModel()
                        .getSelectedItem();

        teamList
                .getItems()
                .setAll(
                        tournament.getTeams()
                );

        teamList
                .getItems()
                .sort(
                        Comparator.comparing(
                                Team::getName,
                                String.CASE_INSENSITIVE_ORDER
                        )
                );

        if (previousSelection != null
                && teamList
                .getItems()
                .contains(
                        previousSelection
                )) {

            teamList
                    .getSelectionModel()
                    .select(
                            previousSelection
                    );

        } else if (!teamList
                .getItems()
                .isEmpty()) {

            teamList
                    .getSelectionModel()
                    .selectFirst();
        }
    }

    private void showTeam(
            Team team) {

        teamName.setText(
                team.getName()
        );

        teamCountry.setText(
                team.getCountry()
                        .getName()
        );

        teamRanking.setText(
                "#"
                        + team.getRanking()
                        + " Continental Ranking"
        );

        teamOverall.setText(
                String.format(
                        "%.1f",
                        team.getOverall()
                )
        );

        if (team.getCoach()
                != null) {

            coachName.setText(
                    team.getCoach()
                            .getName()
                            + " | "
                            + team.getCoach()
                            .getNationality()
                            .getName()
                            + " | "
                            + team.getCoach()
                            .getTitlesObtained()
                            + " titles"
            );

        } else {

            coachName.setText(
                    "-"
            );
        }

        playerTable
                .getItems()
                .setAll(
                        team.getSquad()
                );

        if (!playerTable
                .getItems()
                .isEmpty()) {

            playerTable
                    .getSelectionModel()
                    .selectFirst();
        }
    }

    private void showPlayer(
            Player player) {

        playerName.setText(
                player.getName()
        );

        playerPosition.setText(
                player.getPosition()
                        .toString()
        );

        playerNationality.setText(
                player.getNationality()
                        .getName()
        );

        playerDocument.setText(
                player.getIdType()
                        + " "
                        + player.getIdNumber()
        );

        playerAge.setText(
                player.getAge()
                        + " years"
        );

        playerRating.setText(
                String.format(
                        "%.1f",
                        player.getOverall()
                )
        );

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

            playerAvailability.setText(
                    "SUSPENDED"
            );

        } else if (player.isInjured()) {

            playerAvailability.setText(
                    "INJURED"
            );

        } else {

            playerAvailability.setText(
                    "AVAILABLE"
            );
        }

        if (player
                instanceof Goalkeeper) {

            Goalkeeper goalkeeper =
                    (Goalkeeper) player;

            double average =
                    0.0;

            if (goalkeeper
                    .getMatchesPlayed()
                    > 0) {

                average =
                        goalkeeper
                                .getGoalsConceded()
                                * 1.0
                                / goalkeeper
                                .getMatchesPlayed();
            }

            goalkeeperInformation.setText(
                    goalkeeper
                            .getGoalsConceded()
                            + " goals conceded | "
                            + String.format(
                            "%.2f",
                            average
                    )
                            + " per match"
            );

            goalkeeperInformation.setVisible(
                    true
            );

            goalkeeperInformation.setManaged(
                    true
            );

        } else {

            goalkeeperInformation.setVisible(
                    false
            );

            goalkeeperInformation.setManaged(
                    false
            );
        }
    }


    /*
     * =====================================================
     * BRACKET
     * =====================================================
     */

    private void refreshBracket() {

        if (tournament == null) {
            return;
        }

        qualifiedBox
                .getChildren()
                .clear();

        quarterFinalBox
                .getChildren()
                .clear();

        semiFinalBox
                .getChildren()
                .clear();

        finalBox
                .getChildren()
                .clear();

        refreshQualifiedTeams();

        refreshTwoLegRound(
                tournament
                        .getQuarterFinalFirstLegs(),
                tournament
                        .getQuarterFinalSecondLegs(),
                quarterFinalBox
        );

        refreshTwoLegRound(
                tournament
                        .getSemiFinalFirstLegs(),
                tournament
                        .getSemiFinalSecondLegs(),
                semiFinalBox
        );

        refreshFinal();

        if (tournament
                .getChampion()
                == null) {

            championName.setText(
                    "NOT DEFINED"
            );

        } else {

            championName.setText(
                    tournament
                            .getChampion()
                            .getName()
            );
        }
    }

    /*
     * No muestra líderes provisionales como clasificados.
     * Hasta terminar el grupo muestra que sigue en juego.
     */
    private void refreshQualifiedTeams() {

        for (Group group :
                tournament.getGroups()) {

            VBox card =
                    createBracketCard();

            Label title =
                    new Label(
                            group.getName()
                    );

            title.getStyleClass()
                    .add(
                            "bracket-caption"
                    );

            card.getChildren()
                    .add(
                            title
                    );

            if (!isGroupFinished(
                    group
            )) {

                Label pending =
                        new Label(
                                "Group stage in progress"
                        );

                pending.getStyleClass()
                        .add(
                                "muted-text"
                        );

                card.getChildren()
                        .add(
                                pending
                        );

            } else {

                List<Team> qualified =
                        group.getQualifiedTeams();

                for (int i = 0;
                     i < qualified.size()
                             && i < 2;
                     i++) {

                    Label team =
                            new Label(
                                    (i + 1)
                                            + ". "
                                            + qualified
                                            .get(i)
                                            .getName()
                            );

                    team.getStyleClass()
                            .add(
                                    "bracket-team"
                            );

                    card.getChildren()
                            .add(
                                    team
                            );
                }
            }

            qualifiedBox
                    .getChildren()
                    .add(
                            card
                    );
        }
    }

    private boolean isGroupFinished(
            Group group) {

        if (group.getMatches()
                .isEmpty()) {

            return false;
        }

        for (GroupMatch match :
                group.getMatches()) {

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

            Label pending =
                    new Label(
                            "Not generated yet"
                    );

            pending.getStyleClass()
                    .add(
                            "muted-text"
                    );

            container
                    .getChildren()
                    .add(
                            pending
                    );

            return;
        }

        for (int i = 0;
             i < firstLegs.size();
             i++) {

            Match first =
                    firstLegs.get(i);

            VBox card =
                    createBracketCard();

            Label teams =
                    new Label(
                            first
                                    .getHomeTeam()
                                    .getName()
                                    + "\nvs\n"
                                    + first
                                    .getAwayTeam()
                                    .getName()
                    );

            teams.getStyleClass()
                    .add(
                            "bracket-team"
                    );

            Label firstResult =
                    new Label(
                            "1st leg: "
                                    + scoreText(
                                    first
                            )
                    );

            firstResult
                    .getStyleClass()
                    .add(
                            "muted-text"
                    );

            card.getChildren()
                    .addAll(
                            teams,
                            firstResult
                    );

            if (i < secondLegs.size()) {

                Match second =
                        secondLegs.get(i);

                Label secondResult =
                        new Label(
                                "2nd leg: "
                                        + scoreText(
                                        second
                                )
                        );

                secondResult
                        .getStyleClass()
                        .add(
                                "muted-text"
                        );

                card.getChildren()
                        .add(
                                secondResult
                        );

                if (second.isPlayed()
                        && second.getWinner()
                        != null) {

                    Label winner =
                            new Label(
                                    "WINNER: "
                                            + second
                                            .getWinner()
                                            .getName()
                            );

                    winner.getStyleClass()
                            .add(
                                    "green-chip"
                            );

                    card.getChildren()
                            .add(
                                    winner
                            );
                }
            }

            container
                    .getChildren()
                    .add(
                            card
                    );
        }
    }

    private void refreshFinal() {

        Match finalMatch =
                tournament
                        .getFinalMatch();

        if (finalMatch == null) {

            Label pending =
                    new Label(
                            "Not generated yet"
                    );

            pending.getStyleClass()
                    .add(
                            "muted-text"
                    );

            finalBox
                    .getChildren()
                    .add(
                            pending
                    );

            return;
        }

        VBox card =
                createBracketCard();

        Label teams =
                new Label(
                        finalMatch
                                .getHomeTeam()
                                .getName()
                                + "\nvs\n"
                                + finalMatch
                                .getAwayTeam()
                                .getName()
                );

        teams.getStyleClass()
                .add(
                        "bracket-team"
                );

        Label result =
                new Label(
                        scoreText(
                                finalMatch
                        )
                );

        result.getStyleClass()
                .add(
                        "bracket-score"
                );

        card.getChildren()
                .addAll(
                        teams,
                        result
                );

        finalBox
                .getChildren()
                .add(
                        card
                );
    }

    private VBox createBracketCard() {

        VBox card =
                new VBox(
                        6
                );

        card.getStyleClass()
                .add(
                        "bracket-card"
                );

        return card;
    }

    private String scoreText(
            Match match) {

        if (!match.isPlayed()) {

            return "Pending";
        }

        String result =
                match.getHomeGoals()
                        + " - "
                        + match.getAwayGoals();

        if (match
                .getHomePenalties()
                != null) {

            result +=
                    " ("
                            + match.getHomePenalties()
                            + " - "
                            + match.getAwayPenalties()
                            + " pens)";
        }

        return result;
    }


    /*
     * =====================================================
     * REPORTS
     * =====================================================
     */

    private void configureReports() {

        reportList
                .getItems()
                .addAll(
                        "II - Top Scorers",
                        "III - Fair Play",
                        "IV - Most Minutes",
                        "V - Championship Bracket",
                        "VI - Teams",
                        "VII - Referees",
                        "VIII - Players"
                );

        positionFilter
                .getItems()
                .addAll(
                        Position.values()
                );

        positionFilter.setPromptText(
                "ALL POSITIONS"
        );

        reportList
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable,
                         oldValue,
                         newValue) -> {

                            if (newValue != null) {

                                generateReport();
                            }
                        }
                );

        positionFilter
                .valueProperty()
                .addListener(
                        (observable,
                         oldValue,
                         newValue) -> {

                            String selected =
                                    reportList
                                            .getSelectionModel()
                                            .getSelectedItem();

                            if (selected != null
                                    && selected.startsWith(
                                    "VIII"
                            )) {

                                generateReport();
                            }
                        }
                );
    }

    private void refreshReports() {

        if (reportList
                .getSelectionModel()
                .getSelectedItem()
                == null) {

            reportList
                    .getSelectionModel()
                    .selectFirst();

        } else {

            generateReport();
        }
    }

    @FXML
    private void handleRefreshReport() {

        generateReport();
    }

    @FXML
    private void handleAllPositions() {

        positionFilter.setValue(
                null
        );

        generateReport();
    }

    private void generateReport() {

        if (tournament == null) {
            return;
        }

        String selected =
                reportList
                        .getSelectionModel()
                        .getSelectedItem();

        if (selected == null) {
            return;
        }

        try {

            String result;

            if (selected.startsWith(
                    "II"
            )) {

                selectedReportTitle.setText(
                        "Top Scorers"
                );

                result =
                        reportService
                                .generateScorersRanking(
                                        tournament
                                                .getTeams()
                                );

            } else if (selected.startsWith(
                    "III"
            )) {

                selectedReportTitle.setText(
                        "Fair Play"
                );

                result =
                        reportService
                                .generateFairPlayRanking(
                                        tournament
                                                .getTeams(),
                                        tournament
                                                .getAllMatches()
                                );

            } else if (selected.startsWith(
                    "IV"
            )) {

                selectedReportTitle.setText(
                        "Most Minutes"
                );

                result =
                        reportService
                                .generateMinutesRanking(
                                        tournament
                                                .getTeams()
                                );

            } else if (selected.startsWith(
                    "V"
            )) {

                selectedReportTitle.setText(
                        "Championship Bracket"
                );

                result =
                        reportService
                                .generateChampionshipBracket(
                                        tournament
                                );

            } else if (selected.startsWith(
                    "VI"
            )) {

                selectedReportTitle.setText(
                        "Teams"
                );

                result =
                        reportService
                                .generateTeamsReport(
                                        tournament
                                );

            } else if (selected.startsWith(
                    "VII"
            )) {

                selectedReportTitle.setText(
                        "Referees"
                );

                result =
                        reportService
                                .generateRefereesRanking(
                                        tournament
                                                .getReferees(),
                                        tournament
                                                .getAllMatches()
                                );

            } else {

                selectedReportTitle.setText(
                        "Players"
                );

                Position position =
                        positionFilter
                                .getValue();

                if (position == null) {

                    result =
                            reportService
                                    .generatePlayersReport(
                                            tournament
                                                    .getTeams()
                                    );

                } else {

                    result =
                            reportService
                                    .generatePlayersReport(
                                            tournament
                                                    .getTeams(),
                                            position
                                    );
                }
            }

            reportArea.setText(
                    result
            );

        } catch (Exception exception) {

            reportArea.setText(
                    "Error generating report:\n\n"
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
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
}