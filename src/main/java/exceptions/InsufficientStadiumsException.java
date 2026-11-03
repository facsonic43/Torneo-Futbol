package exceptions;

// Indica que no hay suficientes estadios disponibles para organizar las eliminatorias.

public class InsufficientStadiumsException extends RuntimeException {
    public InsufficientStadiumsException(int required, int available) {
        super("At least " + required + " stadiums are required for the knockout stage without repetitions. "
                + "Available: " + available + ".");
    }
}
