package exceptions;

public class CityHasStadiumsException extends RuntimeException {
    public CityHasStadiumsException(int cityId) {
        super("No se puede eliminar la ciudad " + cityId + " porque tiene estadios asociados.");
    }
}