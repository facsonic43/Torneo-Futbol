package PDF;

import com.itextpdf.barcodes.Barcode128;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import reports.ReportData;
import model.participant.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Printable cards with a Code 128 document identifier and a supplied photograph. */
final class IdentificationPdfSection {
    void addTo(Document document, ReportData data, Path photosDirectory) throws IOException {
        PdfReportStyle.heading(document, "I. Participant identification cards");
        PdfReportStyle.note(document, "Control-point identifier: document type and number, encoded as Code 128. "
                + "A missing photograph is explicitly marked on the card.");
        Table cards = new Table(UnitValue.createPercentArray(2)).useAllAvailableWidth();
        int count = 0;
        for (Referee referee : data.getReferees()) {
            cards.addCell(card(document.getPdfDocument(), referee, "Referee", null, photosDirectory));
            count++;
        }
        for (Team team : data.getTeams()) {
            if (team.getCoach() != null) {
                cards.addCell(card(document.getPdfDocument(), team.getCoach(), "Head coach", team, photosDirectory));
                count++;
            }
            for (Player player : team.getSquad()) {
                cards.addCell(card(document.getPdfDocument(), player,
                        "Player / " + PdfReportStyle.position(player.getPosition()), team, photosDirectory));
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

    private Cell card(PdfDocument pdf, Person person, String role, Team team, Path photosDirectory) throws IOException {
        Cell card = new Cell().setPadding(9).setKeepTogether(true);
        card.add(new Paragraph("CONTINENTAL CUP | " + role).setFontSize(8).simulateBold()
                .setFontColor(PdfReportStyle.NAVY).setMargin(0));
        card.add(new Paragraph(person.getName()).simulateBold().setFontSize(11).setMarginTop(4).setMarginBottom(5));
        Table details = new Table(UnitValue.createPercentArray(new float[]{1, 2.2f})).useAllAvailableWidth();
        Cell photo = new Cell().setBorder(Border.NO_BORDER).setPadding(2).setVerticalAlignment(VerticalAlignment.MIDDLE);
        Path photograph = findPhotograph(person, photosDirectory);
        if (photograph != null) {
            try {
                photo.add(new Image(ImageDataFactory.create(Files.readAllBytes(photograph))).scaleToFit(64, 78)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER));
            } catch (RuntimeException e) {
                throw new IOException("Could not read photograph: " + photograph, e);
            }
        } else {
            photo.setBackgroundColor(PdfReportStyle.LIGHT).setMinHeight(76)
                    .add(new Paragraph("Photo\nunavailable").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
        }
        details.addCell(photo);
        String birthDate = person.getBirthDate() == null ? "Not specified"
                : person.getBirthDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String personal = "Document: " + person.getIdType() + " " + person.getIdNumber()
                + "\nBorn: " + birthDate + "\nAge: " + person.getAge()
                + "\nNationality: " + PdfReportStyle.country(person)
                + (team == null ? "" : "\nTeam: " + team.getName());
        details.addCell(new Cell().setBorder(Border.NO_BORDER).setPaddingLeft(7)
                .add(new Paragraph(personal).setFontSize(8).setMargin(0)));
        card.add(details);
        Barcode128 barcode = new Barcode128(pdf);
        barcode.setCode(identifier(person));
        barcode.setBarHeight(25);
        barcode.setSize(8);
        barcode.setX(0.8f);
        // Preserve white quiet zones on each side of the bars for control-point scanners.
        card.add(new Image(barcode.createFormXObject(pdf)).setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginTop(7).setMarginLeft(10).setMarginRight(10));
        return card;
    }

    static String identifier(Person person) {
        return person.getIdType().trim().toUpperCase(Locale.ROOT) + "-" + person.getIdNumber();
    }

    static Path findPhotograph(Person person, Path directory) {
        if (directory == null) return null;
        String identifier = identifier(person);
        for (String extension : new String[]{".jpg", ".jpeg", ".png", ".JPG", ".JPEG", ".PNG"}) {
            Path path = directory.resolve(identifier + extension);
            if (Files.isRegularFile(path)) return path;
        }
        return null;
    }
}
