package gui;

import gui.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import service.TournamentSessionService;

/*
 * Inicia la interfaz gráfica y prepara la sesión real del campeonato
 * antes de entregar los datos al controlador principal.
 */
public class MainApplication extends Application {

    @Override
    public void start(Stage stage)
            throws Exception {

        TournamentSessionService session =
                new TournamentSessionService();

        session.startSession();

        FXMLLoader loader =
                new FXMLLoader(
                        getClass()
                                .getResource(
                                        "/view/main-view.fxml"
                                )
                );

        Parent root =
                loader.load();

        MainController controller =
                loader.getController();

        controller.setSession(
                session
        );

        Scene scene =
                new Scene(
                        root,
                        1450,
                        850
                );

        scene.getStylesheets()
                .add(
                        getClass()
                                .getResource(
                                        "/css/main.css"
                                )
                                .toExternalForm()
                );

        stage.setTitle(
                "International Club Cup 2026"
        );

        stage.setMinWidth(
                1200
        );

        stage.setMinHeight(
                720
        );

        stage.setScene(
                scene
        );

        stage.setMaximized(
                true
        );

        stage.show();
    }
}