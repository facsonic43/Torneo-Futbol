package PDF;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;

import java.io.IOException;

public class PDFGenerator {

    private Document pdf;

    public PDFGenerator() {
        try {
            this.pdf = new Document(new PdfDocument(new PdfWriter("Report.pdf")));
            System.out.println("Se genero el PDF con EXITO");
            pdf.close();
        } catch (IOException e) {
            System.err.println("Error when creating PDF");
        }
    }
}
