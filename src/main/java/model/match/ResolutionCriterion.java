package model.match;

/**
 * Criterios comunes para describir la resolución de partidos y series en todas las vistas.
 */
public enum ResolutionCriterion {
    NOT_PLAYED("NOT PLAYED"),
    REGULAR_TIME("IN 90 MIN"),
    DRAW("DRAW IN 90 MIN"),
    SERIES_POINTS("BY SERIES POINTS"),
    AGGREGATE_GOALS("BY AGGREGATE GOALS"),
    AWAY_GOALS("BY AWAY GOALS"),
    WEIGHTED_GOAL_DIFFERENCE("BY WEIGHTED GOAL DIFFERENCE"),
    PENALTIES("BY PENALTIES"),
    UNRESOLVED("UNRESOLVED"),
    FORFEIT("BY FORFEIT");

    private final String description;

    ResolutionCriterion(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
