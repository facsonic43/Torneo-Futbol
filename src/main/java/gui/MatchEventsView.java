package gui;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.match.Event;
import model.match.Match;

/** Lists the incidents of a match in the details panel. */
public class MatchEventsView {

    private final VBox eventsBox;

    public MatchEventsView(VBox eventsBox) {
        this.eventsBox = eventsBox;
    }

    /*
     * Muestra las incidencias normales del partido.
     */
    public void render(
            Match match) {

        eventsBox
                .getChildren()
                .clear();

        if (match.getEvents()
                .isEmpty()) {

            Label empty =
                    new Label(
                            match.isPlayed()
                                    ? "No events registered."
                                    : "Match has not been played yet."
                    );

            empty.getStyleClass()
                    .add(
                            "muted-text"
                    );

            eventsBox
                    .getChildren()
                    .add(
                            empty
                    );

            return;
        }

        for (Event event :
                match.getEvents()) {

            if (!event.belongsToShootout()) {

                Label eventLabel =
                        new Label(
                                event.getDescription()
                        );

                eventLabel.setWrapText(
                        true
                );

                eventLabel.getStyleClass()
                        .add(
                                "event-row"
                        );

                eventsBox
                        .getChildren()
                        .add(
                                eventLabel
                        );
            }
        }
    }
}
