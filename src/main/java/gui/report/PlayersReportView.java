package gui.report;

import gui.PlayerDirectoryEntry;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.participant.Player;
import model.participant.Position;
import model.participant.Team;

import java.util.Map;

import static gui.TableCells.createIntegerCenterCell;
import static gui.TableCells.createCenterStringCell;
import static gui.TableCells.createBoldCell;

public class PlayersReportView extends ReportView {

    public PlayersReportView(StackPane reportContentHost, Label reportMetricChip, Label selectedReportSubtitle,
                             Label selectedReportTitle, Node reportsPane) {
        super(reportContentHost, reportMetricChip, selectedReportSubtitle, selectedReportTitle, reportsPane);
    }

    @Override
    protected void render() {
        selectedReportTitle.setText("Tournament Players Directory");
        selectedReportSubtitle.setText("Master database of registered athletes with disciplinary records, ratings and performance metrics");

        VBox layout = new VBox(12);
        layout.setFillWidth(true);
        VBox.setVgrow(layout, Priority.ALWAYS);

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("info-card");

        ComboBox<String> posFilter = new ComboBox<>();
        posFilter.getItems().add("ALL POSITIONS");
        for (Position p : Position.values()) {
            posFilter.getItems().add(p.name());
        }
        posFilter.setValue("ALL POSITIONS");
        posFilter.setPrefWidth(160);

        ComboBox<String> teamFilter = new ComboBox<>();
        teamFilter.getItems().add("ALL TEAMS");
        for (Team t : tournament.getTeams()) {
            teamFilter.getItems().add(t.getName());
        }
        teamFilter.setValue("ALL TEAMS");
        teamFilter.setPrefWidth(200);

        Button btnReset = new Button("RESET FILTERS");
        btnReset.getStyleClass().add("secondary-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        filterBar.getChildren().addAll(
                new Label("POSITION:"), posFilter,
                new Label("TEAM:"), teamFilter,
                btnReset,
                spacer
        );

        TableView<PlayerDirectoryEntry> table = new TableView<>();
        table.getStyleClass().add("dark-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<PlayerDirectoryEntry, String> playerCol = new TableColumn<>("NAME");
        playerCol.setPrefWidth(160);
        playerCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPlayerName()));
        playerCol.setCellFactory(col -> createBoldCell());

        TableColumn<PlayerDirectoryEntry, String> posCol = new TableColumn<>("POSITION");
        posCol.setPrefWidth(70);
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
                    if (posUpper.contains("GOALKEEPER") || posUpper.contains("GK")) badge.getStyleClass().add("pos-gk");
                    else if (posUpper.contains("DEFENDER") || posUpper.contains("DEF")) badge.getStyleClass().add("pos-def");
                    else if (posUpper.contains("MIDFIELDER") || posUpper.contains("MID")) badge.getStyleClass().add("pos-mid");
                    else badge.getStyleClass().add("pos-fwd");
                    setGraphic(badge); setText(null); setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<PlayerDirectoryEntry, String> teamCol = new TableColumn<>("TEAM");
        teamCol.setPrefWidth(140);
        teamCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTeamName()));

        // --- COLUMNA OVERALL CON DISPARADOR DE DIÁLOGO MODAL ---
        TableColumn<PlayerDirectoryEntry, Integer> ovrCol = new TableColumn<>("OVERALL");
        ovrCol.setPrefWidth(55);
        ovrCol.setCellValueFactory(cell -> new SimpleObjectProperty<>((int) Math.round(cell.getValue().getPlayer().getOverall())));
        ovrCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    setGraphic(null);
                    setOnMouseClicked(null);
                } else {
                    Label badge = new Label(String.valueOf(val));
                    badge.getStyleClass().add("ovr-badge");
                    badge.setStyle("-fx-cursor: hand;"); // Cambia el cursor a manito

                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);

                    // Al hacer clic sobre la insignia OVR, abre la ventana emergente
                    setOnMouseClicked(event -> {
                        if (getItem() != null) {
                            PlayerDirectoryEntry entry = getTableView().getItems().get(getIndex());
                            showPlayerAttributesDialog(entry.getPlayer());
                        }
                    });
                }
            }
        });

        TableColumn<PlayerDirectoryEntry, Integer> matchesCol = new TableColumn<>("PLAYED");
        matchesCol.setPrefWidth(50);
        matchesCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getMatchesPlayed()));
        matchesCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<PlayerDirectoryEntry, Integer> minCol = new TableColumn<>("MINUTES");
        minCol.setPrefWidth(55);
        minCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getMinutesPlayed()));
        minCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<PlayerDirectoryEntry, Integer> goalsCol = new TableColumn<>("SCORED");
        goalsCol.setPrefWidth(50);
        goalsCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getGoals()));
        goalsCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<PlayerDirectoryEntry, Integer> yellowCol = new TableColumn<>("YELLOW CARDS");
        yellowCol.setPrefWidth(50);
        yellowCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getPlayer().getYellowCards()));
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

        TableColumn<PlayerDirectoryEntry, Integer> redCol = new TableColumn<>("RED CARDS");
        redCol.setPrefWidth(50);
        redCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getPlayer().getRedCards()));
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

        TableColumn<PlayerDirectoryEntry, String> availCol = new TableColumn<>("AVAILABLE");
        availCol.setPrefWidth(90);
        availCol.setCellValueFactory(cell -> {
            Player p = cell.getValue().getPlayer();
            if (p.isSuspended()) return new SimpleStringProperty("SUSPENDED");
            if (p.isInjured()) return new SimpleStringProperty("INJURED");
            return new SimpleStringProperty("YES");
        });
        availCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null); setGraphic(null);
                } else {
                    Label chip = new Label(status);
                    if ("YES".equalsIgnoreCase(status)) {
                        chip.getStyleClass().add("green-chip");
                    } else {
                        chip.getStyleClass().add("blocked-chip");
                    }
                    setGraphic(chip); setText(null); setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<PlayerDirectoryEntry, Integer> concededCol = new TableColumn<>("CONCEDED");
        concededCol.setPrefWidth(50);
        concededCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getGoalsConceded()));
        concededCol.setCellFactory(col -> createIntegerCenterCell());

        TableColumn<PlayerDirectoryEntry, String> gkAvgCol = new TableColumn<>("CONC./MATCH");
        gkAvgCol.setPrefWidth(85);
        gkAvgCol.setCellValueFactory(cell -> {
            Player p = cell.getValue().getPlayer();
            if (p.isGoalkeeper()) {
                Player gk = p;
                double avg = gk.getMatchesPlayed() > 0 ? (double) gk.getGoalsConceded() / gk.getMatchesPlayed() : 0.0;
                return new SimpleStringProperty(String.format("%.2f", avg));
            }
            return new SimpleStringProperty("-");
        });
        gkAvgCol.setCellFactory(col -> createCenterStringCell());

        table.getColumns().addAll(playerCol, posCol, teamCol, ovrCol, matchesCol, minCol, goalsCol, yellowCol, redCol, availCol, concededCol, gkAvgCol);

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
            String pSel = posFilter.getValue();
            String tSel = teamFilter.getValue();
            filteredList.setPredicate(entry -> {
                boolean matchPos = "ALL POSITIONS".equalsIgnoreCase(pSel) || pSel == null || entry.getPosition().name().equalsIgnoreCase(pSel);
                boolean matchTeam = "ALL TEAMS".equalsIgnoreCase(tSel) || tSel == null || (entry.getTeam() != null && entry.getTeam().getName().equalsIgnoreCase(tSel));
                return matchPos && matchTeam;
            });
            reportMetricChip.setText(filteredList.size() + " PLAYERS");
        };

        posFilter.valueProperty().addListener((obs, o, n) -> updateFilter.run());
        teamFilter.valueProperty().addListener((obs, o, n) -> updateFilter.run());

        btnReset.setOnAction(e -> {
            posFilter.setValue("ALL POSITIONS");
            teamFilter.setValue("ALL TEAMS");
            updateFilter.run();
        });

        updateFilter.run();

        layout.getChildren().addAll(filterBar, table);
        reportContentHost.getChildren().add(layout);
    }

    private void showPlayerAttributesDialog(Player player) {
        Dialog<Void> dialog = new Dialog<>();
        DialogPane dialogPane = dialog.getDialogPane();

        if (reportsPane != null && reportsPane.getScene() != null) {
            dialogPane.getStylesheets().addAll(reportsPane.getScene().getStylesheets());

            Node rootNode = reportsPane.getScene().getRoot();
            dialogPane.getStyleClass().addAll(rootNode.getStyleClass());
        }

        dialogPane.getStyleClass().add("panel");

        VBox contentContainer = new VBox(12);
        contentContainer.setPadding(new Insets(10));

        // Encabezado usando las clases de tu CSS
        VBox headerBox = new VBox(2);
        Label nameLabel = new Label(player.getName().toUpperCase());
        nameLabel.getStyleClass().add("section-title-large");

        Label posLabel = new Label(player.getPosition().toString());
        posLabel.getStyleClass().add("muted-text");

        headerBox.getChildren().addAll(nameLabel, posLabel);

        // Grilla con las estadísticas
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);

        Map<String, Integer> attributes = player.getTechnicalAttributes();

        int row = 0;
        for (Map.Entry<String, Integer> entry : attributes.entrySet()) {
            Label nameLbl = new Label(entry.getKey().toUpperCase() + ":");
            nameLbl.getStyleClass().add("small-label"); // Usa -text-muted según el tema

            Label valLbl = new Label(String.valueOf(entry.getValue()));
            valLbl.getStyleClass().add("ovr-badge"); // Badge verde lima

            grid.add(nameLbl, 0, row);
            grid.add(valLbl, 1, row);
            row++;
        }

        contentContainer.getChildren().addAll(headerBox, new Separator(), grid);

        dialogPane.setContent(contentContainer);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }
}