package model.tournament;

/*
 * Representa la etapa en la que se encuentra actualmente el campeonato.
 * Se utiliza principalmente para saber desde dónde continuar después
 * de cargar una partida guardada.
 */
public enum TournamentStage {
    NOT_STARTED("NOT STARTED"), //sin grupos
    GROUP_STAGE("GROUP STAGE"), //grupos creados
    QUARTER_FINALS("QUARTER-FINALS"), // cuartos creados
    SEMI_FINALS("SEMI-FINALS"), // semis creadas
    FINAL("FINAL"), // final creada
    FINISHED("FINISHED"); // campeon existente

    private final String label;

    TournamentStage(String label) {
        this.label = label;
    }

    /** Name of the stage as shown to the user. */
    public String getLabel() {
        return label;
    }
}
