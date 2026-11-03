package model.participant;

/**
 * Define las posiciones de los futbolistas y sus nombres para mostrarlas en la interfaz.
 */
public enum Position {
    GOALKEEPER("Goalkeeper"),
    DEFENDER("Defender"),
    MIDFIELDER("Midfielder"),
    FORWARD("Forward");

    private final String label;

    Position(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
