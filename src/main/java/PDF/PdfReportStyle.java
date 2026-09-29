package PDF;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.UnitValue;
import model.participant.Person;
import model.participant.Position;

import java.util.Locale;

final class PdfReportStyle {
    static final DeviceRgb NAVY = new DeviceRgb(25, 48, 75);
    static final DeviceRgb LIGHT = new DeviceRgb(240, 244, 248);
    private PdfReportStyle() { }
    static void heading(Document document, String text) {
        document.add(new Paragraph(text).setFontSize(19).simulateBold().setFontColor(NAVY).setMarginBottom(12));
    }
    static void note(Document document, String text) {
        document.add(new Paragraph(text).setFontSize(9).setMarginBottom(12));
    }
    static Table table(float[] widths, String... headings) {
        Table table = new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth().setFontSize(9);
        for (String heading : headings) {
            table.addHeaderCell(new Cell().add(new Paragraph(heading).setMargin(0))
                    .setPadding(5).setBackgroundColor(NAVY).setFontColor(ColorConstants.WHITE).simulateBold());
        }
        return table;
    }
    static void row(Table table, Object... values) {
        for (Object value : values) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(value)).setMargin(0)).setPadding(5).setKeepTogether(true));
        }
    }
    static void empty(Table table, int columns, String message) {
        table.addCell(new Cell(1, columns).add(new Paragraph(message)).setPadding(8));
    }
    static String number(double value) { return String.format(Locale.ENGLISH, "%.2f", value); }
    static String country(Person person) { return person.getNationality() == null ? "Not specified" : person.getNationality().getName(); }
    static String position(Position position) {
        return switch (position) {
            case GOALKEEPER -> "Goalkeeper";
            case DEFENDER -> "Defender";
            case MIDFIELDER -> "Midfielder";
            case FORWARD -> "Forward";
        };
    }
}
