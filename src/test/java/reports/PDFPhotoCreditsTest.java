package reports;

import com.itextpdf.kernel.pdf.*;
import main.loader.TournamentData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reports.pdf.PDFGenerator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PDFPhotoCreditsTest {
    @TempDir Path directory;

    @Test
    void embedsExactPhotoCreditsSoTheyAccompanyThePdfWithoutExtraPages() throws Exception {
        String credits = "# Photo credits\nMessi — Author: Example photographer; CC BY-SA 4.0\n"
                + "Source: https://commons.wikimedia.org/wiki/File:Example.jpg\n";
        byte[] expected = credits.getBytes(StandardCharsets.UTF_8);
        Files.write(directory.resolve("PHOTO-CREDITS.md"), expected);
        Path output = directory.resolve("report.pdf");
        PDFGenerator.generate(new TournamentData(List.of(), List.of()), output,
                new ReportOptions(null, null, directory));

        try (PdfDocument pdf = new PdfDocument(new PdfReader(output.toString()))) {
            var attachments = pdf.getCatalog().getNameTree(PdfName.EmbeddedFiles).getNames();
            assertEquals(1, attachments.size());
            PdfObject value = attachments.values().iterator().next();
            PdfDictionary fileSpec = new PdfArray(value).getAsDictionary(0);
            assertNotNull(fileSpec);
            assertEquals("PHOTO-CREDITS.md", fileSpec.getAsString(PdfName.F).toUnicodeString());
            assertArrayEquals(expected, fileSpec.getAsDictionary(PdfName.EF).getAsStream(PdfName.F).getBytes());
            assertEquals(1, pdf.getNumberOfPages(), "Credits must not add visible report pages");
        }
    }

    @Test
    void exportsNormallyWhenNoPhotoCreditsFileExists() throws Exception {
        Path output = directory.resolve("report.pdf");
        PDFGenerator.generate(new TournamentData(List.of(), List.of()), output,
                new ReportOptions(null, null, directory));

        try (PdfDocument pdf = new PdfDocument(new PdfReader(output.toString()))) {
            assertTrue(pdf.getCatalog().getNameTree(PdfName.EmbeddedFiles).getNames().isEmpty());
            assertEquals(1, pdf.getNumberOfPages());
        }
    }
}
