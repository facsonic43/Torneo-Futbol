package gui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import main.loader.TournamentData;
import model.match.Goal;
import model.tournament.Tournament;
import reports.ChampionshipStatistics;
import reports.ChampionshipStatistics.PlayerStats;
import reports.ChampionshipStatistics.TeamStats;
import reports.ReportData;
import reports.ReportSection;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntFunction;

// Genera las tablas estadísticas del campeonato a partir de una copia del torneo.
// Calcula el contenido en segundo plano y cancela las tareas que dejan de ser necesarias.

public final class StatisticsReportView extends VBox {
    private Task<Content> currentTask;

    private record Content(ChampionshipStatistics statistics) { }
    private record Ranked<T>(int rank, T value) { }

    public StatisticsReportView() {
        setSpacing(14);
        setMinSize(0, 0);
        getStyleClass().add("statistics-report");
        sceneProperty().addListener((observable, previous, scene) -> {
            if (scene == null) cancelPendingReport();
        });
    }

    public Task<Content> getCurrentTask() {
        return currentTask;
    }

    public void showReport(Tournament tournament, ReportSection section) throws IOException {
        Objects.requireNonNull(tournament, "Tournament is required");
        Objects.requireNonNull(section, "Report section is required");
        cancelPendingReport();
        byte[] snapshot = snapshot(tournament);
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(36, 36);
        VBox loading = new VBox(12, progress, label("Generating report...", "muted-text"));
        loading.setAlignment(Pos.CENTER);
        VBox.setVgrow(loading, Priority.ALWAYS);
        getChildren().setAll(loading);
        Task<Content> task = new Task<>() {
            @Override
            protected Content call() throws Exception {
                ReportData data = readSnapshot(snapshot);
                return new Content(new ChampionshipStatistics(data));
            }
        };
        currentTask = task;
        task.setOnSucceeded(event -> {
            if (currentTask != task) return;
            currentTask = null;
            getChildren().clear();
            switch (section) {
                case TOP_SCORERS -> showScorers(task.getValue().statistics());
                case FAIR_PLAY -> showFairPlay(task.getValue().statistics());
                case MOST_MINUTES -> showMinutes(task.getValue().statistics());
            }
        });
        task.setOnFailed(event -> {
            if (currentTask != task) return;
            currentTask = null;
            String detail = task.getException().getMessage();
            getChildren().setAll(note("Could not display this report." + (detail == null ? "" : "\n" + detail)));
        });
        Thread worker = new Thread(task, "statistics-report");
        worker.setDaemon(true);
        worker.start();
    }

    private void showScorers(ChampionshipStatistics statistics) {
        getChildren().add(note("Goals scored during matches, including match penalties. Own goals and penalty shootouts are excluded."));
        TableView<Ranked<PlayerStats>> table = rankedTable(statistics.getTopScorers(), PlayerStats::goals,
                "No goals have been scored in this championship.");
        table.getColumns().add(column("PLAYER", 220, row -> row.value().player().getName(), "player-name-cell"));
        table.getColumns().add(column("TEAM", 180, row -> row.value().team().getName(), null));
        table.getColumns().add(column("GOALS", 80, row -> row.value().goals(), "report-highlight-cell"));
        table.getColumns().add(column("OF WHICH PENALTIES", 130, row -> statistics.hasCompleteGoalInformation()
                ? row.value().penaltyGoals() : "Not available", "table-stat-cell"));
        table.getColumns().add(column("G/MATCH", 90, row -> {
            int matches = row.value().matches();
            double avg = matches > 0 ? (double) row.value().goals() / matches : 0.0;
            return String.format("%.2f", avg);
        }, "table-stat-cell"));
        table.getColumns().add(column("AST", 60, row -> row.value().assists(), "table-stat-cell"));
        addTable(table);
    }

    private void showFairPlay(ChampionshipStatistics statistics) {
        getChildren().add(note("Penalty points: 1 per yellow card + 3 per red card. Fewer points ranks higher. "
                + "A sending-off for two yellows counts as two yellow cards and one red card. Equal scores share a rank."));
        TableView<Ranked<TeamStats>> table = rankedTable(statistics.getFairPlayRanking(), TeamStats::fairPlayScore,
                "No teams registered.");
        table.getColumns().add(column("TEAM", 280, row -> row.value().team().getName(), "player-name-cell"));
        table.getColumns().add(column("PLAYED", 95, row -> row.value().played(), "table-stat-cell"));
        table.getColumns().add(chipColumn("YELLOW CARDS", row -> row.value().yellowCards(), "card-yellow-chip"));
        table.getColumns().add(chipColumn("RED CARDS", row -> row.value().redCards(), "card-red-chip"));
        table.getColumns().add(column("PENALTY POINTS", 140, row -> row.value().fairPlayScore(), "report-highlight-cell"));
        addTable(table);
    }

    private void showMinutes(ChampionshipStatistics statistics) {
        getChildren().add(note("Participation includes starters and substitutes. Minutes end at substitution, dismissal or the final whistle."));
        boolean complete = statistics.hasCompleteStartingLineups();
        TableView<Ranked<PlayerStats>> table = rankedTable(complete ? statistics.getMinutesRanking() : List.of(),
                PlayerStats::minutes, complete
                ? "No players registered." : "Not available: starting lineups were not supplied for one or more completed matches.");
        table.getColumns().add(column("PLAYER", 280, row -> row.value().player().getName(), "player-name-cell"));
        table.getColumns().add(column("TEAM", 260, row -> row.value().team().getName(), null));
        table.getColumns().add(column("MATCHES", 110, row -> row.value().matches(), "table-stat-cell"));
        table.getColumns().add(column("MINUTES", 120, row -> row.value().minutes(), "report-highlight-cell"));
        addTable(table);
    }

    private static <T> TableView<Ranked<T>> rankedTable(List<T> values, ToIntFunction<T> metric, String empty) {
        TableView<Ranked<T>> table = table(empty);
        Integer previous = null;
        int rank = 0;
        for (int index = 0; index < values.size(); index++) {
            T value = values.get(index);
            int current = metric.applyAsInt(value);
            if (previous == null || current != previous) rank = index + 1;
            previous = current;
            table.getItems().add(new Ranked<>(rank, value));
        }
        TableColumn<Ranked<T>, Integer> ranks = column("#", 60, Ranked::rank, "table-stat-cell");
        ranks.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(Integer value, boolean emptyCell) {
                super.updateItem(value, emptyCell);
                setText(null);
                setGraphic(emptyCell || value == null ? null : badge(value.toString(), "rank-badge", value == 1
                        ? "rank-gold" : value == 2 ? "rank-silver" : value == 3 ? "rank-bronze" : "rank-normal"));
                setAlignment(Pos.CENTER);
            }
        });
        table.getColumns().add(ranks);
        return table;
    }

    private static <T> TableView<T> table(String empty) {
        TableView<T> table = new TableView<>();
        table.getStyleClass().addAll("dark-table", "statistics-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(42);
        table.setMinSize(0, 0);
        table.setPlaceholder(label(empty, "muted-text"));
        return table;
    }

    private static <T, V> TableColumn<T, V> column(String title, double width, Function<T, V> value, String style) {
        TableColumn<T, V> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setSortable(false);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        if (style != null) column.setCellFactory(ignored -> new TableCell<>() {
            { getStyleClass().add(style); }
            @Override protected void updateItem(V item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setGraphic(null);
            }
        });
        return column;
    }

    private static <T> TableColumn<T, Integer> chipColumn(String title, Function<T, Integer> value, String style) {
        TableColumn<T, Integer> column = column(title, 95, value, null);
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : badge(item.toString(), style));
                setAlignment(Pos.CENTER);
            }
        });
        return column;
    }

    private void addTable(TableView<?> table) {
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().add(table);
    }

    private static Label note(String text) { return label(text, "report-note"); }

    private static Label badge(String text, String... styles) {
        Label badge = label(text, styles);
        badge.setMaxWidth(Region.USE_PREF_SIZE);
        badge.setAlignment(Pos.CENTER);
        return badge;
    }

    private static Label label(String text, String... styles) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().addAll(styles);
        return label;
    }

    private void cancelPendingReport() {
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
    }

    private static byte[] snapshot(Tournament tournament) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(tournament); }
        return bytes.toByteArray();
    }

    private static ReportData readSnapshot(byte[] bytes) throws IOException, ClassNotFoundException {
        Tournament snapshot;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            snapshot = (Tournament) input.readObject();
        }
        ReportData data = new ReportData(new TournamentData(snapshot.getTeams(), snapshot.getReferees()));
        snapshot.getGroups().forEach(data::addGroup);
        snapshot.getAllMatches().forEach(match -> {
            data.addMatch(match);
            for (Goal goal : match.getGoals()) {
                data.getMatchDetails(match).setGoalDetails(goal, goal.isPenalty(), goal.isOwnGoal(),
                        goal.getGoalkeeperConceded());
            }
        });
        return data;
    }
}
