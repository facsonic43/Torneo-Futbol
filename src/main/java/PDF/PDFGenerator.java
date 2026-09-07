package PDF;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import main.loader.TournamentData;
import model.participant.*;

import java.io.IOException;

public class PDFGenerator {

    private Document pdf;

    public PDFGenerator(TournamentData data) {
        try {
            this.pdf = new Document(new PdfDocument(new PdfWriter("Report.pdf")));
            System.out.println("Se genero el PDF con EXITO");
            loadPDF(data);
            pdf.close();
        } catch (IOException e) {
            System.err.println("Error when creating PDF");
        }
    }

    private void loadPDF(TournamentData data){
        People people=new People(data);
        pdf.add(new Paragraph(people.getInformation()));
    }
}
