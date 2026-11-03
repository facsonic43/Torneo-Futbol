package exceptions;

// Indica que una ciudad no puede eliminarse mientras tenga estadios asociados.

public class CityHasStadiumsException extends RuntimeException {
    public CityHasStadiumsException(int cityId) {
        super("This city cannot be deleted because it is associated with one or more stadiums.");
    }
}
