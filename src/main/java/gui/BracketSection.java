package gui;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.match.GroupMatch;
import model.match.Match;
import model.participant.Team;
import model.tournament.Group;
import model.tournament.Tournament;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import gui.BracketView;
import java.util.function.Supplier;
import model.tournament.TournamentStage;
import static gui.MatchFormat.scoreText;
import static gui.MatchFormat.matchDate;
import static gui.MatchFormat.matchStadium;
import static gui.MatchFormat.matchReferee;

/**
 * Knockout bracket: builds the quarter-final, semi-final and final cards, the tie-break texts
 * and the match detail dialog, and loads them into the {@link BracketView}.
 */
public class BracketSection {

    private final BracketView bracketView;
    private final Node ownerPane;
    private final Supplier<TournamentStage> stage;
    private Tournament tournament;

    public BracketSection(BracketView bracketView, Node ownerPane, Supplier<TournamentStage> stage) {
        this.bracketView = bracketView;
        this.ownerPane = ownerPane;
        this.stage = stage;
    }

    public BracketView getView() {
        return bracketView;
    }

    /** Loads the bracket of the tournament into the view and returns the champion (null if undefined). */
    public Team load(Tournament tournament) {
        this.tournament = tournament;

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

        return champion;
    }

    private static final int[][] QUARTER_SEEDS = {
            {0, 0, 3, 1},
            {1, 0, 2, 1},
            {2, 0, 0, 1},
            {3, 0, 1, 1}
    };

    // Numeración de las series: cuartos I-IV, semifinales V-VI.
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI"};

    /* Ronda en juego según la etapa del torneo (-1 si no hay ronda eliminatoria activa). */
    private int activeRound() {
        switch (stage.get()) {
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
    private List<Region> buildRound(
            List<? extends Match> firstLegs,
            List<? extends Match> secondLegs,
            int slots,
            int romanOffset,
            String roundTitle,
            Team champion,
            IntFunction<Region> placeholder) {

        List<Region> cards = new ArrayList<>();

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

    /*
     * Cruce de ida y vuelta. Estados:
     *  - READY:  los equipos ya están definidos pero la serie no terminó.
     *  - PLAYED: la serie terminó (se resalta el ganador y el criterio).
     */
    private Region twoLegCard(String title, Match first, Match second, Team champion) {
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

    private Region buildFinalCard(Team champion) {
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

    private Region lockedCard(String top, String bottom) {
        Label tag = new Label("LOCKED");
        tag.getStyleClass().add("bracket-lock-tag");

        VBox card = new VBox(4, teamRow(top, false), teamRow(bottom, false), tag);
        card.getStyleClass().addAll("bracket-card", "bracket-card-locked");
        return card;
    }

    private Region quarterPlaceholder(int index) {
        int[] seed = QUARTER_SEEDS[index];
        return lockedCard(seedLabel(seed[0], seed[1]), seedLabel(seed[2], seed[3]));
    }

    private Region semiPlaceholder(int index) {
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

    private void showMatchDetails(String title, Match first, Match second, String outcome) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setMinWidth(440);

        // El diálogo es otra ventana: hay que pasarle los estilos y el tema actual.
        if (ownerPane.getScene() != null) {
            dialog.initOwner(ownerPane.getScene().getWindow());
            pane.getStylesheets().addAll(ownerPane.getScene().getStylesheets());

            if (ownerPane.getScene().getRoot().getStyleClass().contains("light-theme")) {
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
}
