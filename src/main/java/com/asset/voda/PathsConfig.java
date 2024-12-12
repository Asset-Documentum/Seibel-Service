package com.asset.voda;

import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for loading and storing application configurations.
 * This class initializes paths for file workflows and credentials for database connections
 * from a properties file (resources/config.properties).
 */

public class PathsConfig {
    private static final Logger logger = LoggerFactory.getLogger(PathsConfig.class);

    public static String TO_BE_PROCESSED;
    public static String IN_PROGRESS;
    public static String PROCESSED;
    public static String FAILED;

    public static String USERNAME;
    public static String PASS;

    public static String DB_URL;
    public static String USER;
    public static String PASSWORD;

    static {
        // Load paths from a properties file
        try (InputStream input = PathsConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.error("Unable to find config.properties file.");
                throw new RuntimeException("Config file is missing");
            }

            Properties prop = new Properties();
            prop.load(input);

            TO_BE_PROCESSED = prop.getProperty("to-be_processed");
            IN_PROGRESS = prop.getProperty("in_progress");
            PROCESSED = prop.getProperty("processed");
            FAILED = prop.getProperty("failed");

            USERNAME = prop.getProperty("username");
            PASS = prop.getProperty("pass");

            DB_URL = prop.getProperty("url");
            USER = prop.getProperty("user");
            PASSWORD = prop.getProperty("password");

        } catch (Exception e) {
            logger.error("Error loading configuration", e);
            throw new RuntimeException("Failed to load configuration", e);
        }
    }
}

