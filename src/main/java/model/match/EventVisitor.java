package model.match;

/**
 * Procesa las incidencias mediante una operación específica para cada tipo.
 * Las operaciones predeterminadas no realizan acciones y cada visitante implementa las que necesita.
 */
public interface EventVisitor {
    default void visit(Goal goal) {
    }

    default void visit(YellowCard yellowCard) {
    }

    default void visit(RedCard redCard) {
    }

    default void visit(Substitution substitution) {
    }

    default void visit(Injury injury) {
    }

    default void visit(PenaltyTaken penaltyTaken) {
    }
}
