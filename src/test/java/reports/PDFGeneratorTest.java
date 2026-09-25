package reports;

import PDF.PDFGenerator;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import main.loader.TournamentData;
import main.loader.TournamentDataLoader;
import model.match.*;
import model.participant.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PDFGeneratorTest {
    @TempDir Path directory;

    @Test
    void exportsAllEightSectionsAndAllCredentialsWhileFilteringOnlyPlayerProfiles() throws Exception {
        TournamentData participants = new TournamentDataLoader().load("torneo.json");
        ReportData data = ReportData.fromInitialData(participants);
        Player selected = data.getTeams().getFirst().getSquad().getFirst();
        Path output = directory.resolve("complete.pdf");
        PDFGenerator.generate(data, output, new ReportOptions(Position.GOALKEEPER, selected, directory));

        try (PdfDocument pdf = new PdfDocument(new PdfReader(output.toString()))) {
            String text = text(pdf);
            for (String heading : List.of("I. Participant identification cards", "II. Top scorers",
                    "III. Fair Play ranking", "IV. Most minutes played", "V. Championship bracket",
                    "VI. Team statistics", "VII. Referee ranking", "VIII. Player information")) {
                assertTrue(text.contains(heading), "Missing section: " + heading);
            }
            List<Person> people = new ArrayList<>(data.getReferees());
            data.getTeams().forEach(team -> { people.add(team.getCoach()); people.addAll(team.getSquad()); });
            assertEquals(323, people.size());
            for (Person person : people) {
                assertTrue(text.contains(person.getName()), "Missing person: " + person.getName());
                assertTrue(text.contains(person.getIdType() + "-" + person.getIdNumber()), "Missing barcode identifier");
            }
            assertTrue(countXObjects(pdf, PdfName.Form) >= people.size(), "Each card must have a vector barcode");
            String profiles = text.substring(text.lastIndexOf("VIII. Player information"));
            assertTrue(profiles.contains(selected.getName()));
            assertFalse(profiles.contains(data.getTeams().getFirst().getSquad().get(1).getName()));
            assertTrue(profiles.contains("Goals conceded per match"));
            assertTrue(profiles.contains("reflexes: 92"));
            assertTrue(text.contains("Not drawn"));
            assertTrue(text.contains("No goals have been scored"));
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                String pageText = PdfTextExtractor.getTextFromPage(pdf.getPage(page));
                assertFalse(pageText.replaceAll("Continental Cup \\| Reports \\| Page \\d+", "").isBlank(), "Blank page " + page);
            }
        }
    }

    @Test
    void embedsSuppliedPhotographAndReportsMissingOnes() throws Exception {
        ReportData data = fixture();
        Path photo = directory.resolve("DU-300.jpg");
        assertTrue(ImageIO.write(new BufferedImage(20, 30, BufferedImage.TYPE_INT_RGB), "jpg", photo.toFile()));
        Path output = directory.resolve("photos.pdf");
        PDFGenerator.generate(data, output, new ReportOptions(null, null, directory));
        try (PdfDocument pdf = new PdfDocument(new PdfReader(output.toString()))) {
            assertEquals(1, countXObjects(pdf, PdfName.Image));
            // Two-column extraction may interleave the photo label with adjacent personal details.
            assertTrue(text(pdf).contains("unavailable"));
        }
    }

    @Test
    void preservesExistingReportAndRemovesTemporaryOutputWhenPhotographIsInvalid() throws Exception {
        ReportData data = fixture();
        Path output = directory.resolve("report.pdf");
        PDFGenerator.generate(data, output, new ReportOptions(null, null, directory));
        byte[] original = Files.readAllBytes(output);
        Files.writeString(directory.resolve("DU-300.jpg"), "invalid image");
        assertThrows(IOException.class, () -> PDFGenerator.generate(data, output, new ReportOptions(null, null, directory)));
        assertArrayEquals(original, Files.readAllBytes(output));
        try (var files = Files.list(directory)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".championship-report-")));
        }
    }

    @Test
    void handlesEmptyTournamentAndMissingExperienceWithoutInventingAverages() throws Exception {
        Path output = directory.resolve("empty.pdf");
        PDFGenerator.generate(new ReportData(new TournamentData(List.of(), List.of())), output,
                new ReportOptions(null, null, directory));
        try (PdfDocument pdf = new PdfDocument(new PdfReader(output.toString()))) {
            String text = text(pdf);
            assertTrue(text.contains("No participants registered"));
            assertTrue(text.contains("No players match"));
            assertTrue(text.contains("Average refereeing experience: Not available"));
        }
    }

    @Test
    void exposesMissingMatchDetailsAndRejectsAnUnregisteredSelectedPlayer() throws Exception {
        ReportData data = fixture();
        Team home = data.getTeams().getFirst();
        Team away = data.getTeams().get(1);
        Match match = new GroupMatch(home, away, data.getReferees().getFirst(), null, LocalDate.of(2026, 1, 1));
        match.addEvent(new Goal(20, home, home.getSquad().getFirst(), null));
        match.setHomeGoals(1);
        match.setPlayed(true);
        data.addMatch(match);
        Path output = directory.resolve("incomplete.pdf");
        PDFGenerator.generate(data, output, new ReportOptions(null, null, directory));
        try (PdfDocument pdf = new PdfDocument(new PdfReader(output.toString()))) {
            String text = text(pdf);
            assertTrue(text.contains("Additional report details are missing for 1 completed matches"));
            assertTrue(text.contains("Not available: starting lineups were not supplied for one or more completed matches"));
            String profiles = text.substring(text.lastIndexOf("VIII. Player information"));
            assertTrue(profiles.contains("Goals conceded: Not available | Goals conceded per match: Not available"));
            assertTrue(profiles.contains("Not available Not available 1 Not available Not available 0 0"));
        }
        Player stranger = keeper("Unknown", 999);
        assertThrows(IllegalArgumentException.class,
                () -> PDFGenerator.generate(data, output, new ReportOptions(null, stranger, directory)));
    }

    @Test
    void retainsLegitimateZeroesWhenMatchDetailsAreComplete() throws Exception {
        ReportData data = fixture();
        Team home = data.getTeams().getFirst();
        Team away = data.getTeams().get(1);
        Match match = new GroupMatch(home, away, data.getReferees().getFirst(), null, LocalDate.of(2026, 1, 1));
        data.getMatchDetails(match).setStartingPlayers(home.getSquad(), away.getSquad());
        match.setPlayed(true);
        data.addMatch(match);

        Path output = directory.resolve("complete-details.pdf");
        PDFGenerator.generate(data, output, new ReportOptions(null, null, directory));

        try (PdfDocument pdf = new PdfDocument(new PdfReader(output.toString()))) {
            String text = text(pdf);
            assertFalse(text.contains("Not available: starting lineups were not supplied for one or more completed matches"));
            assertTrue(text.contains("Goals conceded: 0 | Goals conceded per match: 0.00"));
            assertTrue(text.contains("Keeper One Alpha 1 90"));
        }
    }

    private static ReportData fixture() {
        Country country = new Country("Argentina");
        Coach coach = new Coach("Coach One", 101, "DU", LocalDate.of(1970, 1, 1), country, 3);
        Team first = new Team("Alpha", country, 1, coach, 1, 1, 1, null);
        first.addPlayer(keeper("Keeper One", 201));
        Team second = new Team("Beta", country, 2,
                new Coach("Coach Two", 102, "DU", LocalDate.of(1975, 1, 1), country, 1), 1, 1, 1, null);
        second.addPlayer(keeper("Keeper Two", 202));
        Referee referee = new Referee("Referee One", 300, "DU", LocalDate.of(1980, 1, 1), country, 99, 12);
        return new ReportData(new TournamentData(List.of(first, second), List.of(referee)));
    }

    private static Player keeper(String name, int id) {
        return new Goalkeeper(name, id, "DU", LocalDate.of(1995, 1, 1), new Country("Argentina"),
                0, 0, 0, 0, 0, 0, 60, 70, 80, 90, 80, 70);
    }

    private static String text(PdfDocument pdf) {
        StringBuilder text = new StringBuilder();
        for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
            text.append(PdfTextExtractor.getTextFromPage(pdf.getPage(page))).append('\n');
        }
        return text.toString().replaceAll("\\s+", " ");
    }

    private static int countXObjects(PdfDocument pdf, PdfName subtype) {
        Set<Integer> references = new HashSet<>();
        for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
            PdfDictionary objects = pdf.getPage(page).getResources().getPdfObject().getAsDictionary(PdfName.XObject);
            if (objects == null) continue;
            for (PdfName name : objects.keySet()) {
                PdfStream object = objects.getAsStream(name);
                if (object != null && subtype.equals(object.getAsName(PdfName.Subtype)))
                    references.add(object.getIndirectReference().getObjNumber());
            }
        }
        return references.size();
    }
}
