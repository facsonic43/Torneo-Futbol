package exceptions;

public class InvalidTournamentDataException extends RuntimeException {
    public InvalidTournamentDataException(String message) {
        super(message);
    }
}
