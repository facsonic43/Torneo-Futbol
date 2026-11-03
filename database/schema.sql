-- Tables used by CityDAO and StadiumDAO. IDs are generated automatically.

CREATE TABLE IF NOT EXISTS city (
    id_city SERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS stadium (
    id_stadium SERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    id_city    INTEGER NOT NULL REFERENCES city (id_city)
);
