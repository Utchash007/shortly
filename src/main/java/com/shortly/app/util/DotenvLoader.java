package com.shortly.app.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Minimal {@code .env} file loader, applied before the Spring context starts.
 *
 * <p>Spring Boot does not read {@code .env} files on its own, so a file
 * mounted as a secret (Render Secret Files, Docker secrets) would otherwise
 * be ignored. Entries become JVM system properties, which
 * {@code application.yml} placeholders resolve.
 *
 * <p>Precedence is dashboard-first: a real OS environment variable or an
 * explicit {@code -D} property always wins over the file; the file wins over
 * {@code application.yml} defaults. Lookup order is {@code DOTENV_PATH} when
 * set, then {@code .env} in the working directory, then
 * {@code /etc/secrets/.env}.
 *
 * <p>Two legacy shorthands are translated to canonical variables:
 * {@code jdbc} (full JDBC URL with embedded credentials) and
 * {@code redis_conn} ({@code redis://user:password@host:port}).
 */
public final class DotenvLoader {

    /**
     * Environment variable pointing at the file to load.
     */
    public static final String DOTENV_PATH = "DOTENV_PATH";

    /**
     * Conventional secret-mount location on hosting platforms.
     */
    static final String SECRET_MOUNT_FILE = "/etc/secrets/.env";

    private DotenvLoader() {
    }

    /**
     * Loads the resolved file, if any exists.
     */
    public static void load() {
        String configured = System.getenv(DOTENV_PATH);
        if (configured != null && !configured.isBlank()) {
            load(Path.of(configured));
            return;
        }
        Path local = Path.of(".env");
        if (Files.isRegularFile(local)) {
            load(local);
            return;
        }
        Path mounted = Path.of(SECRET_MOUNT_FILE);
        if (Files.isRegularFile(mounted)) {
            load(mounted);
        }
    }

    /**
     * Loads one file, applying entries that are not already defined.
     *
     * <p>A missing file is normal (dashboard-only configuration) and ignored.
     *
     * @param path the file to load
     */
    static void load(Path path) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            return;
        }
        int applied = 0;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = unquote(line.substring(separator + 1).trim());
            if (key.isEmpty()) {
                continue;
            }
            if ("jdbc".equals(key)) {
                if (applyJdbc(value)) {
                    applied++;
                }
            } else if ("redis_conn".equals(key)) {
                if (applyRedis(value)) {
                    applied++;
                }
            } else if (setIfAbsent(key, value)) {
                applied++;
            }
        }
        if (applied > 0) {
            System.out.println("[shortly] applied " + applied + " entries from " + path);
        }
    }

    /**
     * Translates a legacy {@code jdbc} shorthand into datasource variables.
     *
     * @param jdbc full JDBC URL, credentials may be embedded as query parameters
     * @return true when the URL variable was applied
     */
    private static boolean applyJdbc(String jdbc) {
        String httpish = jdbc.replaceFirst("^jdbc:postgresql://", "http://");
        int queryAt = httpish.indexOf('?');
        String base = queryAt < 0 ? httpish : httpish.substring(0, queryAt);
        String query = queryAt < 0 ? "" : httpish.substring(queryAt + 1);
        boolean applied = false;
        try {
            java.net.URI uri = java.net.URI.create(base);
            applied = setIfAbsent("SPRING_DATASOURCE_URL",
                    "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath());
        } catch (IllegalArgumentException e) {
            return false;
        }
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && "user".equals(parts[0])) {
                setIfAbsent("SPRING_DATASOURCE_USERNAME", parts[1]);
            }
            if (parts.length == 2 && "password".equals(parts[0])) {
                setIfAbsent("SPRING_DATASOURCE_PASSWORD", parts[1]);
            }
        }
        return applied;
    }

    /**
     * Translates a legacy {@code redis_conn} shorthand into Redis variables.
     *
     * @param url {@code redis://user:password@host:port} value
     * @return true when the host variable was applied
     */
    private static boolean applyRedis(String url) {
        try {
            String noScheme = url.substring(url.indexOf("://") + 3);
            String userInfo = noScheme.substring(0, noScheme.indexOf('@'));
            String hostPort = noScheme.substring(noScheme.indexOf('@') + 1);
            String password = userInfo.substring(userInfo.indexOf(':') + 1);
            String host = hostPort.substring(0, hostPort.lastIndexOf(':'));
            String port = hostPort.substring(hostPort.lastIndexOf(':') + 1);
            boolean applied = setIfAbsent("SPRING_DATA_REDIS_HOST", host);
            setIfAbsent("SPRING_DATA_REDIS_PORT", port);
            setIfAbsent("SPRING_DATA_REDIS_PASSWORD", password);
            return applied;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    /**
     * Sets a system property unless the environment or command line wins.
     *
     * @param key property name
     * @param value property value
     * @return true when the property was set from the file
     */
    private static boolean setIfAbsent(String key, String value) {
        if (System.getenv(key) != null || System.getProperty(key) != null) {
            return false;
        }
        System.setProperty(key, value);
        return true;
    }

    /**
     * Strips one layer of surrounding single or double quotes.
     *
     * @param value the raw value
     * @return the unquoted value
     */
    private static String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
