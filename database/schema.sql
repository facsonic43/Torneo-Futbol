-- Reconstructed from the queries in CityDAO and StadiumDAO.
-- Compare it with your actual database and adjust types/lengths if they differ.

CREATE TABLE IF NOT EXISTS ciudad (
    id_ciudad SERIAL PRIMARY KEY,
    nombre    VARCHAR(100) NOT NULL,
    pais      VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS estadio (
    id_estadio SERIAL PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    id_ciudad  INTEGER NOT NULL REFERENCES ciudad (id_ciudad)
);
