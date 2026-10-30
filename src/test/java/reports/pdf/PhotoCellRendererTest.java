package reports.pdf;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.Matrix;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.*;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PhotoCellRendererTest {
    @TempDir Path directory;

    @Test
    void photosCoverTheEntireCellEvenWhenAdjacentTextMakesItTaller() throws Exception {
        for (String photo : List.of("Coaches/DU-10000019.jpg", "Players/DU-10000001.jpg")) {
            PdfImageXObject image = new PdfImageXObject(ImageDataFactory.create(
                    Files.readAllBytes(Path.of("Images/Person").resolve(photo))));
            Cell cell = new Cell().setPadding(0).setBorder(Border.NO_BORDER).setMinHeight(78);
            var renderer = new IdentificationPdfSection.PhotoCellRenderer(cell, image);
            cell.setNextRenderer(renderer);
            Path output = directory.resolve(Path.of(photo).getFileName() + ".pdf");
            Rectangle frame;
            try (Document document = new Document(new PdfDocument(new PdfWriter(output.toString())))) {
                Table table = new Table(new float[]{1, 1}).setWidth(200).setFixedLayout();
                table.addCell(cell);
                table.addCell(new Cell().setMinHeight(140).add(new Paragraph("Participant details")));
                document.add(table);
                frame = renderer.getOccupiedAreaBBox().clone();
            }
            assertTrue(frame.getWidth() > 64);
            assertTrue(frame.getHeight() > 78);
            try (PdfDocument pdf = new PdfDocument(new PdfReader(output.toString()))) {
                ImageGeometry geometry = new ImageGeometry();
                new PdfCanvasProcessor(geometry).processPageContent(pdf.getPage(1));
                assertNotNull(geometry.image);
                assertTrue(geometry.clipAtImage.equalsWithEpsilon(frame, 0.02f), "Clip must match the whole photo cell");
                assertTrue(geometry.image.getLeft() <= frame.getLeft() + 0.02f);
                assertTrue(geometry.image.getRight() >= frame.getRight() - 0.02f);
                assertTrue(geometry.image.getBottom() <= frame.getBottom() + 0.02f);
                assertTrue(geometry.image.getTop() >= frame.getTop() - 0.02f);
                assertEquals(image.getWidth() / image.getHeight(),
                        geometry.image.getWidth() / geometry.image.getHeight(), 0.001f);
            }
        }
    }

    private static class ImageGeometry implements IEventListener {
        Rectangle clip;
        Rectangle clipAtImage;
        Rectangle image;

        @Override
        public void eventOccurred(IEventData data, EventType type) {
            if (type == EventType.CLIP_PATH_CHANGED) {
                clip = Rectangle.calculateBBox(((ClippingPathInfo) data).getClippingPath().getSubpaths().stream()
                        .flatMap(path -> path.getPiecewiseLinearApproximation().stream()).toList());
            } else if (type == EventType.RENDER_IMAGE) {
                Matrix matrix = ((ImageRenderInfo) data).getImageCtm();
                image = new Rectangle(matrix.get(Matrix.I31), matrix.get(Matrix.I32),
                        matrix.get(Matrix.I11), matrix.get(Matrix.I22));
                clipAtImage = clip;
            }
        }

        @Override
        public Set<EventType> getSupportedEvents() {
            return Set.of(EventType.CLIP_PATH_CHANGED, EventType.RENDER_IMAGE);
        }
    }
}
