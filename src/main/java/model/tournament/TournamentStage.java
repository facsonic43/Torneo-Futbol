package model.tournament;

/*
 * Representa la etapa en la que se encuentra actualmente el campeonato.
 * Se utiliza principalmente para saber desde dónde continuar después
 * de cargar una partida guardada.
 */
public enum TournamentStage {
    NOT_STARTED, //sin grupos
    GROUP_STAGE, //grupos creados
    QUARTER_FINALS, // cuartos creados
    SEMI_FINALS, // semis creadas
    FINAL, // final creada
    FINISHED // campeon existente
}