package report;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import model.participant.Coach;
import model.participant.Person;
import model.participant.Player;
import model.participant.Referee;
import model.participant.Team;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/*
 * Genera el Reporte I solicitado por el trabajo práctico.
 * Crea en PDF una identificación para cada jugador, director técnico y árbitro,
 * incluyendo datos personales, rol, fotografía y un código de barras único.
 */
public class PdfIdentificationReportService {

    private PDType1Font regularFont =
            new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA
            );

    private PDType1Font boldFont =
            new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA_BOLD
            );

    // Genera un único PDF con las identificaciones de todos los participantes.
    public int generateIdentifications(
            List<Team> teams,
            List<Referee> referees,
            String outputFile) throws Exception {

        Path outputPath =
                Path.of(outputFile);

        if (outputPath.getParent() != null) {
            Files.createDirectories(
                    outputPath.getParent()
            );
        }

        int credentialsCreated = 0;

        try (PDDocument document =
                     new PDDocument()) {

            for (Team team : teams) {

                addCredential(
                        document,
                        team.getCoach(),
                        "COACH",
                        team
                );

                credentialsCreated++;

                for (Player player :
                        team.getSquad()) {

                    addCredential(
                            document,
                            player,
                            "PLAYER",
                            team
                    );

                    credentialsCreated++;
                }
            }

            for (Referee referee :
                    referees) {

                addCredential(
                        document,
                        referee,
                        "REFEREE",
                        null
                );

                credentialsCreated++;
            }

            document.save(
                    outputFile
            );
        }

        return credentialsCreated;
    }

    // Crea una página con la credencial correspondiente a una persona.
    private void addCredential(
            PDDocument document,
            Person person,
            String role,
            Team team) throws Exception {

        PDRectangle credentialSize =
                new PDRectangle(
                        420,
                        260
                );

        PDPage page =
                new PDPage(
                        credentialSize
                );

        document.addPage(page);

        try (PDPageContentStream content =
                     new PDPageContentStream(
                             document,
                             page
                     )) {

            drawBorder(
                    content,
                    credentialSize
            );

            writeText(
                    content,
                    "INTERNATIONAL CLUB CUP 2026",
                    boldFont,
                    15,
                    20,
                    235
            );

            writeText(
                    content,
                    "OFFICIAL IDENTIFICATION",
                    boldFont,
                    11,
                    20,
                    217
            );

            PDImageXObject photo =
                    createPhoto(
                            document,
                            person
                    );

            content.drawImage(
                    photo,
                    20,
                    68,
                    100,
                    135
            );

            float textX = 140;
            float textY = 199;
            float lineHeight = 15;

            writeText(
                    content,
                    "Name: "
                            + person.getName(),
                    boldFont,
                    10,
                    textX,
                    textY
            );

            textY -= lineHeight;

            writeText(
                    content,
                    "Role: "
                            + role,
                    regularFont,
                    10,
                    textX,
                    textY
            );

            textY -= lineHeight;

            if (person instanceof Player) {

                Player player =
                        (Player) person;

                writeText(
                        content,
                        "Position: "
                                + player.getPosition(),
                        regularFont,
                        10,
                        textX,
                        textY
                );

                textY -= lineHeight;
            }

            if (team != null) {

                writeText(
                        content,
                        "Team: "
                                + team.getName(),
                        regularFont,
                        10,
                        textX,
                        textY
                );

                textY -= lineHeight;
            }

            writeText(
                    content,
                    "Document: "
                            + person.getIdType()
                            + " "
                            + person.getIdNumber(),
                    regularFont,
                    10,
                    textX,
                    textY
            );

            textY -= lineHeight;

            writeText(
                    content,
                    "Birth date: "
                            + person.getBirthDate(),
                    regularFont,
                    10,
                    textX,
                    textY
            );

            textY -= lineHeight;

            writeText(
                    content,
                    "Age: "
                            + person.getAge(),
                    regularFont,
                    10,
                    textX,
                    textY
            );

            textY -= lineHeight;

            writeText(
                    content,
                    "Nationality: "
                            + person.getNationality()
                            .getName(),
                    regularFont,
                    10,
                    textX,
                    textY
            );

            textY -= lineHeight;

            if (person instanceof Coach) {

                Coach coach =
                        (Coach) person;

                writeText(
                        content,
                        "Titles: "
                                + coach.getTitlesObtained(),
                        regularFont,
                        10,
                        textX,
                        textY
                );
            }

            if (person instanceof Referee) {

                Referee referee =
                        (Referee) person;

                writeText(
                        content,
                        "Years officiating: "
                                + referee.getYearsOfficiated(),
                        regularFont,
                        10,
                        textX,
                        textY
                );
            }

            String barcodeValue =
                    createBarcodeValue(
                            person,
                            role
                    );

            PDImageXObject barcode =
                    createBarcode(
                            document,
                            barcodeValue
                    );

            content.drawImage(
                    barcode,
                    140,
                    28,
                    240,
                    43
            );

            writeText(
                    content,
                    barcodeValue,
                    regularFont,
                    7,
                    140,
                    17
            );
        }
    }

    private void drawBorder(
            PDPageContentStream content,
            PDRectangle pageSize)
            throws Exception {

        content.setLineWidth(
                1.5f
        );

        content.addRect(
                8,
                8,
                pageSize.getWidth() - 16,
                pageSize.getHeight() - 16
        );

        content.stroke();
    }

    private void writeText(
            PDPageContentStream content,
            String text,
            PDType1Font font,
            float fontSize,
            float x,
            float y) throws Exception {

        content.beginText();

        content.setFont(
                font,
                fontSize
        );

        content.newLineAtOffset(
                x,
                y
        );

        content.showText(
                safeText(
                        text,
                        font
                )
        );

        content.endText();
    }

    // Reemplaza caracteres que la fuente estándar del PDF no pueda representar.
    private String safeText(
            String text,
            PDType1Font font) {

        if (text == null) {
            return "";
        }

        StringBuilder result =
                new StringBuilder();

        for (int i = 0;
             i < text.length();
             i++) {

            String character =
                    String.valueOf(
                            text.charAt(i)
                    );

            try {
                font.encode(
                        character
                );

                result.append(
                        character
                );

            } catch (Exception exception) {

                result.append(
                        "?"
                );
            }
        }

        return result.toString();
    }

    // Busca la fotografía de la persona dentro de resources/photos.
    private PDImageXObject createPhoto(
            PDDocument document,
            Person person) throws Exception {

        String fileName =
                person.getIdType()
                        + "-"
                        + person.getIdNumber();

        String[] extensions = {
                ".jpg",
                ".jpeg",
                ".png"
        };

        for (String extension :
                extensions) {

            String resource =
                    "photos/"
                            + fileName
                            + extension;

            try (InputStream inputStream =
                         getClass()
                                 .getClassLoader()
                                 .getResourceAsStream(
                                         resource
                                 )) {

                if (inputStream != null) {

                    byte[] imageBytes =
                            inputStream.readAllBytes();

                    return PDImageXObject
                            .createFromByteArray(
                                    document,
                                    imageBytes,
                                    fileName
                            );
                }
            }
        }

        return createPlaceholderPhoto(
                document,
                person
        );
    }

    // Si todavía no existe una foto real, genera una imagen temporal con las iniciales.
    private PDImageXObject createPlaceholderPhoto(
            PDDocument document,
            Person person) throws Exception {

        int width = 300;
        int height = 400;

        BufferedImage image =
                new BufferedImage(
                        width,
                        height,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                image.createGraphics();

        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        graphics.setColor(
                java.awt.Color.LIGHT_GRAY
        );

        graphics.fillRect(
                0,
                0,
                width,
                height
        );

        graphics.setColor(
                java.awt.Color.DARK_GRAY
        );

        graphics.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        72
                )
        );

        String initials =
                getInitials(
                        person.getName()
                );

        int textWidth =
                graphics.getFontMetrics()
                        .stringWidth(
                                initials
                        );

        graphics.drawString(
                initials,
                (width - textWidth) / 2,
                220
        );

        graphics.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        20
                )
        );

        String message =
                "PHOTO NOT AVAILABLE";

        int messageWidth =
                graphics.getFontMetrics()
                        .stringWidth(
                                message
                        );

        graphics.drawString(
                message,
                (width - messageWidth) / 2,
                280
        );

        graphics.dispose();

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        ImageIO.write(
                image,
                "png",
                output
        );

        return PDImageXObject
                .createFromByteArray(
                        document,
                        output.toByteArray(),
                        "placeholder"
                );
    }

    private String getInitials(
            String name) {

        if (name == null
                || name.isBlank()) {

            return "?";
        }

        String[] words =
                name.trim()
                        .split("\\s+");

        StringBuilder initials =
                new StringBuilder();

        for (String word : words) {

            if (!word.isEmpty()) {

                initials.append(
                        Character.toUpperCase(
                                word.charAt(0)
                        )
                );
            }

            if (initials.length()
                    == 2) {

                break;
            }
        }

        return initials.toString();
    }

    // Genera un código CODE_128 que luego puede ser leído en puntos de control.
    private PDImageXObject createBarcode(
            PDDocument document,
            String value) throws Exception {

        BitMatrix matrix =
                new MultiFormatWriter()
                        .encode(
                                value,
                                BarcodeFormat.CODE_128,
                                700,
                                120
                        );

        BufferedImage barcodeImage =
                MatrixToImageWriter
                        .toBufferedImage(
                                matrix
                        );

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        ImageIO.write(
                barcodeImage,
                "png",
                output
        );

        return PDImageXObject
                .createFromByteArray(
                        document,
                        output.toByteArray(),
                        "barcode"
                );
    }

    private String createBarcodeValue(
            Person person,
            String role) {

        return "TP2026"
                + "|"
                + role
                + "|"
                + person.getIdType()
                + "|"
                + person.getIdNumber();
    }
}