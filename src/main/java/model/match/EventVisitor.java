package model.match;

/**
 * Visits a match incident according to its concrete type, without asking for its class.
 * Every method does nothing by default, so implementations override only the incidents they need.
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
