package reports.pdf;

import model.participant.Coach;
import model.participant.Country;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentPhotographsTest {
    @TempDir Path directory;

    @Test
    void findsOnlyThePhotographBelongingToEachDocumentAcrossRepeatedLookups() throws Exception {
        Coach first = coach("First", "DU", 101);
        Coach second = coach("Second", "DU", 102);
        Path firstPhoto = Files.createFile(directory.resolve("DU-101.jpg"));
        Path secondPhoto = Files.createFile(directory.resolve("DU-102.png"));

        for (int lookup = 0; lookup < 3; lookup++) {
            assertEquals(secondPhoto, IdentificationPdfSection.findPhotograph(second, directory));
            assertEquals(firstPhoto, IdentificationPdfSection.findPhotograph(first, directory));
        }
    }

    @Test
    void includesDocumentTypeSoEqualNumbersDoNotShareAPhotograph() throws Exception {
        Path du = Files.createFile(directory.resolve("DU-101.jpg"));
        Path passport = Files.createFile(directory.resolve("PAS-101.jpg"));

        assertEquals(du, IdentificationPdfSection.findPhotograph(coach("First", " du ", 101), directory));
        assertEquals(passport, IdentificationPdfSection.findPhotograph(coach("Second", "PAS", 101), directory));
    }

    @Test
    void supportsJpegAndPngWithMixedCaseExtensions() throws Exception {
        int number = 101;
        for (String extension : List.of(".jpg", ".jpeg", ".png", ".JpG", ".JPEG", ".PnG")) {
            Coach person = coach("Person", "DU", number++);
            Path photo = Files.createFile(directory.resolve(IdentificationPdfSection.identifier(person) + extension));
            assertEquals(photo, IdentificationPdfSection.findPhotograph(person, directory));
        }
    }

    @Test
    void doesNotUseAnotherParticipantsPhotoOrAnArbitraryFilenameAsAFallback() throws Exception {
        Files.createFile(directory.resolve("DU-102.jpg"));
        Files.createFile(directory.resolve("Max Verstappen.jpg"));
        Files.createFile(directory.resolve("unassigned-photo.jpg"));
        Files.createFile(directory.resolve("DU-101.txt"));
        Files.createDirectory(directory.resolve("DU-101.png"));

        assertNull(IdentificationPdfSection.findPhotograph(coach("Missing", "DU", 101), directory));
    }

    @Test
    void missingAndEmptyRoleFoldersLeaveTheParticipantWithoutAPhoto() throws Exception {
        Coach person = coach("Missing", "DU", 101);
        assertNull(IdentificationPdfSection.findPhotograph(person, directory));
        assertNull(IdentificationPdfSection.findPhotograph(person, directory.resolve("missing")));
    }

    @Test
    void doesNotSearchSiblingRoleFolders() throws Exception {
        Path coaches = Files.createDirectory(directory.resolve("Coaches"));
        Path players = Files.createDirectory(directory.resolve("Players"));
        Coach person = coach("Coach", "DU", 101);
        Files.createFile(players.resolve("DU-101.jpg"));

        assertNull(IdentificationPdfSection.findPhotograph(person, coaches));
    }

    private static Coach coach(String name, String idType, int idNumber) {
        return new Coach(name, idNumber, idType, LocalDate.of(1970, 1, 1), new Country("Argentina"), 0);
    }
}
