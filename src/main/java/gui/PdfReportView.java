package gui;

import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.ScrollPaneSkin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import main.loader.TournamentData;
import model.match.Goal;
import model.tournament.Tournament;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import reports.ReportData;
import reports.pdf.StatisticsPreviewGenerator;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Displays the original PDF pages so the on-screen report uses the same layout. */
public final class PdfReportView extends ScrollPane {
    private static final double PAGE_PADDING = 24;
    private final VBox pages = new VBox(20);
    private Task<List<Image>> currentTask;

    public PdfReportView() {
        setMinSize(0, 0);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setStyle("-fx-background: #30343b; -fx-background-color: #30343b; -fx-padding: 0;");
        pages.setAlignment(Pos.TOP_CENTER);
        pages.setFillWidth(false);
        pages.setPadding(new Insets(PAGE_PADDING));
        pages.setStyle("-fx-background-color: #30343b;");
        setContent(pages);
        sceneProperty().addListener((observable, previous, scene) -> {
            if (scene == null) cancelPendingReport();
        });
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        ScrollPaneSkin skin = new ScrollPaneSkin(this);
        // ScrollPane caches its viewport bitmap; report changes must display the new pages.
        for (Node child : skin.getChildren()) {
            if (child.getStyleClass().contains("viewport")) child.setCache(false);
        }
        return skin;
    }

    /** Generates and renders the selected report without blocking the JavaFX thread. */
    public void showReport(Tournament tournament, StatisticsPreviewGenerator.Section section) throws IOException {
        Objects.requireNonNull(tournament, "Tournament is required");
        Objects.requireNonNull(section, "Report section is required");
        cancelPendingReport();
        // Capture on the JavaFX thread before simulation or navigation can change the live model.
        byte[] snapshot = snapshot(tournament);
        setVvalue(0);
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(36, 36);
        progress.setMaxSize(36, 36);
        pages.getChildren().setAll(progress, message("Generating report..."));

        Task<List<Image>> task = new Task<>() {
            @Override
            protected List<Image> call() throws Exception {
                if (isCancelled()) return List.of();
                ReportData data = readSnapshot(snapshot);
                byte[] bytes = StatisticsPreviewGenerator.generate(data, section);
                if (isCancelled()) return List.of();
                List<Image> result = new ArrayList<>();
                try (PDDocument document = Loader.loadPDF(bytes)) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    for (int index = 0; index < document.getNumberOfPages(); index++) {
                        if (isCancelled()) return List.of();
                        BufferedImage rendered = renderer.renderImage(index, 2, ImageType.RGB);
                        try {
                            if (isCancelled()) return List.of();
                            result.add(toFxImage(rendered));
                        } finally {
                            rendered.flush();
                        }
                    }
                }
                return result;
            }
        };
        currentTask = task;
        task.setOnSucceeded(event -> {
            if (currentTask != task) return;
            currentTask = null;
            pages.getChildren().clear();
            int number = 1;
            for (Image page : task.getValue()) {
                ImageView view = new ImageView(page);
                view.setPreserveRatio(true);
                view.setSmooth(true);
                view.setAccessibleText("Report page " + number++);
                view.fitWidthProperty().bind(Bindings.createDoubleBinding(
                        () -> Math.max(1, getViewportBounds().getWidth() - PAGE_PADDING * 2),
                        viewportBoundsProperty()));
                pages.getChildren().add(view);
            }
            if (pages.getChildren().isEmpty()) pages.getChildren().add(message("No report pages available."));
            setVvalue(0);
        });
        task.setOnFailed(event -> {
            if (currentTask != task) return;
            currentTask = null;
            String detail = task.getException().getMessage();
            pages.getChildren().setAll(message("Could not display this report."
                    + (detail == null || detail.isBlank() ? "" : "\n" + detail)));
        });
        Thread worker = new Thread(task, "pdf-report-preview");
        worker.setDaemon(true);
        worker.start();
    }

    private void cancelPendingReport() {
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
    }

    private static byte[] snapshot(Tournament tournament) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(tournament);
        }
        return bytes.toByteArray();
    }

    private static ReportData readSnapshot(byte[] bytes) throws IOException, ClassNotFoundException {
        Tournament snapshot;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            snapshot = (Tournament) input.readObject();
        }
        ReportData data = new ReportData(new TournamentData(snapshot.getTeams(), snapshot.getReferees()));
        snapshot.getGroups().forEach(data::addGroup);
        snapshot.getAllMatches().forEach(match -> {
            data.addMatch(match);
            for (var event : match.getEvents()) {
                if (event instanceof Goal goal) {
                    data.getMatchDetails(match).setGoalDetails(goal, goal.isPenalty(), goal.isOwnGoal(),
                            goal.getGoalkeeperConceded());
                }
            }
        });
        return data;
    }

    private static Image toFxImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        WritableImage result = new WritableImage(width, height);
        result.getPixelWriter().setPixels(0, 0, width, height,
                PixelFormat.getIntArgbInstance(), pixels, 0, width);
        return result;
    }

    private static Label message(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(600);
        label.setStyle("-fx-text-fill: #f0f2f5; -fx-font-size: 14px;");
        return label;
    }
}
