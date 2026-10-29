package gui;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import main.loader.TournamentData;
import model.match.Goal;
import model.tournament.Tournament;
import reports.BracketReport;
import reports.BracketReport.BracketNode;
import reports.ChampionshipStatistics;
import reports.ChampionshipStatistics.PlayerStats;
import reports.ChampionshipStatistics.TeamStats;
import reports.ReportData;
import reports.ReportSection;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Native report controls using the same tournament snapshot and calculations as the PDF. */
public final class StatisticsReportView extends VBox {
    private Task<Content> currentTask;

    private record Content(ChampionshipStatistics statistics, BracketReport bracket) { }
    private record Ranked<T>(int rank, T value) { }

    public StatisticsReportView() {
        setSpacing(14);
        setMinSize(0, 0);
        getStyleClass().add("statistics-report");
        sceneProperty().addListener((observable, previous, scene) -> {
            if (scene == null) cancelPendingReport();
        });
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
                return section == ReportSection.BRACKET
                        ? new Content(null, new BracketReport(data))
                        : new Content(new ChampionshipStatistics(data), null);
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
                case BRACKET -> showBracket(task.getValue().bracket());
            }
        });
        task.setOnFailed(event -> {
            if (currentTask != task) return;
            currentTask = null;
            String detail = task.getException().getMessage();
            getChildren().setAll(note("Could not display this report."
                    + (detail == null ? "" : "\n" + detail)));
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
        table.getColumns().add(column("OF WHICH PENALTIES", 130,
                row -> statistics.hasCompleteGoalInformation() ? row.value().penaltyGoals() : "Not available", "table-stat-cell"));
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
                PlayerStats::minutes, complete ? "No players registered."
                        : "Not available: starting lineups were not supplied for one or more completed matches.");
        table.getColumns().add(column("PLAYER", 280, row -> row.value().player().getName(), "player-name-cell"));
        table.getColumns().add(column("TEAM", 260, row -> row.value().team().getName(), null));
        table.getColumns().add(column("MATCHES", 110, row -> row.value().matches(), "table-stat-cell"));
        table.getColumns().add(column("MINUTES", 120, row -> row.value().minutes(), "report-highlight-cell"));
        addTable(table);
    }

    private void showBracket(BracketReport report) {
        VBox content = new VBox(16);
        content.getChildren().add(note("Qualification is confirmed after all six distinct group fixtures have been played. "
                + "An unresolved tie remains pending. Zone numbers follow the registered group order."));
        TableView<BracketReport.GroupQualification> qualifiers = table("No groups registered.");
        qualifiers.getItems().setAll(report.getQualifiers());
        qualifiers.getColumns().add(column("ZONE / GROUP", 155,
                row -> (report.getQualifiers().indexOf(row) + 1) + " / " + row.groupName(), "player-name-cell"));
        qualifiers.getColumns().add(column("FIRST PLACE", 200, row -> BracketReport.name(row.first()), null));
        qualifiers.getColumns().add(column("SECOND PLACE", 200, row -> BracketReport.name(row.second()), null));
        qualifiers.getColumns().add(column("PLAYED", 80, row -> row.playedMatches() + "/" + row.requiredMatches(), "table-stat-cell"));
        qualifiers.getColumns().add(column("STATUS", 170, BracketReport.GroupQualification::status, null));
        qualifiers.setMinHeight(215);
        qualifiers.setPrefHeight(215);
        qualifiers.setMaxHeight(215);
        content.getChildren().add(qualifiers);
        content.getChildren().add(note("Quarter-finals and semi-finals: two legs. Final: one match. "
                + "Arrows show the path to the championship. Diagram scores follow the team order shown in each box."));
        content.getChildren().add(bracketDiagram(report));
        VBox champion = new VBox(6, label("TOURNAMENT CHAMPION", "section-caption"),
                label(BracketReport.name(report.getChampion()), "champion-text"));
        champion.getStyleClass().add("info-card");
        content.getChildren().addAll(champion, label("MATCH DETAILS", "section-caption"),
                note("Two-leg ties are decided by points across both matches, then goal difference "
                        + "with away goals counting double, then penalties. Both legs must be completed."));
        for (BracketNode node : report.getNodes()) content.getChildren().add(matchDetails(node));
        if (!report.getUnassignedMatches().isEmpty()) {
            content.getChildren().add(label("Registered knockout matches awaiting confirmed bracket placement", "section-caption"));
            report.getUnassignedMatches().forEach(match -> content.getChildren().add(note(match.description())));
        }
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setMinSize(0, 0);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
    }

    private Node bracketDiagram(BracketReport report) {
        GridPane grid = new GridPane();
        grid.setHgap(34);
        grid.setVgap(14);
        grid.setMinWidth(740);
        grid.getRowConstraints().add(new RowConstraints(24));
        for (int row = 0; row < 8; row++) grid.getRowConstraints().add(new RowConstraints(95, 95, Double.MAX_VALUE));
        for (int column = 0; column < 3; column++) {
            ColumnConstraints constraint = new ColumnConstraints();
            constraint.setPercentWidth(100.0 / 3);
            grid.getColumnConstraints().add(constraint);
            Label heading = label(new String[]{"QUARTER-FINALS", "SEMI-FINALS", "FINAL"}[column], "section-caption");
            grid.add(heading, column, 0);
        }
        List<VBox> cards = new ArrayList<>();
        for (int index = 0; index < report.getNodes().size(); index++) {
            BracketNode node = report.getNodes().get(index);
            VBox card = new VBox(7, label(node.round() + " " + node.id(), "card-caption"),
                    label(node.homeSource() + ": " + BracketReport.name(node.homeTeam()), "card-value"),
                    label(node.awaySource() + ": " + BracketReport.name(node.awayTeam()), "card-value"));
            if (index < 6) {
                card.getChildren().add(label("Leg 1: " + score(node.firstLeg(), node) + "  |  Leg 2: " + score(node.secondLeg(), node), "small-text"));
            } else {
                card.getChildren().add(label("Score: " + score(node.finalMatch(), node), "small-text"));
            }
            card.getChildren().addAll(label(node.status(), "muted-text"),
                    label("Winner: " + BracketReport.name(node.winner()), "report-winner"));
            card.getStyleClass().add("bracket-card");
            card.setMinHeight(Region.USE_PREF_SIZE);
            card.setMaxHeight(Region.USE_PREF_SIZE);
            if (index < 4) grid.add(card, 0, 1 + index * 2, 1, 2);
            else if (index < 6) grid.add(card, 1, 1 + (index - 4) * 4, 1, 4);
            else grid.add(card, 2, 1, 1, 8);
            cards.add(card);
        }
        Pane arrows = new Pane();
        arrows.setMouseTransparent(true);
        int[][] connections = {{0, 4}, {1, 4}, {2, 5}, {3, 5}, {4, 6}, {5, 6}};
        for (int[] connection : connections) {
            VBox source = cards.get(connection[0]), target = cards.get(connection[1]);
            Path arrow = new Path();
            arrow.getStyleClass().add("report-bracket-arrow");
            InvalidationListener update = observable -> {
                double x1 = source.getLayoutX() + source.getWidth(), y1 = source.getLayoutY() + source.getHeight() / 2;
                double x2 = target.getLayoutX(), y2 = target.getLayoutY() + target.getHeight() / 2;
                double midpoint = (x1 + x2) / 2;
                arrow.getElements().setAll(new MoveTo(x1, y1), new LineTo(midpoint, y1), new LineTo(midpoint, y2),
                        new LineTo(x2, y2), new MoveTo(x2 - 5, y2 - 4), new LineTo(x2, y2), new LineTo(x2 - 5, y2 + 4));
            };
            source.layoutBoundsProperty().addListener(update);
            source.layoutXProperty().addListener(update);
            source.layoutYProperty().addListener(update);
            target.layoutBoundsProperty().addListener(update);
            target.layoutXProperty().addListener(update);
            target.layoutYProperty().addListener(update);
            arrows.getChildren().add(arrow);
        }
        return new StackPane(arrows, grid);
    }

    private VBox matchDetails(BracketNode node) {
        VBox card = new VBox(10);
        card.getStyleClass().add("info-card");
        card.getChildren().add(label(node.round() + " " + node.id() + " | " + node.homeSource() + " vs " + node.awaySource(), "card-caption"));
        GridPane details = new GridPane();
        details.setHgap(16);
        details.setVgap(8);
        ColumnConstraints caption = new ColumnConstraints();
        caption.setMinWidth(100);
        ColumnConstraints value = new ColumnConstraints();
        value.setHgrow(Priority.ALWAYS);
        details.getColumnConstraints().addAll(caption, value);
        detail(details, "Teams", BracketReport.name(node.homeTeam()) + " vs " + BracketReport.name(node.awayTeam()));
        if (node.finalMatch() != null || node.round().equals("Final")) detail(details, "Final", BracketReport.describe(node.finalMatch()));
        else {
            detail(details, "First leg", BracketReport.describe(node.firstLeg()));
            detail(details, "Second leg", BracketReport.describe(node.secondLeg()));
        }
        detail(details, "Status", node.status());
        detail(details, "Winner", BracketReport.name(node.winner()));
        if (!node.resolution().isBlank()) detail(details, "Decision", node.resolution());
        card.getChildren().add(details);
        return card;
    }

    private static void detail(GridPane grid, String title, String value) {
        int row = grid.getRowCount();
        grid.addRow(row, label(title, "muted-text"), label(value, "report-detail"));
    }

    private static String score(BracketReport.MatchResult match, BracketNode node) {
        if (match == null || !match.played()) return "pending";
        boolean sameOrder = match.homeTeam() == node.homeTeam();
        String score = sameOrder ? match.homeGoals() + " - " + match.awayGoals()
                : match.awayGoals() + " - " + match.homeGoals();
        if (match.homePenalties() != null && match.awayPenalties() != null) {
            score += sameOrder ? " (pens " + match.homePenalties() + " - " + match.awayPenalties() + ")"
                    : " (pens " + match.awayPenalties() + " - " + match.homePenalties() + ")";
        }
        return score;
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
                setGraphic(emptyCell || value == null ? null : badge(value.toString(),
                        "rank-badge", value == 1 ? "rank-gold" : value == 2 ? "rank-silver" : value == 3 ? "rank-bronze" : "rank-normal"));
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
                data.getMatchDetails(match).setGoalDetails(goal,
                        goal.isPenalty(), goal.isOwnGoal(), goal.getGoalkeeperConceded());
            }
        });
        return data;
    }
}