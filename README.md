# Football Championship

Java 25 / Maven / JavaFX project for the Programación B (UFASTA) 2026 team assignment:
a management and simulation system for the *Copa Internacional de Clubes*.

## Requirements

- JDK 25 and Maven
- PostgreSQL (only cities and stadiums are stored there)

## Setup

1. **Database.** Create a PostgreSQL database named `copa_clubes` and run
   `database/schema.sql` (tables `ciudad` and `estadio`). The knockout stage needs at
   least 13 stadiums, so load that many from the application's *Database* screen or with SQL.
2. **Credentials.** Create `src/main/resources/database.properties` (it is git-ignored):

   ```properties
   db.url=jdbc:postgresql://localhost:5432/copa_clubes
   db.user=postgres
   db.password=your-password
   ```
3. **Photos.** Put participant photos (`.jpg`, `.jpeg`, `.png`) in `Images/Person/Coaches`,
   `Images/Person/Players`, or `Images/Person/Referees`, according to their role,
   relative to the directory the application runs from. Name each photo
   `<idType>-<idNumber>.<extension>` using the participant's document, for example
   `Images/Person/Players/DU-10000001.jpg`. Every PDF export looks up that exact document
   in the participant's role folder; photo assignment stays the same across exports.
   File extensions are case-insensitive. Keep one photo per document, and replace that
   file to change a participant's photo. Participants without a matching file show
   *Photo unavailable*; an unreadable matching image aborts the export.

   The 323 photos currently show distinct footballers selected from historical and
   current football icons. The tournament's participant names are fictional and do
   not identify the footballers pictured. The document-based filenames stay unchanged.
   `Images/Person/photo-assignments.csv` records the footballer shown in each file,
   its source, author, declared license, and image hash. The PDF finds photos by their
   filenames and does not depend on that CSV.

   Each photograph has its own declared license (CC BY, CC BY-SA, CC0, or public
   domain). Author credits, source links, license links, and the PDF display-cropping
   notice are recorded in `Images/Person/PHOTO-CREDITS.md`. Keep those credits with
   the images. When that file exists, the exporter also embeds it as an attachment
   in the PDF without adding visible pages.

   The current images come from Wikimedia Commons. Source details are documented in
   `Images/Person/PHOTO-SOURCES.md`.

## Run

- Application: `mvn javafx:run` (main class `run.RunGui`).
- Console simulation: `mvn exec:java` (`run.RunChampionship`, in the test sources). It plays
  the whole championship printing standings and match details and writes `Report.pdf`.
- Tests: `mvn test`.

## How it works

The team data (clubs, players, coaches, referees) is read from `src/main/resources/torneo.json`.
**New game** creates the draw and the group fixtures; **Continue** restores the tournament saved
in `saves/tournament.dat` (Java serialization), and the **Save** button writes it. Saves made
with an older version of the classes may not load: start a new game in that case.

| Package | Responsibility |
|---|---|
| `gui.controller`, `resources/view` | JavaFX screens and their controllers |
| `gui`, `gui.report` | Reusable screen parts: bracket (`BracketSection`, `BracketView`), table cells, and one `ReportView` subclass per report |
| `service` | Tournament session, match execution |
| `model.tournament`, `model.match`, `model.participant` | Domain: tournament, groups, matches, events, teams, people |
| `control` | Match, event and penalty shootout simulators |
| `main.loader`, `persistence` | JSON loading and save/restore |
| `dao`, `db` | PostgreSQL access (cities and stadiums) |
| `reports`, `reports.pdf` | Statistics and the participant identification PDF |

Team strength (`Team.getTeamPower`) combines the squad's average overall, the club's prestige,
the coach's titles and the ranking; the simulators add randomness so upsets can happen.

## Reports (assignment section E)

Open **Reports** in the application:

| Entry | Assignment item |
|---|---|
| Generate PDF | I. Identification cards (data, role, photo, Code 128 barcode) |
| Top Scorers | II. Scorers, with how many goals were penalties |
| Fair Play | III. Fair play ranking by team |
| Most Minutes | IV. Players with most minutes and matches played |
| Championship Bracket | V. Bracket up to the champion |
| Teams | VI. Teams in alphabetical order: average player age, coach age and nationality, goals for/against, effectiveness |
| Referees | VII. Referees by matches directed, years of experience and average |
| Players | VIII. Players by position (or all), goalkeepers with goals conceded and average |

*Matches Summary* and *Coaches* are extra views. Reports are calculated from the matches
played so far. Fair play uses `yellow cards + 3 * red cards` (fewer is better), because the
assignment does not define a weighting. Team effectiveness is
`100 * points / (3 * matches played)`.
