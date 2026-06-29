package com.lebarapp.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the default public-code generator: length, alphabet and a
 * sanity check that codes are not trivially repeated.
 */
class RandomPublicCodeGeneratorTest {

    private final RandomPublicCodeGenerator generator = new RandomPublicCodeGenerator();

    @Test
    void generatesCodeWithinSixToEightUppercaseAlphanumerics() {
        for (int i = 0; i < 1_000; i++) {
            String code = generator.generate();
            assertThat(code).hasSize(RandomPublicCodeGenerator.CODE_LENGTH);
            assertThat(code.length()).isBetween(6, 8);
            assertThat(code).matches("[A-Z0-9]+");
            assertThat(code.chars())
                    .allMatch(c -> RandomPublicCodeGenerator.ALPHABET.indexOf(c) >= 0);
        }
    }

    @Test
    void generatesMostlyDistinctCodes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            codes.add(generator.generate());
        }
        // With a 31^6 space, 1000 draws should be (near-)collision-free.
        assertThat(codes).hasSizeGreaterThan(990);
    }
}
