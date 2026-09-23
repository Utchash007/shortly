package com.shortly.app.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for random short-code generation.
 */
class ShortCodeGeneratorTest {

    private final ShortCodeGenerator generator = new ShortCodeGenerator();

    /**
     * Verifies the default code length.
     */
    @Test
    void generate_usesDefaultLengthOfSeven() {
        assertEquals(ShortCodeGenerator.DEFAULT_LENGTH, generator.generate().length());
        assertEquals(7, generator.generate().length());
    }

    /**
     * Verifies custom lengths are honoured.
     */
    @Test
    void generate_honoursRequestedLength() {
        assertEquals(6, generator.generate(6).length());
        assertEquals(8, generator.generate(8).length());
    }

    /**
     * Verifies only Base62 characters are emitted.
     */
    @Test
    void generate_emitsOnlyBase62Characters() {
        for (int i = 0; i < 100; i++) {
            assertTrue(generator.generate().matches("[A-Za-z0-9]+"));
        }
    }

    /**
     * Verifies uniqueness across a large batch.
     *
     * <p>With a 62^7 space, a collision here indicates a broken RNG rather
     * than acceptable randomness.
     */
    @Test
    void generate_uniqueAcrossTenThousand() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            seen.add(generator.generate());
        }
        assertEquals(10_000, seen.size());
    }

    /**
     * Verifies non-positive lengths are rejected.
     */
    @Test
    void generate_rejectsNonPositiveLength() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(0));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(-1));
    }
}
