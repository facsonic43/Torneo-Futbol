package gui;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.match.Event;
import model.match.Match;

// Muestra las incidencias del partido seleccionado en el panel de detalles.

public class MatchEventsView {
    private final VBox eventsBox;

    public MatchEventsView(VBox eventsBox) {
        this.eventsBox = eventsBox;
    }

    public VBox getEventsBox() {
        return eventsBox;
    }

    public void render(Match match) {
        eventsBox.getChildren().clear();
        if (match.getEvents().isEmpty()) {
            Label empty = new Label(match.isPlayed() ? "No events registered." : "Match has not been played yet.");
            empty.getStyleClass().add("muted-text");
            eventsBox.getChildren().add(empty);
            return;
        }
        for (Event event : match.getEvents()) {
            if (!event.belongsToShootout()) {
                Label eventLabel = new Label(event.getDescription());
                eventLabel.setWrapText(true);
                eventLabel.getStyleClass().add("event-row");
                eventsBox.getChildren().add(eventLabel);
            }
        }
    }
}
