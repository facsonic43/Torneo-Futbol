# Footballer photo sources

The 323 photos in `Players`, `Coaches`, and `Referees` show distinct footballers
selected from historical and current football icons. The tournament's participant
names are fictional: the photographed footballer does not identify
the participant named on the card or imply endorsement of this project.

Photos are sourced from [Wikimedia Commons](https://commons.wikimedia.org/).
Each image has its own declared license: CC BY, CC BY-SA, CC0, or public domain.
[PHOTO-CREDITS.md](PHOTO-CREDITS.md) records each source file and page, author,
attribution, license name, and license URL. Retain those credits when distributing
the images. The PDF exporter includes the credits file as an attachment when it
exists, without adding visible report pages.

Wikimedia thumbnail JPEGs are stored without further modification. The PDF scales
and center-crops their display to fill the photo cells. The individual image and
its displayed crop retain the license identified in the credits; the credit file
also records this display change and the applicable ShareAlike terms.

[photo-assignments.csv](photo-assignments.csv) maps each participant document to the
footballer pictured, local filename, source title and page, author, declared license,
license URL, and SHA-256 hash. The PDF locates photos directly by document filenames
such as `Players/DU-10000001.jpg`; it does not require the CSV.

To replace a photo manually, place the new image in the participant's role folder
using the same document-based filename. Keep its author, source, and license
information with the project, and update the credits when the source changes.
