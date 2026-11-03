package exceptions;

// Indica que no se puede incorporar otro jugador porque el plantel alcanzó su límite de 18.

public class FullSquadException extends RuntimeException {
    public FullSquadException() {
        super("The squad has reached its maximum limit of 18 players.");
    }
}
