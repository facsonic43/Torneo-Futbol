package reports.pdf;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import model.participant.Person;

// Reúne colores y formatos de texto comunes a las secciones del reporte PDF.

final class PdfReportStyle {
    private static final DeviceRgb NAVY = new DeviceRgb(25, 48, 75);
    private static final DeviceRgb LIGHT = new DeviceRgb(240, 244, 248);
    private PdfReportStyle() { }
    static DeviceRgb getNavy() {
        return NAVY;
    }
    static DeviceRgb getLight() {
        return LIGHT;
    }
    static void heading(Document document, String text) {
        document.add(new Paragraph(text).setFontSize(19).simulateBold().setFontColor(NAVY).setMarginBottom(12));
    }
    static void note(Document document, String text) {
        document.add(new Paragraph(text).setFontSize(9).setMarginBottom(12));
    }
    static String country(Person person) { return person.getNationality() == null ? "Not specified" : person.getNationality().getName(); }
}
