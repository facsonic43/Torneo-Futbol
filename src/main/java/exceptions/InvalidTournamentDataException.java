package exceptions;

/**
 * Indica que los datos iniciales del campeonato no cumplen las validaciones requeridas.
 */
public class InvalidTournamentDataException extends RuntimeException {
    public InvalidTournamentDataException(String message) {
        super(message);
    }
}
