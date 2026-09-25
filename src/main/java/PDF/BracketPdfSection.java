package PDF;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.renderer.DivRenderer;
import com.itextpdf.layout.renderer.DrawContext;
import com.itextpdf.layout.renderer.IRenderer;
import reports.ReportData;
import reports.BracketReport;
import reports.BracketReport.BracketNode;
import reports.BracketReport.GroupQualification;

/** Qualification, a connected bracket diagram and complete match results. */
public final class BracketPdfSection {
    private static final DeviceRgb BLUE = new DeviceRgb(27, 64, 99);
    private static final DeviceRgb PALE_BLUE = new DeviceRgb(239, 245, 251);

    private BracketPdfSection() {}

    public static void addTo(Document document, ReportData data) {
        BracketReport report = new BracketReport(data);
        document.add(new Paragraph("V. Championship bracket").setFontSize(18).simulateBold().setFontColor(BLUE));
        document.add(new Paragraph("Qualification is confirmed after all six distinct group fixtures have been played. "
                + "An unresolved tie remains pending. Zone numbers follow the registered group order.")
                .setFontSize(9));
        Table qualifiers = new Table(UnitValue.createPercentArray(new float[]{17, 23, 23, 10, 27})).useAllAvailableWidth();
        for (String heading : new String[]{"Zone / group", "First place", "Second place", "Played", "Status"})
            qualifiers.addHeaderCell(header(heading));
        for (int i = 0; i < report.getQualifiers().size(); i++) {
            GroupQualification q = report.getQualifiers().get(i);
            for (String value : new String[]{(i + 1) + " / " + q.groupName(), BracketReport.name(q.first()),
                    BracketReport.name(q.second()), q.playedMatches() + "/" + q.requiredMatches(), q.status()})
                qualifiers.addCell(cell(value));
        }
        document.add(qualifiers);
        document.add(new Paragraph("Quarter-finals and semi-finals: two legs. Final: one match. "
                + "Arrows show the path to the championship. Diagram scores follow the team order shown in each box.")
                .setFontSize(9).setMarginTop(10));
        Div diagram = new Div().setHeight(445).setKeepTogether(true);
        diagram.setNextRenderer(new BracketRenderer(diagram, report));
        document.add(diagram);
        document.add(new Paragraph("Champion: " + BracketReport.name(report.getChampion()))
                .simulateBold().setFontColor(BLUE).setFontSize(12));
        document.add(new Paragraph("Match details").simulateBold().setFontSize(13).setKeepWithNext(true));
        document.add(new Paragraph("Two-leg ties are decided by points across both matches, then goal difference "
                + "with away goals counting double, then penalties. Both legs must be completed.").setFontSize(9));
        for (BracketNode node : report.getNodes()) addDetails(document, node);
        if (!report.getUnassignedMatches().isEmpty()) {
            document.add(new Paragraph("Registered knockout matches awaiting confirmed bracket placement")
                    .simulateBold().setFontSize(11).setKeepWithNext(true));
            for (BracketReport.MatchResult match : report.getUnassignedMatches())
                document.add(new Paragraph(match.description()).setFontSize(9));
        }
    }

    private static void addDetails(Document document, BracketNode node) {
        Table details = new Table(UnitValue.createPercentArray(new float[]{19, 81})).useAllAvailableWidth()
                .setMarginBottom(10).setKeepTogether(true);
        details.addCell(new Cell(1, 2).add(new Paragraph(node.round() + " " + node.id() + " | "
                        + node.homeSource() + " vs " + node.awaySource()).simulateBold().setFontSize(10))
                .setBackgroundColor(PALE_BLUE));
        row(details, "Teams", BracketReport.name(node.homeTeam()) + " vs " + BracketReport.name(node.awayTeam()));
        if (node.round().equals("Final")) row(details, "Final", BracketReport.describe(node.finalMatch()));
        else {
            row(details, "First leg", BracketReport.describe(node.firstLeg()));
            row(details, "Second leg", BracketReport.describe(node.secondLeg()));
        }
        row(details, "Status", node.status());
        row(details, "Winner", BracketReport.name(node.winner()));
        if (!node.resolution().isBlank()) row(details, "Decision", node.resolution());
        document.add(details);
    }

    private static void row(Table table, String title, String value) {
        table.addCell(cell(title).simulateBold());
        table.addCell(cell(value));
    }

    private static Cell header(String value) {
        return cell(value).setBackgroundColor(BLUE).setFontColor(ColorConstants.WHITE).simulateBold();
    }

    private static Cell cell(String value) {
        return new Cell().add(new Paragraph(value).setMargin(0).setFontSize(8)).setPadding(4);
    }

    private static final class BracketRenderer extends DivRenderer {
        private final BracketReport report;

        private BracketRenderer(Div element, BracketReport report) {
            super(element);
            this.report = report;
        }

        @Override
        public IRenderer getNextRenderer() { return new BracketRenderer((Div) modelElement, report); }

        @Override
        public void draw(DrawContext context) {
            super.draw(context);
            Rectangle area = getOccupiedAreaBBox();
            float gap = 22;
            float width = (area.getWidth() - gap * 2) / 3;
            float height = 99;
            float left = area.getLeft();
            float top = area.getTop() - 15;
            float[] centres = {top - 48, top - 155, top - 262, top - 369,
                    top - 101.5f, top - 315.5f, top - 208.5f};
            PdfCanvas graphics = context.getCanvas();
            graphics.saveState().setStrokeColor(BLUE).setLineWidth(0.8f);
            connect(graphics, left + width, left + width + gap, centres[0], centres[1], centres[4]);
            connect(graphics, left + width, left + width + gap, centres[2], centres[3], centres[5]);
            connect(graphics, left + 2 * width + gap, left + 2 * (width + gap), centres[4], centres[5], centres[6]);
            graphics.restoreState();
            for (int i = 0; i < 7; i++) {
                int column = i < 4 ? 0 : i < 6 ? 1 : 2;
                Rectangle box = new Rectangle(left + column * (width + gap), centres[i] - height / 2, width, height);
                drawBox(context, box, report.getNodes().get(i));
            }
        }

        private static void connect(PdfCanvas canvas, float start, float end, float first, float second, float target) {
            float middle = (start + end) / 2;
            canvas.moveTo(start, first).lineTo(middle, first).lineTo(middle, second).lineTo(start, second).stroke();
            canvas.moveTo(middle, target).lineTo(end, target).stroke();
            canvas.moveTo(end - 4, target + 3).lineTo(end, target).lineTo(end - 4, target - 3).stroke();
        }

        private static void drawBox(DrawContext context, Rectangle box, BracketNode node) {
            context.getCanvas().saveState().setFillColor(PALE_BLUE).setStrokeColor(BLUE).setLineWidth(0.8f)
                    .rectangle(box).fillStroke().restoreState();
            Rectangle inside = new Rectangle(box.getX() + 5, box.getY() + 4, box.getWidth() - 10, box.getHeight() - 8);
            try (Canvas canvas = new Canvas(context.getCanvas(), inside)) {
                canvas.add(new Paragraph(node.round() + " " + node.id()).simulateBold().setFontColor(BLUE)
                        .setFontSize(8).setMargin(0).setMultipliedLeading(1.05f));
                canvas.add(new Paragraph(node.homeSource() + ": " + BracketReport.name(node.homeTeam())
                        + "\n" + node.awaySource() + ": " + BracketReport.name(node.awayTeam())
                        + "\n" + scores(node) + "\n" + node.status() + "\nWinner: " + BracketReport.name(node.winner()))
                        .setFontSize(7.3f).setMargin(0).setMultipliedLeading(1));
            }
        }

        private static String scores(BracketNode node) {
            if (node.round().equals("Final")) return "Score: " + score(node.finalMatch(), node);
            return "Leg 1: " + score(node.firstLeg(), node) + "; Leg 2: " + score(node.secondLeg(), node);
        }

        private static String score(BracketReport.MatchResult match, BracketNode node) {
            if (match == null || !match.played()) return "pending";
            boolean sameOrder = match.homeTeam() == node.homeTeam();
            String score = sameOrder ? match.homeGoals() + "-" + match.awayGoals()
                    : match.awayGoals() + "-" + match.homeGoals();
            if (match.homePenalties() != null && match.awayPenalties() != null) {
                score += sameOrder ? " (p " + match.homePenalties() + "-" + match.awayPenalties() + ")"
                        : " (p " + match.awayPenalties() + "-" + match.homePenalties() + ")";
            }
            return score;
        }
    }
}
