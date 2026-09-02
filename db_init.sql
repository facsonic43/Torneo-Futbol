CREATE TABLE ciudad (
        id_ciudad   SERIAL PRIMARY KEY,
        nombre      VARCHAR(100) NOT NULL,
        pais        VARCHAR(100) NOT NULL,
        UNIQUE (nombre, pais)
        );

        CREATE TABLE estadio (
        id_estadio  SERIAL PRIMARY KEY,
        nombre      VARCHAR(150) NOT NULL,
        id_ciudad   INTEGER NOT NULL,
        CONSTRAINT fk_estadio_ciudad
        FOREIGN KEY (id_ciudad)
        REFERENCES ciudad (id_ciudad)
        ON DELETE RESTRICT
        );

        INSERT INTO ciudad (nombre, pais) VALUES
        ('Madrid', 'España'),
        ('Roma', 'Italia'),
        ('Múnich', 'Alemania'),
        ('Londres', 'Reino Unido'),
        ('París', 'Francia'),
        ('Yakarta', 'Indonesia'),
        ('Tokio', 'Japón'),
        ('Abuya', 'Nigeria'),
        ('Río de Janeiro', 'Brasil'),
        ('Buenos Aires', 'Argentina'),
        ('Moscú', 'Rusia'),
        ('Estambul', 'Turquía');

        INSERT INTO estadio (nombre, id_ciudad) VALUES
        ('Santiago Bernabéu', (SELECT id_ciudad FROM ciudad WHERE nombre = 'Madrid')),
        ('Metropolitano', (SELECT id_ciudad FROM ciudad WHERE nombre = 'Madrid')),
        ('Stadio Olimpico', (SELECT id_ciudad FROM ciudad WHERE nombre = 'Roma')),
        ('Stadio Flaminio', (SELECT id_ciudad FROM ciudad WHERE nombre = 'Roma')),
        ('Allianz Arena', (SELECT id_ciudad FROM ciudad WHERE nombre = 'Múnich')),
        ('Wembley Stadium', (SELECT id_ciudad FROM ciudad WHERE nombre = 'Londres')),
        ('Emirates Stadium', (SELECT id_ciudad FROM ciudad WHERE nombre = 'Londres')),
        ('Stade de France', (SELECT id_ciudad FROM ciudad WHERE nombre = 'París')),
        ('Gelora Bung Karno', (SELECT id_ciudad FROM ciudad WHERE nombre = 'Yakarta')),
        ('Japan National Stadium', (SELECT id_ciudad FROM ciudad WHERE nombre = 'Tokio')),
        ('Moshood Abiola National Stadium', (SELECT id_ciudad FROM ciudad WHERE nombre = 'Abuya')),
        ('Maracanã', (SELECT id_ciudad FROM ciudad WHERE nombre = 'Río de Janeiro')),
        ('Monumental', (SELECT id_ciudad FROM ciudad WHERE nombre = 'Buenos Aires')),
        ('La Bombonera', (SELECT id_ciudad FROM ciudad WHERE nombre = 'Buenos Aires')),
        ('Luzhniki Stadium', (SELECT id_ciudad FROM ciudad WHERE nombre = 'Moscú')),
        ('Atatürk Olympic Stadium', (SELECT id_ciudad FROM ciudad WHERE nombre = 'Estambul'));