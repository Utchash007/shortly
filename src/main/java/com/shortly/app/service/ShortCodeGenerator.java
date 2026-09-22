package com.shortly.app.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Generates random Base62 short codes.
 *
 * <p>Stateless and thread-safe: the underlying {@link SecureRandom} may be
 * shared across threads. Uniqueness against concurrent inserts is enforced by
 * the database {@code UNIQUE} constraint, not by this generator.
 */
@Service
public class ShortCodeGenerator {

    /**
     * Base62 alphabet used for short codes.
     */
    public static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * Default code length, inside the supported 6–8 range.
     */
    public static final int DEFAULT_LENGTH = 7;

    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a code of the default length.
     *
     * @return a random Base62 string of {@link #DEFAULT_LENGTH} characters
     */
    public String generate() {
        return generate(DEFAULT_LENGTH);
    }

    /**
     * Generates a code of the requested length.
     *
     * @param length the number of characters, must be positive
     * @return a random Base62 string of the requested length
     * @throws IllegalArgumentException when length is not positive
     */
    public String generate(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return result.toString();
    }
}
