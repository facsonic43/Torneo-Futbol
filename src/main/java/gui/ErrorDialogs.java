package gui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

// Presenta los errores en una ventana compartida, incluso si provienen de tareas en segundo plano.
public final class ErrorDialogs {
    private ErrorDialogs() { }

    public static void installUncaughtExceptionHandler(Window owner) {
        Thread.UncaughtExceptionHandler handler = (thread, exception) ->
                Platform.runLater(() -> show(owner, "Unexpected error", exception));
        Thread.currentThread().setUncaughtExceptionHandler(handler);
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }

    public static void show(Node source, String title, Throwable exception) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(source, title, exception));
            return;
        }
        show(source == null || source.getScene() == null ? null : source.getScene().getWindow(), title, exception);
    }

    public static void show(Window owner, String title, Throwable exception) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(owner, title, exception));
            return;
        }
        Window actualOwner = owner == null
                ? Window.getWindows().stream().filter(Window::isFocused).findFirst().orElse(null) : owner;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Error");
        dialog.setHeaderText(null);
        dialog.initModality(actualOwner == null ? Modality.APPLICATION_MODAL : Modality.WINDOW_MODAL);
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setMinWidth(460);
        if (actualOwner != null) {
            dialog.initOwner(actualOwner);
        }
        Scene scene = actualOwner == null ? null : actualOwner.getScene();
        if (scene != null) {
            pane.getStylesheets().addAll(scene.getStylesheets());
            if (scene.getRoot().getStyleClass().contains("light-theme")) {
                pane.getStyleClass().add("light-theme");
            }
        }
        if (pane.getStylesheets().isEmpty()) {
            pane.getStylesheets().add(ErrorDialogs.class.getResource("/css/main.css").toExternalForm());
        }
        pane.getStyleClass().add("app-root");
        Button closeButton = (Button) pane.lookupButton(ButtonType.CLOSE);
        closeButton.setText("Close");
        closeButton.getStyleClass().add("utility-button");
        Label errorType = new Label("ERROR");
        errorType.getStyleClass().add("blocked-chip");
        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        heading.setWrapText(true);
        Label details = new Label(messageOf(exception));
        details.getStyleClass().add("info-value");
        details.setWrapText(true);
        details.setMaxWidth(500);
        VBox errorCard = new VBox(12, errorType, heading, details);
        errorCard.getStyleClass().add("info-card");
        VBox content = new VBox(14, errorCard);
        content.getStyleClass().add("page-content");
        pane.setContent(content);
        dialog.show();
    }

    private static String messageOf(Throwable exception) {
        while ((exception.getMessage() == null || exception.getMessage().isBlank()) && exception.getCause() != null) {
            exception = exception.getCause();
        }
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
