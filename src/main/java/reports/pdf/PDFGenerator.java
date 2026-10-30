package reports.pdf;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEvent;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEventHandler;
import com.itextpdf.kernel.pdf.event.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.filespec.PdfFileSpec;
import com.itextpdf.layout.Document;
import main.loader.TournamentData;
import reports.ReportOptions;
import reports.ReportData;
import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;

/** Exports participant identification cards required by section E, item I. */
public class PDFGenerator {
    public static void generate(TournamentData data, Path output, ReportOptions options) throws IOException {
        generate(new ReportData(data), output, options);
    }

    public static void generate(ReportData data, Path output, ReportOptions options) throws IOException {
        Objects.requireNonNull(data, "Tournament data is required");
        Objects.requireNonNull(output, "Output path is required");
        Objects.requireNonNull(options, "Report options are required");
        // Preserve the previous report if generation fails before the new file is closed.
        Path destination = output.toAbsolutePath();
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), ".championship-report-", ".pdf");
        try {
            write(data, options, temporary);
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void write(ReportData data, ReportOptions options, Path output) throws IOException {
        try (PdfWriter writer = new PdfWriter(output.toString());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4)) {
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            document.setFont(font).setFontSize(10);
            document.setMargins(36, 36, 42, 36);
            pdf.getDocumentInfo().setTitle("Continental Cup - Participant identification cards").setAuthor("Championship");
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new PageFooter(font));
            new IdentificationPdfSection().addTo(document, data, options.photosDirectory());
            Path photoCredits = options.photosDirectory().resolve("PHOTO-CREDITS.md");
            if (Files.isRegularFile(photoCredits)) {
                PdfFileSpec attachment = PdfFileSpec.createEmbeddedFileSpec(pdf,
                        Files.readAllBytes(photoCredits), "Photograph sources, authors and licenses",
                        "PHOTO-CREDITS.md", new PdfName("text/markdown"), null, PdfName.Source);
                pdf.addFileAttachment("Photograph credits and licenses", attachment);
            }
        }
    }

    private static class PageFooter extends AbstractPdfDocumentEventHandler {
        private final PdfFont font;
        PageFooter(PdfFont font) { this.font = font; }
        @Override
        protected void onAcceptedEvent(AbstractPdfDocumentEvent event) {
            PdfDocumentEvent pageEvent = (PdfDocumentEvent) event;
            PdfPage page = pageEvent.getPage();
            PdfDocument pdf = pageEvent.getDocument();
            PdfCanvas canvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdf);
            canvas.beginText().setFontAndSize(font, 8).moveText(36, 22)
                    .showText("Continental Cup | Identification cards | Page " + pdf.getPageNumber(page)).endText();
            canvas.release();
        }
    }
}
