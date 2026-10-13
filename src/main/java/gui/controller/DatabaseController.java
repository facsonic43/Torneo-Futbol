package gui.controller;

import dao.CityDAO;
import dao.StadiumDAO;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
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

            showDatabaseError(
                    "Could not load cities and stadiums",
                    exception
            );
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

        } catch (SQLException | RuntimeException exception) {

            showDatabaseError(
                    "Could not add the city",
                    exception
            );
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

            City updatedCity =
                    new City(
                            selectedCity.getId(),
                            name,
                            country
                    );

            cityDAO.update(
                    updatedCity
            );

            databaseStatus.setText(
                    "City updated successfully."
            );

            refreshData();

        } catch (SQLException | RuntimeException exception) {

            showDatabaseError(
                    "Could not update the city",
                    exception
            );
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

        } catch (SQLException | RuntimeException exception) {

            showDatabaseError(
                    "Could not delete the city",
                    exception
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

        } catch (SQLException | RuntimeException exception) {

            showDatabaseError(
                    "Could not add the stadium",
                    exception
            );
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

            Stadium updatedStadium =
                    new Stadium(
                            selectedStadium.getId(),
                            name,
                            selectedCity.getId()
                    );

            stadiumDAO.update(
                    updatedStadium
            );

            databaseStatus.setText(
                    "Stadium updated successfully."
            );

            refreshData();

            refreshTournamentStadiums();

        } catch (SQLException | RuntimeException exception) {

            showDatabaseError(
                    "Could not update the stadium",
                    exception
            );
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

        } catch (SQLException | RuntimeException exception) {

            showDatabaseError(
                    "Could not delete the stadium",
                    exception
            );
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

            showDatabaseError(
                    "Stadiums were saved, but the tournament could not refresh",
                    exception
            );
        }
    }

    private void showDatabaseError(String title, Exception exception) {
        exception.printStackTrace();
        databaseStatus.setText("Database operation failed.");

        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Database error");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setMinWidth(460);

        Scene scene = databaseStatus.getScene();
        if (scene != null) {
            dialog.initOwner(scene.getWindow());
            pane.getStylesheets().addAll(scene.getStylesheets());

            if (scene.getRoot().getStyleClass().contains("light-theme")) {
                pane.getStyleClass().add("light-theme");
            }
        }
        pane.getStyleClass().add("app-root");

        Button closeButton = (Button) pane.lookupButton(ButtonType.CLOSE);
        closeButton.setText("Cerrar");
        closeButton.getStyleClass().add("utility-button");

        Label errorType = new Label("DATABASE ERROR");
        errorType.getStyleClass().add("blocked-chip");

        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        heading.setWrapText(true);

        Label details = new Label(message);
        details.getStyleClass().add("info-value");
        details.setWrapText(true);
        details.setMaxWidth(500);

        VBox errorCard = new VBox(12, errorType, heading, details);
        errorCard.getStyleClass().add("info-card");

        VBox content = new VBox(14, errorCard);
        content.getStyleClass().add("page-content");
        pane.setContent(content);

        dialog.showAndWait();
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