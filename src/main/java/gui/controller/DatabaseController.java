package gui.controller;

import dao.CityDAO;
import dao.StadiumDAO;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import model.match.City;
import model.match.Stadium;
import service.TournamentSessionService;

import java.sql.SQLException;
import java.util.List;

/*
 * Administra ciudades y estadios almacenados en PostgreSQL.
 * Las consultas SQL siguen delegadas a los DAO.
 */
public class DatabaseController {

    @FXML
    private ListView<City> cityList;

    @FXML
    private ListView<Stadium> stadiumList;

    @FXML
    private TextField cityNameField;

    @FXML
    private TextField cityCountryField;

    @FXML
    private TextField stadiumNameField;

    @FXML
    private ComboBox<City> stadiumCityCombo;

    @FXML
    private Label cityCount;

    @FXML
    private Label stadiumCount;

    @FXML
    private Label databaseStatus;

    private CityDAO cityDAO;
    private StadiumDAO stadiumDAO;

    private TournamentSessionService session;

    @FXML
    public void initialize() {

        cityDAO =
                new CityDAO();

        stadiumDAO =
                new StadiumDAO();

        cityList
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable,
                         oldValue,
                         newValue) -> {

                            if (newValue != null) {

                                showCity(
                                        newValue
                                );
                            }
                        }
                );

        stadiumList
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable,
                         oldValue,
                         newValue) -> {

                            if (newValue != null) {

                                showStadium(
                                        newValue
                                );
                            }
                        }
                );
    }

    public void setSession(
            TournamentSessionService session) {

        this.session =
                session;

        refreshData();
    }

    public void refreshFromMain() {

        refreshData();
    }

    private void refreshData() {

        try {

            List<City> cities =
                    cityDAO.findAll();

            List<Stadium> stadiums =
                    stadiumDAO.findAll();

            cityList
                    .getItems()
                    .setAll(
                            cities
                    );

            stadiumCityCombo
                    .getItems()
                    .setAll(
                            cities
                    );

            stadiumList
                    .getItems()
                    .setAll(
                            stadiums
                    );

            cityCount.setText(
                    cities.size()
                            + " CITIES"
            );

            stadiumCount.setText(
                    stadiums.size()
                            + " STADIUMS"
            );

            databaseStatus.setText(
                    "Database connected."
            );

        } catch (SQLException exception) {

            databaseStatus.setText(
                    "Database error: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    @FXML
    private void handleAddCity() {

        String name =
                cityNameField
                        .getText()
                        .trim();

        String country =
                cityCountryField
                        .getText()
                        .trim();

        if (name.isEmpty()
                || country.isEmpty()) {

            databaseStatus.setText(
                    "Complete city name and country."
            );

            return;
        }

        try {

            City city =
                    new City(
                            name,
                            country
                    );

            cityDAO.add(
                    city
            );

            databaseStatus.setText(
                    "City added successfully."
            );

            clearCity();

            refreshData();

        } catch (SQLException exception) {

            databaseStatus.setText(
                    "Error adding city: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    @FXML
    private void handleUpdateCity() {

        City selectedCity =
                cityList
                        .getSelectionModel()
                        .getSelectedItem();

        if (selectedCity == null) {

            databaseStatus.setText(
                    "Select a city first."
            );

            return;
        }

        String name =
                cityNameField
                        .getText()
                        .trim();

        String country =
                cityCountryField
                        .getText()
                        .trim();

        if (name.isEmpty()
                || country.isEmpty()) {

            databaseStatus.setText(
                    "Complete city name and country."
            );

            return;
        }

        try {

            selectedCity.setName(
                    name
            );

            selectedCity.setCountry(
                    country
            );

            cityDAO.update(
                    selectedCity
            );

            databaseStatus.setText(
                    "City updated successfully."
            );

            refreshData();

        } catch (SQLException exception) {

            databaseStatus.setText(
                    "Error updating city: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteCity() {

        City selectedCity =
                cityList
                        .getSelectionModel()
                        .getSelectedItem();

        if (selectedCity == null) {

            databaseStatus.setText(
                    "Select a city first."
            );

            return;
        }

        try {

            cityDAO.delete(
                    selectedCity.getId()
            );

            databaseStatus.setText(
                    "City deleted successfully."
            );

            clearCity();

            refreshData();

        } catch (SQLException exception) {

            databaseStatus.setText(
                    "The city cannot be deleted if it has associated stadiums."
            );
        }
    }

    @FXML
    private void handleAddStadium() {

        String name =
                stadiumNameField
                        .getText()
                        .trim();

        City selectedCity =
                stadiumCityCombo
                        .getValue();

        if (name.isEmpty()
                || selectedCity == null) {

            databaseStatus.setText(
                    "Complete stadium name and city."
            );

            return;
        }

        try {

            Stadium stadium =
                    new Stadium(
                            name,
                            selectedCity.getId()
                    );

            stadiumDAO.add(
                    stadium
            );

            databaseStatus.setText(
                    "Stadium added successfully."
            );

            clearStadium();

            refreshData();

            refreshTournamentStadiums();

        } catch (Exception exception) {

            databaseStatus.setText(
                    "Error adding stadium: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    @FXML
    private void handleUpdateStadium() {

        Stadium selectedStadium =
                stadiumList
                        .getSelectionModel()
                        .getSelectedItem();

        City selectedCity =
                stadiumCityCombo
                        .getValue();

        String name =
                stadiumNameField
                        .getText()
                        .trim();

        if (selectedStadium == null) {

            databaseStatus.setText(
                    "Select a stadium first."
            );

            return;
        }

        if (name.isEmpty()
                || selectedCity == null) {

            databaseStatus.setText(
                    "Complete stadium name and city."
            );

            return;
        }

        try {

            selectedStadium.setName(
                    name
            );

            selectedStadium.setCityId(
                    selectedCity.getId()
            );

            stadiumDAO.update(
                    selectedStadium
            );

            databaseStatus.setText(
                    "Stadium updated successfully."
            );

            refreshData();

            refreshTournamentStadiums();

        } catch (Exception exception) {

            databaseStatus.setText(
                    "Error updating stadium: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteStadium() {

        Stadium selectedStadium =
                stadiumList
                        .getSelectionModel()
                        .getSelectedItem();

        if (selectedStadium == null) {

            databaseStatus.setText(
                    "Select a stadium first."
            );

            return;
        }

        try {

            stadiumDAO.delete(
                    selectedStadium.getId()
            );

            databaseStatus.setText(
                    "Stadium deleted successfully."
            );

            clearStadium();

            refreshData();

            refreshTournamentStadiums();

        } catch (Exception exception) {

            databaseStatus.setText(
                    "Error deleting stadium: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    @FXML
    private void handleClearCity() {

        clearCity();
    }

    @FXML
    private void handleClearStadium() {

        clearStadium();
    }

    private void showCity(
            City city) {

        cityNameField.setText(
                city.getName()
        );

        cityCountryField.setText(
                city.getCountry()
        );
    }

    private void showStadium(
            Stadium stadium) {

        stadiumNameField.setText(
                stadium.getName()
        );

        for (City city :
                stadiumCityCombo
                        .getItems()) {

            if (city.getId()
                    == stadium.getCityId()) {

                stadiumCityCombo.setValue(
                        city
                );

                return;
            }
        }

        stadiumCityCombo.setValue(
                null
        );
    }

    private void refreshTournamentStadiums() {

        if (session == null) {
            return;
        }

        try {

            session.refreshStadiums();

        } catch (Exception exception) {

            databaseStatus.setText(
                    "Database changed, but tournament stadiums could not refresh: "
                            + exception.getMessage()
            );
        }
    }

    private void clearCity() {

        cityList
                .getSelectionModel()
                .clearSelection();

        cityNameField.clear();

        cityCountryField.clear();
    }

    private void clearStadium() {

        stadiumList
                .getSelectionModel()
                .clearSelection();

        stadiumNameField.clear();

        stadiumCityCombo.setValue(
                null
        );
    }
}