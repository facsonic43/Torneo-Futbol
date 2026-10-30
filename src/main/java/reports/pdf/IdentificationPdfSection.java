package reports.pdf;

import com.itextpdf.barcodes.Barcode128;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import com.itextpdf.layout.renderer.CellRenderer;
import com.itextpdf.layout.renderer.DrawContext;
import com.itextpdf.layout.renderer.IRenderer;
import reports.ReportData;
import model.participant.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Printable cards with a Code 128 document identifier and a supplied photograph. */
final class IdentificationPdfSection {
    private static final float PHOTO_HEIGHT = 120;

    void addTo(Document document, ReportData data, Path photosDirectory) throws IOException {
        PdfReportStyle.heading(document, "I. Participant identification cards");
        PdfReportStyle.note(document, "Control-point identifier: document type and number, encoded as Code 128. "
                + "A missing photograph is explicitly marked on the card.");
        Table cards = new Table(UnitValue.createPercentArray(2)).useAllAvailableWidth();
        int count = 0;
        for (Referee referee : data.getReferees()) {
            cards.addCell(card(document.getPdfDocument(), referee, "Referee", null,
                    findPhotograph(referee, photosDirectory.resolve("Referees"))));
            count++;
        }
        for (Team team : data.getTeams()) {
            if (team.getCoach() != null) {
                cards.addCell(card(document.getPdfDocument(), team.getCoach(), "Head coach", team,
                        findPhotograph(team.getCoach(), photosDirectory.resolve("Coaches"))));
                count++;
            }
            for (Player player : team.getSquad()) {
                cards.addCell(card(document.getPdfDocument(), player,
                        "Player / " + player.getPosition().getLabel(), team,
                        findPhotograph(player, photosDirectory.resolve("Players"))));
                count++;
            }
        }
        if (count == 0) {
            document.add(new Paragraph("No participants registered."));
        } else {
            if (count % 2 != 0) cards.addCell(new Cell().setBorder(Border.NO_BORDER));
            document.add(cards);
        }
    }

    private Cell card(PdfDocument pdf, Person person, String role, Team team, Path photograph) throws IOException {
        Cell card = new Cell().setPadding(9).setKeepTogether(true);
        card.add(new Paragraph("CONTINENTAL CUP | " + role).setFontSize(8).simulateBold()
                .setFontColor(PdfReportStyle.NAVY).setMargin(0));
        card.add(new Paragraph(person.getName()).simulateBold().setFontSize(11).setMarginTop(4).setMarginBottom(5));
        Table details = new Table(UnitValue.createPercentArray(new float[]{1, 2.2f})).useAllAvailableWidth();
        Cell photo = new Cell().setBorder(Border.NO_BORDER).setPadding(0)
                .setMinHeight(PHOTO_HEIGHT).setBackgroundColor(PdfReportStyle.LIGHT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (photograph != null) {
            try {
                photo.setNextRenderer(new PhotoCellRenderer(photo,
                        new PdfImageXObject(ImageDataFactory.create(Files.readAllBytes(photograph)))));
            } catch (RuntimeException e) {
                throw new IOException("Could not read photograph: " + photograph, e);
            }
        } else {
            photo.add(new Paragraph("Photo\nunavailable").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
        }
        details.addCell(photo);
        String birthDate = person.getBirthDate() == null ? "Not specified"
                : person.getBirthDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String personal = "Document: " + person.getIdType() + " " + person.getIdNumber()
                + "\nBorn: " + birthDate + "\nAge: " + person.getAge()
                + "\nNationality: " + PdfReportStyle.country(person)
                + (team == null ? "" : "\nTeam: " + team.getName());
        Cell information = new Cell().setBorder(Border.NO_BORDER).setPaddingLeft(7)
                .add(new Paragraph(personal).setFontSize(8).setMargin(0));
        Barcode128 barcode = new Barcode128(pdf);
        barcode.setCode(identifier(person));
        barcode.setBarHeight(25);
        barcode.setSize(8);
        barcode.setX(0.8f);
        // Preserve white quiet zones on each side of the bars for control-point scanners.
        information.add(new Image(barcode.createFormXObject(pdf)).setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginTop(7).setMarginLeft(10).setMarginRight(10));
        details.addCell(information);
        card.add(details);
        return card;
    }

    /** Paints to the final cell bounds, including any extra height required by the adjacent text. */
    static final class PhotoCellRenderer extends CellRenderer {
        private final PdfImageXObject image;

        PhotoCellRenderer(Cell cell, PdfImageXObject image) {
            super(cell);
            this.image = image;
        }

        @Override
        public IRenderer getNextRenderer() {
            return new PhotoCellRenderer((Cell) getModelElement(), image);
        }

        @Override
        public void drawBackground(DrawContext context) {
            super.drawBackground(context);
            Rectangle frame = getOccupiedAreaBBox();
            float scale = Math.max(frame.getWidth() / image.getWidth(), frame.getHeight() / image.getHeight());
            float width = image.getWidth() * scale;
            float height = image.getHeight() * scale;
            Rectangle placement = new Rectangle(frame.getX() + (frame.getWidth() - width) / 2,
                    frame.getY() + (frame.getHeight() - height) / 2, width, height);
            PdfCanvas canvas = context.getCanvas();
            canvas.saveState().rectangle(frame).clip().endPath();
            canvas.addXObjectFittedIntoRectangle(image, placement);
            canvas.restoreState();
        }

    }

    static String identifier(Person person) {
        return person.getIdType().trim().toUpperCase(Locale.ROOT) + "-" + person.getIdNumber();
    }

    static Path findPhotograph(Person person, Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return null;
        String document = identifier(person);
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).filter(path -> {
                String name = path.getFileName().toString();
                int dot = name.lastIndexOf('.');
                if (dot < 0 || !name.substring(0, dot).equalsIgnoreCase(document)) return false;
                String extension = name.substring(dot).toLowerCase(Locale.ROOT);
                return extension.equals(".jpg") || extension.equals(".jpeg") || extension.equals(".png");
            }).sorted().findFirst().orElse(null);
        }
    }
}
