package exceptions;

public class CityHasStadiumsException extends RuntimeException {
    public CityHasStadiumsException(int cityId) {
        super("City " + cityId + " cannot be deleted, it has stadiums associated.");
    }
}