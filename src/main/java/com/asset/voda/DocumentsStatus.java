package com.asset.voda;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * A singleton class for tracking the status of document uploads and generating a JSON summary file.
 * This class manages counters for successful and failed uploads and saves a summary report in JSON format.
 */
public class DocumentsStatus {

    private static final DocumentsStatus instance = new DocumentsStatus();

    private static final Logger logger = LoggerFactory.getLogger(DocumentsStatus.class);
    private int uploadedDocuments = 0;  // Initial count for successfully uploaded documents
    private int failedDocuments = 0;    // Initial count for failed documents
    private String inProgressPath = PathsConfig.IN_PROGRESS;  // Directory where the JSON file will be saved

    private DocumentsStatus() {
    }

    // Get the singleton instance
    public static DocumentsStatus getInstance() {
        return instance;
    }

    // Method to generate the JSON summary file
    public void generateJsonSummary(String folderName) {
        try {
            // Create the properties JSON object
            JsonObject properties = new JsonObject();
            properties.addProperty("uploaded_documents", String.valueOf(uploadedDocuments));
            properties.addProperty("failed_documents", String.valueOf(failedDocuments));

            // Create the final JSON object with the folder name as key
            JsonObject jsonObject = new JsonObject();
            jsonObject.add(folderName, properties);  // Use folder name instead of "properties"

            // Convert to JSON string using Gson
            Gson gson = new Gson();
            String jsonString = gson.toJson(jsonObject);

            // Specify a unique filename using the folder name
            File jsonFile = new File(inProgressPath, "upload_summary.json");

            // Ensure the directory exists
            File dir = new File(inProgressPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Write the JSON string to the file
            try (FileWriter fileWriter = new FileWriter(jsonFile, true)) {
                fileWriter.write(jsonString);
                logger.info("Generated JSON summary and saved to: {}", jsonFile.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Error generating JSON summary and saving to file: {}", e.getMessage());
        }
    }

    // Methods to increment success/failure counters
    public void incrementSuccess() {
        uploadedDocuments++;
    }

    public void incrementFailure() {
        failedDocuments++;
    }
}
