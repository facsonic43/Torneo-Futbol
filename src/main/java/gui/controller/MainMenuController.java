package gui.controller;

import java.io.File;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import service.TournamentSessionService;

public class MainMenuController {

    @FXML
    private ImageView menuLogo;

    @FXML
    private Button continueButton;

    @FXML
    private Button themeButton;

    private TournamentSessionService session;

    public void setSession(TournamentSessionService session) {
        this.session = session;
        checkSaveFile();
        loadLogo();
    }

    private void loadLogo() {
        File logoFile = new File("Images/logo.png");
        if (logoFile.exists()) {
            menuLogo.setImage(new Image(logoFile.toURI().toString()));
        }
    }

    private void checkSaveFile() {
        boolean saveExists = session != null && session.saveExists();
        continueButton.setDisable(!saveExists);

        if (saveExists) {
            if (!continueButton.getStyleClass().contains("primary-button")) {
                continueButton.getStyleClass().add("primary-button");
            }
            continueButton.getStyleClass().remove("secondary-button");
        } else {
            continueButton.getStyleClass().remove("primary-button");
            if (!continueButton.getStyleClass().contains("secondary-button")) {
                continueButton.getStyleClass().add("secondary-button");
            }
        }
    }

    @FXML
    private void handleThemeToggle() {
        Parent root = continueButton.getScene().getRoot();
        if (root.getStyleClass().contains("light-theme")) {
            root.getStyleClass().remove("light-theme");
            themeButton.setText("☀");
        } else {
            root.getStyleClass().add("light-theme");
            themeButton.setText("🌙");
        }
    }

    @FXML
    private void handleContinueGame() {
        try {
            session.load();
            launchMainApp();
        } catch (Exception ex) {
            showError("Error loading saved game", ex);
        }
    }

    @FXML
    private void handleNewGame() {
        try {
            if (session.saveExists()) {
                Alert alert = new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Starting a new game will overwrite your previous save data. Do you want to continue?",
                        ButtonType.YES,
                        ButtonType.NO
                );
                alert.setTitle("New Game Confirmation");
                alert.setHeaderText(null);

                if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                    return;
                }
            }

            session.createNewTournament();
            launchMainApp();

        } catch (Exception ex) {
            showError("Error creating new game", ex);
        }
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    private void launchMainApp() throws Exception {
        Stage stage = (Stage) continueButton.getScene().getWindow();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-view.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();
        controller.setSession(session);

        Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());

        java.net.URL cssUrl = getClass().getResource("/css/main.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        // Si el menú tenía el modo claro activo, se propaga a la vista principal
        if (continueButton.getScene().getRoot().getStyleClass().contains("light-theme")) {
            root.getStyleClass().add("light-theme");
        }

        stage.setScene(scene);
    }

    private void showError(String message, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message + ": " + ex.getMessage(), ButtonType.OK);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}