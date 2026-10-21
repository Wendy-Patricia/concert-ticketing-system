package com.example.concertticketingsystem.entity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ArtistTests {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void rejectsMissingOrBlankName(String name) {
        assertThat(validator.validate(new Artist(name, "Jazz")))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("name");
    }

    @Test
    void allowsArtistWithoutMusicalGenre() {
        assertThat(validator.validate(new Artist("Nina Simone", null))).isEmpty();
    }

    @Test
    void acceptsValuesAtColumnLengthLimit() {
        assertThat(validator.validate(new Artist("a".repeat(255), "b".repeat(255)))).isEmpty();
    }

    @Test
    void rejectsValuesExceedingColumnLengths() {
        assertThat(validator.validate(new Artist("a".repeat(256), "b".repeat(256))))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("name", "musicalGenre");
    }
}
