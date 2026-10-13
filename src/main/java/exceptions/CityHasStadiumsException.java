package exceptions;

public class CityHasStadiumsException extends RuntimeException {
    public CityHasStadiumsException(int cityId) {
        super("This city cannot be deleted because it is associated with one or more stadiums.");
    }
}