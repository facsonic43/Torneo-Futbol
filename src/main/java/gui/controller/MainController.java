package gui.controller;

import java.io.File;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import model.tournament.Tournament;
import service.TournamentSessionService;

/*
 * Controla la navegación principal de la aplicación.
 * Carga los módulos una sola vez y comparte la misma sesión del campeonato.
 */
public class MainController {

    @FXML
    private Button tournamentButton;

    @FXML
    private Button matchDayButton;

    @FXML
    private Button reportsButton;

    @FXML
    private Button databaseButton;

    @FXML
    private Button themeButton;

    @FXML
    private ImageView brandLogo;

    @FXML
    private Label pageTitle;

    @FXML
    private Label pageSubtitle;

    @FXML
    private Label statusLabel;

    @FXML
    private StackPane contentHost;

    @FXML
    private Label mainStageLabel;

    @FXML
    private Label mainMatchesLabel;

    @FXML
    private HBox globalTournamentStatusBox;

    private TournamentSessionService session;

    private Parent tournamentView;
    private Parent matchDayView;
    private Parent databaseView;

    private TournamentController tournamentController;
    private MatchDayController matchDayController;
    private DatabaseController databaseController;

    private void updateHeader(String title, String subtitle) {
        pageTitle.setText(title);
        if (subtitle == null || subtitle.isBlank()) {
            pageSubtitle.setText("");
            pageSubtitle.setVisible(false);
            pageSubtitle.setManaged(false);
        } else {
            pageSubtitle.setText(subtitle);
            pageSubtitle.setVisible(true);
            pageSubtitle.setManaged(true);
        }
    }

    public void setSession(TournamentSessionService session) {
        this.session = session;

        try {
            File logoFile = new File("Images/logo.png");
            if (logoFile.exists()) {
                brandLogo.setImage(new Image(logoFile.toURI().toString()));
            }

            loadModules();
            handleTournament();
            updateGlobalStatus();

            statusLabel.setText(
                    session.saveExists()
                            ? "Saved tournament loaded."
                            : "New tournament created."
            );

        } catch (Exception exception) {
            showError("Error starting interface", exception);
        }
    }

    private void loadModules() throws Exception {
        FXMLLoader tournamentLoader = new FXMLLoader(getClass().getResource("/view/tournament-view.fxml"));
        tournamentView = tournamentLoader.load();
        tournamentController = tournamentLoader.getController();
        tournamentController.setSession(session);

        FXMLLoader matchDayLoader = new FXMLLoader(getClass().getResource("/view/match-day-view.fxml"));
        matchDayView = matchDayLoader.load();
        matchDayController = matchDayLoader.getController();
        matchDayController.setSession(session);

        FXMLLoader databaseLoader = new FXMLLoader(getClass().getResource("/view/database-view.fxml"));
        databaseView = databaseLoader.load();
        databaseController = databaseLoader.getController();
        databaseController.setSession(session);
    }

    @FXML
    private void handleTournament() {
        contentHost.getChildren().setAll(tournamentView);
        tournamentController.showOverview();
        tournamentController.refreshAll();
        updateGlobalStatus();
        setActiveButton(tournamentButton);
        updateHeader("Tournament Overview - International Club Cup 2026", null);
    }

    @FXML
    private void handleMatchDay() {
        contentHost.getChildren().setAll(matchDayView);
        matchDayController.refreshFromMain();
        updateGlobalStatus();
        setActiveButton(matchDayButton);
        updateHeader("Match Center", "Play selected matches or simulate the current stage");
    }

    @FXML
    private void handleReports() {
        contentHost.getChildren().setAll(tournamentView);
        tournamentController.showReports();
        tournamentController.refreshAll();
        setActiveButton(reportsButton);
        updateHeader("Analytics, Reports & Bracket", "Comprehensive tournament statistics, bracket tracker and disciplinary logs");
    }

    @FXML
    private void handleDatabase() {
        contentHost.getChildren().setAll(databaseView);
        databaseController.refreshFromMain();
        setActiveButton(databaseButton);
        updateHeader("Database Management", "Persistent records, imports and system integrity");
    }

    @FXML
    private void handleContinue() {
        handleMatchDay();
    }

    @FXML
    private void handleThemeToggle() {
        Parent root = tournamentButton.getScene().getRoot();
        if (root.getStyleClass().contains("light-theme")) {
            root.getStyleClass().remove("light-theme");
            themeButton.setText("☼");
        } else {
            root.getStyleClass().add("light-theme");
            themeButton.setText("🌙");
        }
    }

    @FXML
    private void handleSave() {
        try {
            session.save();
            statusLabel.setText("Tournament saved successfully.");
        } catch (Exception exception) {
            showError("Error saving tournament", exception);
        }
    }

    @FXML
    private void handleMainMenu() {
        try {
            Alert alert = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Do you want to save your progress before returning to the Main Menu?",
                    ButtonType.YES,
                    ButtonType.NO,
                    ButtonType.CANCEL
            );
            alert.setTitle("Return to Main Menu");
            alert.setHeaderText("Unsaved progress may be lost.");

            ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);

            if (result == ButtonType.CANCEL) return;
            if (result == ButtonType.YES) session.save();

            Stage stage = (Stage) contentHost.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-menu-view.fxml"));
            Parent root = loader.load();

            MainMenuController menuController = loader.getController();
            menuController.setSession(session);

            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());

            java.net.URL cssUrl = getClass().getResource("/css/main.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            if (contentHost.getScene().getRoot().getStyleClass().contains("light-theme")) {
                root.getStyleClass().add("light-theme");
            }

            stage.setScene(scene);

        } catch (Exception exception) {
            showError("Error returning to Main Menu", exception);
        }
    }

    private void setActiveButton(Button selectedButton) {
        tournamentButton.getStyleClass().remove("nav-button-active");
        matchDayButton.getStyleClass().remove("nav-button-active");
        reportsButton.getStyleClass().remove("nav-button-active");
        databaseButton.getStyleClass().remove("nav-button-active");

        if (selectedButton != null) {
            selectedButton.getStyleClass().add("nav-button-active");
        }
    }

    private void showError(String message, Exception exception) {
        statusLabel.setText(message + ": " + exception.getMessage());
        exception.printStackTrace();
    }

    public void updateGlobalStatus() {
        if (session == null || session.getTournament() == null) return;

        Tournament tournament = session.getTournament();

        switch (session.getStage()) {
            case GROUP_STAGE -> mainStageLabel.setText("GROUP STAGE");
            case QUARTER_FINALS -> mainStageLabel.setText("QUARTER-FINALS");
            case SEMI_FINALS -> mainStageLabel.setText("SEMI-FINALS");
            case FINAL -> mainStageLabel.setText("FINAL");
            case FINISHED -> mainStageLabel.setText("FINISHED");
            default -> mainStageLabel.setText("NOT STARTED");
        }

        int played = 0;
        for (model.match.Match m : tournament.getAllMatches()) {
            if (m.isPlayed()) played++;
        }
        mainMatchesLabel.setText(played + " / 37");
    }
}