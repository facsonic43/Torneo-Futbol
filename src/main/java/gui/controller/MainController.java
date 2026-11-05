package gui.controller;

import java.io.File;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import model.tournament.Tournament;
import service.TournamentSessionService;

// Coordina la navegación de la ventana principal y comparte la sesión entre sus pantallas.
// Actualiza el estado del campeonato y mantiene el tema visual seleccionado.

public class MainController {
    @FXML
    private Button tournamentButton;

    @FXML
    private Button matchDayButton;

    @FXML
    private MenuButton reportsButton;

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

    void setSession(TournamentSessionService session) {
        this.session = session;
        try {
            File logoFile = new File("Images/logo.png");
            if (logoFile.exists()) {
                brandLogo.setImage(new Image(logoFile.toURI().toString()));
            }
            loadModules();
            handleTournament();
            updateGlobalStatus();
            statusLabel.setText(session.isLoadedFromSave() ? "Saved tournament loaded." : "New tournament created.");
        } catch (Exception exception) {
            showError("Error starting interface", exception);
        }
    }

    public Button getTournamentButton() {
        return tournamentButton;
    }

    public Button getMatchDayButton() {
        return matchDayButton;
    }

    public MenuButton getReportsButton() {
        return reportsButton;
    }

    public Button getDatabaseButton() {
        return databaseButton;
    }

    public Button getThemeButton() {
        return themeButton;
    }

    public ImageView getBrandLogo() {
        return brandLogo;
    }

    public Label getPageTitle() {
        return pageTitle;
    }

    public Label getPageSubtitle() {
        return pageSubtitle;
    }

    public Label getStatusLabel() {
        return statusLabel;
    }

    public StackPane getContentHost() {
        return contentHost;
    }

    public Label getMainStageLabel() {
        return mainStageLabel;
    }

    public Label getMainMatchesLabel() {
        return mainMatchesLabel;
    }

    public HBox getGlobalTournamentStatusBox() {
        return globalTournamentStatusBox;
    }

    public TournamentSessionService getSession() {
        return session;
    }

    public Parent getTournamentView() {
        return tournamentView;
    }

    public Parent getMatchDayView() {
        return matchDayView;
    }

    public Parent getDatabaseView() {
        return databaseView;
    }

    public TournamentController getTournamentController() {
        return tournamentController;
    }

    public MatchDayController getMatchDayController() {
        return matchDayController;
    }

    public DatabaseController getDatabaseController() {
        return databaseController;
    }

    private void setActiveButton(javafx.scene.control.Control selectedButton) {
        tournamentButton.getStyleClass().remove("nav-button-active");
        matchDayButton.getStyleClass().remove("nav-button-active");
        if (reportsButton != null) reportsButton.getStyleClass().remove("nav-button-active");
        databaseButton.getStyleClass().remove("nav-button-active");
        if (selectedButton != null) {
            selectedButton.getStyleClass().add("nav-button-active");
        }
    }

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
    private void handleShowReportsPane(ActionEvent event) {
        handleReports();
        if (tournamentController != null) {
            tournamentController.handleShowReportsPane(event);
        }
    }

    @FXML
    private void handleGeneratePdf(ActionEvent event) {
        if (tournamentController != null) {
            tournamentController.handleGeneratePdf(contentHost.getScene().getWindow());
        }
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
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Your tournament progress has been saved successfully.", ButtonType.OK);
            alert.initOwner(contentHost.getScene().getWindow());
            alert.setTitle("Progress Saved");
            alert.setHeaderText(null);
            alert.showAndWait();
        } catch (Exception exception) {
            showError("Error saving tournament", exception);
        }
    }

    @FXML
    private void handleMainMenu() {
        try {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Do you want to save your progress before returning to the Main Menu?", ButtonType.YES,
                    ButtonType.NO, ButtonType.CANCEL);
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

    private void showError(String message, Exception exception) {
        statusLabel.setText(message + ": " + exception.getMessage());
        exception.printStackTrace();
    }

    private void updateGlobalStatus() {
        if (session == null || session.getTournament() == null) return;
        Tournament tournament = session.getTournament();
        mainStageLabel.setText(session.getStage().getLabel());
        int played = 0;
        for (model.match.Match m : tournament.getAllMatches()) {
            if (m.isPlayed()) played++;
        }
        mainMatchesLabel.setText(played + " / 37");
    }
}
