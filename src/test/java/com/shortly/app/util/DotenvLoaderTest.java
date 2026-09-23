package com.shortly.app.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for file-based environment loading.
 *
 * <p>Every system property a test sets is cleared afterwards so the shared
 * test JVM stays clean for Spring slice tests.
 */
class DotenvLoaderTest {

    @TempDir
    private Path temp;

    private static final List<String> KEYS = List.of(
            "SHORTLY_TEST_ALPHA", "SHORTLY_TEST_BETA",
            "SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD",
            "SPRING_DATA_REDIS_HOST", "SPRING_DATA_REDIS_PORT", "SPRING_DATA_REDIS_PASSWORD");

    /**
     * Clears properties possibly set by a test.
     */
    @AfterEach
    void clearProperties() {
        KEYS.forEach(System::clearProperty);
    }

    /**
     * Verifies plain entries load while comments and blanks are skipped.
     *
     * @throws Exception on file failures
     */
    @Test
    void load_appliesEntriesAndSkipsNoise() throws Exception {
        Path file = temp.resolve(".env");
        Files.writeString(file, "# comment\n\nSHORTLY_TEST_ALPHA=hello\nSHORTLY_TEST_BETA=\"quoted\"\n");

        DotenvLoader.load(file);

        assertEquals("hello", System.getProperty("SHORTLY_TEST_ALPHA"));
        assertEquals("quoted", System.getProperty("SHORTLY_TEST_BETA"));
    }

    /**
     * Verifies the legacy shorthands translate to canonical variables.
     *
     * @throws Exception on file failures
     */
    @Test
    void load_translatesLegacyJdbcAndRedis() throws Exception {
        Path file = temp.resolve(".env");
        Files.writeString(file,
                "jdbc=jdbc:postgresql://db.example.com:5432/postgres?user=pguser&password=pgpass\n"
                        + "redis_conn=redis://default:redispass@redis.example.com:14792\n");

        DotenvLoader.load(file);

        assertEquals("jdbc:postgresql://db.example.com:5432/postgres",
                System.getProperty("SPRING_DATASOURCE_URL"));
        assertEquals("pguser", System.getProperty("SPRING_DATASOURCE_USERNAME"));
        assertEquals("pgpass", System.getProperty("SPRING_DATASOURCE_PASSWORD"));
        assertEquals("redis.example.com", System.getProperty("SPRING_DATA_REDIS_HOST"));
        assertEquals("14792", System.getProperty("SPRING_DATA_REDIS_PORT"));
        assertEquals("redispass", System.getProperty("SPRING_DATA_REDIS_PASSWORD"));
    }

    /**
     * Verifies a missing file is ignored without failure.
     */
    @Test
    void load_ignoresMissingFile() {
        DotenvLoader.load(temp.resolve("does-not-exist"));
        assertNull(System.getProperty("SHORTLY_TEST_ALPHA"));
    }
}
