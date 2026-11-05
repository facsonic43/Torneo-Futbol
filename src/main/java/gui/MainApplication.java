package gui;

import gui.controller.MainMenuController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import service.TournamentSessionService;

import java.util.Locale;

// Inicia la interfaz JavaFX, carga el menú principal y aplica los estilos de la aplicación.

public class MainApplication extends Application {
    @Override
    public void init() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        TournamentSessionService session = new TournamentSessionService();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-menu-view.fxml"));
        Parent root = loader.load();
        MainMenuController menuController = loader.getController();
        menuController.setSession(session);
        Scene scene = new Scene(root, 1280, 720);
        scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        primaryStage.setTitle("International Club Cup 2026");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
