package com.asset.voda;

import java.io.File;
import java.nio.file.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderWatcher {

    private static final Logger logger = LoggerFactory.getLogger(FolderWatcher.class);
    private static final DocumentUploaderManager uploaderManager = new DocumentUploaderManager();
    private final String TO_BE_PROCESSED = PathsConfig.TO_BE_PROCESSED;

    private MachineDetails machineDetails;

    // Method to check whether the name of folder is vaild or not
    private boolean isValidDateFolder(File folder) {
        return folder.getName().matches("\\d{2}-\\d{2}-\\d{4}");
    }

    // Method to process a folder by passing it to DocumentUploaderManager
    private void processFolder(Path folderPath) {
        try {
            machineDetails = new MachineDetails();
            DocumentsStatus documentsStatus = DocumentsStatus.getInstance();

            uploaderManager.distributeAndUpload(folderPath);

            // After processing, generate the summary
            documentsStatus.generateJsonSummary(folderPath.getFileName().toString());
        } catch (Exception e) {
            logger.error("Error processing folder: {}. Error: {}", folderPath, e.getMessage(), e);
        }
    }

    // Modified method to only process existing folders, without watching for new files
    public void watchFolder() {
        try {
            File folder = new File(TO_BE_PROCESSED);
            if (!folder.exists() || !folder.isDirectory()) {
                logger.error("Invalid folder path: {}", TO_BE_PROCESSED);
                return;
            }

            File[] dateFolders = folder.listFiles(File::isDirectory);

            if (dateFolders != null) {
                for (File dateFolder : dateFolders) {
                    if (!isValidDateFolder(dateFolder)) {
                        logger.warn("Skipping invalid folder: {}", dateFolder.getName());
                        continue;
                    }
                    logger.info("Existing folder detected: {}", dateFolder.getName());
                    processFolder(dateFolder.toPath());
                }
                machineDetails.closeConnection();
            }
        } catch (Exception e) {
            logger.error("Error while processing existing folders", e);
        }
    }
}
