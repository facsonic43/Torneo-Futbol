package run;

import dao.CityDAO;
import dao.StadiumDAO;
import db.DatabaseConnection;
import model.match.City;
import model.match.Stadium;

import java.sql.Connection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Permite comprobar de forma aislada que la conexión con PostgreSQL funciona
 * y que las ciudades y estadios necesarios para el campeonato pueden cargarse.
 */
public class DatabaseTest {

    public static void main(String[] args) {

        try {
            System.out.println("====================================");
            System.out.println("DATABASE CONNECTION TEST");
            System.out.println("====================================");

            Connection connection =
                    DatabaseConnection.getConnection();

            if (connection != null && !connection.isClosed()) {
                System.out.println("Connection: OK");
            }

            System.out.println(
                    "Database: "
                            + connection.getCatalog()
            );

            CityDAO cityDAO =
                    new CityDAO();

            StadiumDAO stadiumDAO =
                    new StadiumDAO();

            List<City> cities =
                    cityDAO.findAll();

            List<Stadium> stadiums =
                    stadiumDAO.findAll();

            System.out.println();
            System.out.println(
                    "Cities found: "
                            + cities.size()
            );

            System.out.println(
                    "Stadiums found: "
                            + stadiums.size()
            );

            System.out.println();
            System.out.println("CITIES");

            for (City city : cities) {
                System.out.println(
                        city.getId()
                                + " | "
                                + city.getName()
                                + " | "
                                + city.getCountry()
                );
            }

            System.out.println();
            System.out.println("STADIUMS");

            for (Stadium stadium : stadiums) {
                System.out.println(
                        stadium.getId()
                                + " | "
                                + stadium.getName()
                                + " | City ID: "
                                + stadium.getCityId()
                );
            }

            validateStadiumCities(
                    cities,
                    stadiums
            );

            if (stadiums.size() < 13) {
                throw new IllegalStateException(
                        "At least 13 stadiums are required for the knockout stage."
                );
            }

            System.out.println();
            System.out.println(
                    "Database structure: OK"
            );

            System.out.println(
                    "Knockout stadiums requirement: OK"
            );

            System.out.println();
            System.out.println(
                    "DATABASE TEST COMPLETED SUCCESSFULLY"
            );

        } catch (Exception exception) {

            System.out.println();
            System.out.println(
                    "DATABASE TEST FAILED"
            );

            System.out.println(
                    "Error: "
                            + exception.getMessage()
            );

            exception.printStackTrace();

        } finally {

            DatabaseConnection.closeConnection();
        }
    }

    // Verifica que cada estadio esté relacionado con una ciudad existente.
    private static void validateStadiumCities(
            List<City> cities,
            List<Stadium> stadiums) {

        Set<Integer> cityIds =
                new HashSet<>();

        for (City city : cities) {
            cityIds.add(
                    city.getId()
            );
        }

        for (Stadium stadium : stadiums) {

            if (!cityIds.contains(
                    stadium.getCityId())) {

                throw new IllegalStateException(
                        "Stadium "
                                + stadium.getName()
                                + " references a non-existing city: "
                                + stadium.getCityId()
                );
            }
        }
    }
}