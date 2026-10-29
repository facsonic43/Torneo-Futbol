package gui.report;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.match.Event;
import model.match.Formation;
import model.match.Match;
import model.participant.Goalkeeper;
import model.participant.Player;
import model.participant.Position;
import model.participant.Referee;
import model.participant.Team;
import model.tournament.Group;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import static gui.MatchFormat.scoreText;

public class MatchesReportView extends ReportView {

    public MatchesReportView(StackPane reportContentHost, Label reportMetricChip, Label selectedReportSubtitle,
            Label selectedReportTitle, Node reportsPane) {
        super(reportContentHost, reportMetricChip, selectedReportSubtitle, selectedReportTitle, reportsPane);
    }

    // --- REPORTE IV: MATCHES SUMMARY & INCIDENCES ---
    @Override
    protected void render() {
        selectedReportTitle.setText("Matches Log & Incidences");
        selectedReportSubtitle.setText("Comprehensive log of played matches, starting lineups, formations and match events");

        // 1. Obtener partidos jugados y ordenarlos por fecha
        List<Match> playedMatches = new ArrayList<>();
        if (tournament.getAllMatches() != null) {
            for (Match m : tournament.getAllMatches()) {
                if (m.isPlayed()) {
                    playedMatches.add(m);
                }
            }
        }
        playedMatches.sort(Comparator.comparing(Match::getMatchDate, Comparator.nullsLast(Comparator.naturalOrder())));

        reportMetricChip.setText(playedMatches.size() + " MATCHES PLAYED");

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
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        // --- BARRA DE FILTROS ---
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("info-card");

        // Filtro de Equipos con opción ALL TEAMS
        ComboBox<String> teamFilter = new ComboBox<>();
        teamFilter.getItems().add("ALL TEAMS");
        for (Team t : tournament.getTeams()) {
            teamFilter.getItems().add(t.getName());
        }
        teamFilter.setValue("ALL TEAMS");
        teamFilter.setPrefWidth(200);

        // Filtro de Etapas con opción ALL STAGES
        ComboBox<String> stageFilter = new ComboBox<>();
        stageFilter.getItems().addAll("ALL STAGES", "GROUP STAGE", "QUARTER-FINALS", "SEMI-FINALS", "FINAL");
        stageFilter.setValue("ALL STAGES");
        stageFilter.setPrefWidth(180);

        Button btnReset = new Button("RESET FILTERS");
        btnReset.getStyleClass().add("secondary-button");

        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);

        filterBar.getChildren().addAll(
                new Label("TEAM:"), teamFilter,
                new Label("STAGE:"), stageFilter,
                btnReset,
                filterSpacer
        );

        // --- CONTENIDO (LISTA + DETALLES) ---
        HBox contentLayout = new HBox(16);
        contentLayout.setFillHeight(true);
        VBox.setVgrow(contentLayout, Priority.ALWAYS);

        // Columna izquierda: Lista de partidos
        VBox leftSide = new VBox(10);
        leftSide.setPrefWidth(300);

        ListView<Match> playedList = new ListView<>();
        playedList.getStyleClass().add("dark-list");
        VBox.setVgrow(playedList, Priority.ALWAYS);

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

        // Columna derecha: Detalle del partido seleccionado
        VBox rightSide = new VBox(12);
        rightSide.getStyleClass().add("info-card");
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        playedList.getSelectionModel().selectedItemProperty().addListener((obs, oldM, selectedMatch) -> {
            if (selectedMatch != null) {
                renderMatchDetails(rightSide, selectedMatch);
            }
        });

        // Lógica del filtro reactivo contemplando las opciones "ALL"
        Runnable updateFilters = () -> {
            String tSel = teamFilter.getValue();
            String sSel = stageFilter.getValue();

            filteredList.setPredicate(match -> {
                boolean matchTeam = "ALL TEAMS".equalsIgnoreCase(tSel) || tSel == null
                        || match.getHomeTeam().getName().equalsIgnoreCase(tSel)
                        || match.getAwayTeam().getName().equalsIgnoreCase(tSel);

                boolean matchStage = "ALL STAGES".equalsIgnoreCase(sSel) || sSel == null;

                if (!matchStage) {
                    String stageName = getMatchStageName(match).toUpperCase();
                    switch (sSel.toUpperCase()) {
                        case "FINAL":
                            matchStage = stageName.equals("FINAL");
                            break;
                        case "SEMI-FINALS":
                            matchStage = stageName.equals("SEMI-FINALS");
                            break;
                        case "QUARTER-FINALS":
                            matchStage = stageName.equals("QUARTER-FINALS");
                            break;
                        case "GROUP STAGE":
                            matchStage = stageName.equals("GROUP STAGE");
                            break;
                        default:
                            matchStage = stageName.contains(sSel.toUpperCase());
                            break;
                    }
                }

                return matchTeam && matchStage;
            });

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
            teamFilter.setValue("ALL TEAMS");
            stageFilter.setValue("ALL STAGES");
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

        return switch (tournament.getStageOf(match)) {
            case FINAL -> "Final";
            case SEMI_FINALS -> "Semi-Finals";
            case QUARTER_FINALS -> "Quarter-Finals";
            // Knockout matches that do not belong to any round of this tournament.
            default -> match.isKnockout() ? "Knockout Stage" : "Group Stage";
        };
    }
}
