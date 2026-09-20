package gui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import service.TournamentSessionService;

/*
 * Controla la navegación principal de la aplicación.
 * Carga los módulos una sola vez y comparte la misma
 * sesión del campeonato entre todas las pantallas.
 */
public class MainController {

    @FXML
    private Button tournamentButton;

    @FXML
    private Button teamsButton;

    @FXML
    private Button matchDayButton;

    @FXML
    private Button bracketButton;

    @FXML
    private Button reportsButton;

    @FXML
    private Button databaseButton;

    @FXML
    private Label pageTitle;

    @FXML
    private Label pageSubtitle;

    @FXML
    private Label statusLabel;

    @FXML
    private StackPane contentHost;

    private TournamentSessionService session;

    private Parent tournamentView;
    private Parent matchDayView;
    private Parent databaseView;

    private TournamentController tournamentController;
    private MatchDayController matchDayController;
    private DatabaseController databaseController;

    public void setSession(
            TournamentSessionService session) {

        this.session =
                session;

        try {

            loadModules();

            handleTournament();

            statusLabel.setText(
                    session.saveExists()
                            ? "Saved tournament loaded."
                            : "New tournament created."
            );

        } catch (Exception exception) {

            showError(
                    "Error starting interface",
                    exception
            );
        }
    }

    // Carga cada módulo una sola vez.
    private void loadModules()
            throws Exception {

        FXMLLoader tournamentLoader =
                new FXMLLoader(
                        getClass()
                                .getResource(
                                        "/view/tournament-view.fxml"
                                )
                );

        tournamentView =
                tournamentLoader.load();

        tournamentController =
                tournamentLoader.getController();

        tournamentController.setSession(
                session
        );


        FXMLLoader matchDayLoader =
                new FXMLLoader(
                        getClass()
                                .getResource(
                                        "/view/match-day-view.fxml"
                                )
                );

        matchDayView =
                matchDayLoader.load();

        matchDayController =
                matchDayLoader.getController();

        matchDayController.setSession(
                session
        );


        FXMLLoader databaseLoader =
                new FXMLLoader(
                        getClass()
                                .getResource(
                                        "/view/database-view.fxml"
                                )
                );

        databaseView =
                databaseLoader.load();

        databaseController =
                databaseLoader.getController();

        databaseController.setSession(
                session
        );
    }

    @FXML
    private void handleTournament() {

        contentHost
                .getChildren()
                .setAll(
                        tournamentView
                );

        tournamentController
                .showOverview();

        tournamentController
                .refreshAll();

        setActiveButton(
                tournamentButton
        );

        pageTitle.setText(
                "Tournament Overview"
        );

        pageSubtitle.setText(
                "International Club Cup 2026"
        );
    }

    @FXML
    private void handleTeams() {

        contentHost
                .getChildren()
                .setAll(
                        tournamentView
                );

        tournamentController
                .showTeams();

        tournamentController
                .refreshAll();

        setActiveButton(
                teamsButton
        );

        pageTitle.setText(
                "Teams & Squad"
        );

        pageSubtitle.setText(
                "Clubs, coaches and player profiles"
        );
    }

    @FXML
    private void handleMatchDay() {

        contentHost
                .getChildren()
                .setAll(
                        matchDayView
                );

        matchDayController
                .refreshFromMain();

        setActiveButton(
                matchDayButton
        );

        pageTitle.setText(
                "Match Center"
        );

        pageSubtitle.setText(
                "Play selected matches or simulate the current stage"
        );
    }

    @FXML
    private void handleBracket() {

        contentHost
                .getChildren()
                .setAll(
                        tournamentView
                );

        tournamentController
                .showBracket();

        tournamentController
                .refreshAll();

        setActiveButton(
                bracketButton
        );

        pageTitle.setText(
                "Championship Bracket"
        );

        pageSubtitle.setText(
                "Qualified teams and knockout path"
        );
    }

    @FXML
    private void handleReports() {

        contentHost
                .getChildren()
                .setAll(
                        tournamentView
                );

        tournamentController
                .showReports();

        tournamentController
                .refreshAll();

        setActiveButton(
                reportsButton
        );

        pageTitle.setText(
                "Reports & Rankings"
        );

        pageSubtitle.setText(
                "Tournament statistics available at any time"
        );
    }

    @FXML
    private void handleDatabase() {

        contentHost
                .getChildren()
                .setAll(
                        databaseView
                );

        databaseController
                .refreshFromMain();

        setActiveButton(
                databaseButton
        );

        pageTitle.setText(
                "Cities & Stadiums"
        );

        pageSubtitle.setText(
                "Relational database administration"
        );
    }

    @FXML
    private void handleContinue() {

        handleMatchDay();
    }

    @FXML
    private void handleSave() {

        try {

            session.save();

            statusLabel.setText(
                    "Tournament saved successfully."
            );

        } catch (Exception exception) {

            showError(
                    "Error saving tournament",
                    exception
            );
        }
    }

    @FXML
    private void handleLoad() {

        try {

            session.load();

            tournamentController
                    .setSession(
                            session
                    );

            matchDayController
                    .setSession(
                            session
                    );

            databaseController
                    .setSession(
                            session
                    );

            handleTournament();

            statusLabel.setText(
                    "Tournament loaded successfully."
            );

        } catch (Exception exception) {

            showError(
                    "Error loading tournament",
                    exception
            );
        }
    }

    private void setActiveButton(
            Button selectedButton) {

        tournamentButton
                .getStyleClass()
                .remove(
                        "nav-button-active"
                );

        teamsButton
                .getStyleClass()
                .remove(
                        "nav-button-active"
                );

        matchDayButton
                .getStyleClass()
                .remove(
                        "nav-button-active"
                );

        bracketButton
                .getStyleClass()
                .remove(
                        "nav-button-active"
                );

        reportsButton
                .getStyleClass()
                .remove(
                        "nav-button-active"
                );

        databaseButton
                .getStyleClass()
                .remove(
                        "nav-button-active"
                );

        selectedButton
                .getStyleClass()
                .add(
                        "nav-button-active"
                );
    }

    private void showError(
            String message,
            Exception exception) {

        statusLabel.setText(
                message
                        + ": "
                        + exception.getMessage()
        );

        exception.printStackTrace();
    }
}