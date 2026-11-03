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
import model.match.ResolutionCriterion;
import model.participant.Team;
import model.tournament.Group;
import model.tournament.Tournament;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import model.tournament.TournamentStage;

import static gui.MatchFormat.scoreText;
import static gui.MatchFormat.matchDate;
import static gui.MatchFormat.matchStadium;
import static gui.MatchFormat.matchReferee;

// Construye las tarjetas de cuartos, semifinales y final y los diálogos de detalle del cuadro.
// Muestra los criterios definidos por el modelo sin recalcular las reglas de clasificación.
// Conserva el orden de los equipos al mostrar penales y resalta el recorrido del campeón.

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

    public Team load(Tournament tournament) {
        this.tournament = tournament;
        Team champion = tournament.getChampion();
        bracketView.setMatches(buildRound(tournament.getQuarterFinalFirstLegs(), tournament.getQuarterFinalSecondLegs(),
                4, 0, "QUARTER-FINAL", champion, this::quarterPlaceholder),
                buildRound(tournament.getSemiFinalFirstLegs(), tournament.getSemiFinalSecondLegs(), 2, 4, "SEMI-FINAL",
                champion, this::semiPlaceholder), buildFinalCard(champion));
        bracketView.setActiveRound(activeRound());
        return champion;
    }

    private static final int[][] QUARTER_SEEDS = {
            {0, 0, 3, 1},
            {1, 0, 2, 1},
            {2, 0, 0, 1},
            {3, 0, 1, 1}
    };

    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI"};

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

    private List<Region> buildRound(List<? extends Match> firstLegs, List<? extends Match> secondLegs, int slots,
            int romanOffset, String roundTitle, Team champion, IntFunction<Region> placeholder) {
        List<Region> cards = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            if (firstLegs != null && i < firstLegs.size()) {
                Match second = (secondLegs != null && i < secondLegs.size())
                        ? secondLegs.get(i)
                        : null;
                cards.add(twoLegCard( roundTitle + " " + ROMAN[romanOffset + i], firstLegs.get(i), second, champion));
            } else {
                cards.add(placeholder.apply(i));
            }
        }
        return cards;
    }

    private Region twoLegCard(String title, Match first, Match second, Team champion) {
        Team a = first.getHomeTeam();
        Team b = first.getAwayTeam();
        Team winner = (second != null && second.isPlayed()) ? second.getWinner() : null;
        boolean decided = winner != null;
        String criterion = decided ? resolutionText(second, a, b) : null;
        VBox card = new VBox(4, teamRow(a.getName(), a.equals(winner), legGoals(first, a), legGoals(second, a)),
                teamRow(b.getName(), b.equals(winner), legGoals(first, b), legGoals(second, b)), infoLine("L1", first));
        if (second != null) {
            card.getChildren().add(infoLine("L2", second));
        }
        if (decided) {
            Label criterionLabel = new Label(criterion);
            criterionLabel.getStyleClass().add("bracket-criterion");
            card.getChildren().add(criterionLabel);
        }
        card.getStyleClass().addAll("bracket-card", decided ? "bracket-card-played" : "bracket-card-ready");
        card.getProperties().put(BracketView.CHAMPION_PATH, decided && winner.equals(champion));
        makeClickable(card, title, first, second, decided ? "WINNER: " + winner.getName() + " · " + criterion : null);
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
        String criterion = played ? resolutionText(finalMatch, a, b) : null;
        VBox card = new VBox(4, teamRow(a.getName(), a.equals(champion), finalScore(finalMatch, a)),
                teamRow(b.getName(), b.equals(champion), finalScore(finalMatch, b)), infoLine("MATCH", finalMatch));
        if (played) {
            Label criterionLabel = new Label(criterion);
            criterionLabel.getStyleClass().add("bracket-criterion");
            card.getChildren().add(criterionLabel);
        }
        card.getStyleClass().addAll("bracket-card", played ? "bracket-card-played" : "bracket-card-ready");
        card.setAlignment(Pos.CENTER_LEFT);
        makeClickable(card, "FINAL", finalMatch, null, (played && champion != null) ? "CHAMPION: " + champion.getName()
                + " · " + criterion : null);
        return card;
    }

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

    private Label infoLine(String tag, Match match) {
        Label line = new Label(tag + " · " + matchDate(match) + " · " + matchStadium(match) + " · REF "
                + matchReferee(match));
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

    private String legGoals(Match match, Team team) {
        if (match == null || !match.isPlayed()) {
            return "-";
        }
        return String.valueOf(goalsOf(match, team));
    }

    private String penalties(Match match, Team team) {
        Integer value = match.getHomeTeam().equals(team)
                ? match.getHomePenalties()
                : match.getAwayPenalties();
        return value == null ? "-" : String.valueOf(value);
    }

    private String resolutionText(Match match, Team a, Team b) {
        String description = match.getResolutionCriterion().getDescription();
        return match.getResolutionCriterion() == ResolutionCriterion.PENALTIES
                ? description + " (" + penalties(match, a) + "-" + penalties(match, b) + ")"
                : description;
    }

    private String finalScore(Match match, Team team) {
        if (!match.isPlayed()) {
            return "-";
        }
        boolean home = match.getHomeTeam().equals(team);
        String goals = String.valueOf(home ? match.getHomeGoals() : match.getAwayGoals());
        Integer pens = home ? match.getHomePenalties() : match.getAwayPenalties();
        return match.getResolutionCriterion() == ResolutionCriterion.PENALTIES ? goals + " (" + pens + ")" : goals;
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
        return lockedCard( "WINNER QF " + firstQf, "WINNER QF " + secondQf );
    }

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
        VBox box = new VBox(6, headingLabel, teams, score, detailRow("DATE", matchDate(match)), detailRow("STADIUM",
                matchStadium(match)), detailRow("REFEREE", matchReferee(match)));
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
