package run;

import dao.CityDAO;
import dao.StadiumDAO;

import model.match.City;
import model.match.Stadium;


import java.util.List;

public class TestDAOs {
    public static void main(String[] args) {
        try {
            CityDAO cityDAO = new CityDAO();
            List<City> cities = cityDAO.findAll();
            System.out.println("Cities found: " + cities.size());
            cities.forEach(System.out::println);

            StadiumDAO stadiumDAO = new StadiumDAO();
            List<Stadium> stadiums = stadiumDAO.findAll();
            System.out.println("\nStadiums found: " + stadiums.size());
            stadiums.forEach(System.out::println);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
