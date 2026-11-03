package exceptions;

// Indica que ya existe una ciudad o un estadio con el nombre que se intenta registrar.

public class DuplicateLocationException extends RuntimeException {
    public DuplicateLocationException(String message) {
        super(message);
    }
}
