package com.asset.voda;

import java.io.File;
import java.nio.file.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches the folder designated for processing documents. It processes existing folders
 * that follow a specific date format (e.g., "DD-MM-YYYY") and handles the documents within.
 * It does not watch for new files, but rather processes folders that already exist.
 */
public class FolderWatcher {

    private static final Logger logger = LoggerFactory.getLogger(FolderWatcher.class);
    private static final DocumentUploaderManager uploaderManager = new DocumentUploaderManager();
    private final String TO_BE_PROCESSED = PathsConfig.TO_BE_PROCESSED;

    private MachineDetails machineDetails;

    /**
     * Checks whether the given folder name matches the expected date format (DD-MM-YYYY).
     *
     * @param folder The folder to check.
     * @return True if the folder name matches the date format; otherwise, false.
     */
    private boolean isValidDateFolder(File folder) {
        return folder.getName().matches("\\d{2}-\\d{2}-\\d{4}");
    }

    /**
     * Processes a folder by passing it to the DocumentUploaderManager to distribute and upload documents.
     * After processing, a summary of the process is generated.
     *
     * @param folderPath The path of the folder to process.
     */
    private void processFolder(Path folderPath) {
        try {

            DocumentsStatus documentsStatus = DocumentsStatus.getInstance();
            MachineDetails machineDetails = MachineDetails.getInstance();

            uploaderManager.distributeAndUpload(folderPath);

            // After processing, generate the summary
            documentsStatus.generateJsonSummary(folderPath.getFileName().toString());
        } catch (Exception e) {
            logger.error("Error processing folder: {}. Error: {}", folderPath, e.getMessage(), e);
        }
    }

    /**
     * Watches the folder designated for processing documents. It processes existing folders
     * that follow a specific date format (e.g., "DD-MM-YYYY").
     */
    public void watchFolder() {
        try {
            // Validate the folder path
            File folder = new File(TO_BE_PROCESSED);
            if (!folder.exists() || !folder.isDirectory()) {
                logger.error("Invalid folder path: {}", TO_BE_PROCESSED);
                return;
            }

            // Get all subdirectories in the specified folder
            File[] dateFolders = folder.listFiles(File::isDirectory);

            // Process each folder that matches the date format
            if (dateFolders != null) {
                for (File dateFolder : dateFolders) {
                    if (!isValidDateFolder(dateFolder)) {
                        logger.warn("Skipping invalid folder: {}", dateFolder.getName());
                        continue;
                    }
                    logger.info("Existing folder detected: {}", dateFolder.getName());
                    processFolder(dateFolder.toPath());
                }
                // Close the connection after processing
                machineDetails.closeConnection();
            }
        } catch (Exception e) {
            logger.error("Error while processing existing folders", e);
        }
    }
}
