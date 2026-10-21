CREATE TABLE artists (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    musical_genre VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_artists_name UNIQUE (name),
    CONSTRAINT ck_artists_name_not_blank CHECK (CHAR_LENGTH(TRIM(name)) > 0)
) ENGINE = InnoDB;

CREATE TABLE venues (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    capacity INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_venues_name_city UNIQUE (name, city),
    CONSTRAINT ck_venues_name_not_blank CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_venues_city_not_blank CHECK (CHAR_LENGTH(TRIM(city)) > 0),
    CONSTRAINT ck_venues_capacity CHECK (capacity BETWEEN 50 AND 50000)
) ENGINE = InnoDB;

CREATE TABLE concerts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    artist_id BIGINT NOT NULL,
    venue_id BIGINT NOT NULL,
    date_time DATETIME(6) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_concerts_artist FOREIGN KEY (artist_id) REFERENCES artists (id),
    CONSTRAINT fk_concerts_venue FOREIGN KEY (venue_id) REFERENCES venues (id),
    CONSTRAINT ck_concerts_title_not_blank CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT ck_concerts_unit_price CHECK (unit_price > 0),
    INDEX idx_concerts_artist_id (artist_id),
    INDEX idx_concerts_venue_id (venue_id)
) ENGINE = InnoDB;
