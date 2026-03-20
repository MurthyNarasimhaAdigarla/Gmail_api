package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigReader {
    private static final String DEFAULT_PROPERTIES = "config.properties";
    private static final Properties properties = new Properties();

    static {
        // Load from classpath first (src/main/resources or src/test/resources on test runs)
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES)) {
            if (in != null) {
                properties.load(in);
            } else {
                // If not on classpath, try working-directory path as a fallback
                // (useful when running from IDE or tests that place resources elsewhere)
                try (InputStream fis = new java.io.FileInputStream("src/test/resources/" + DEFAULT_PROPERTIES)) {
                    properties.load(fis);
                } catch (IOException e) {
                    // No properties found; leave properties empty and fail later with clear message
                }
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load configuration: " + e.getMessage());
        }
    }

    private ConfigReader() { /* utility */ }

    public static String getNullable(String key) {
        return properties.getProperty(key);
    }

    public static String get(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing required configuration property: " + key);
        }
        return value;
    }

    public static String getOrDefault(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}