package run;

import gui.MainApplication;
import javafx.application.Application;

/*
 * Punto de entrada de la interfaz gráfica.
 * Se mantiene separado de MainApplication para facilitar
 * la ejecución de JavaFX desde IntelliJ.
 */
public class RunGui {

    public static void main(String[] args) {

        Application.launch(
                MainApplication.class,
                args
        );
    }
}