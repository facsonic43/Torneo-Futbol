package model.tournament;

/**
 * Define las etapas del campeonato y el nombre que se muestra para cada una en la interfaz.
 */
public enum TournamentStage {
    NOT_STARTED("NOT STARTED"),
    GROUP_STAGE("GROUP STAGE"),
    QUARTER_FINALS("QUARTER-FINALS"),
    SEMI_FINALS("SEMI-FINALS"),
    FINAL("FINAL"),
    FINISHED("FINISHED");

    private final String label;

    TournamentStage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
