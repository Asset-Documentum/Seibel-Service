package com.asset.voda;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentUploaderManager {

    private static final Logger logger = LoggerFactory.getLogger(DocumentUploaderManager.class);
    private static final Seibel uploader = new Seibel();

    private static final int THREAD_POOL_SIZE = Integer.parseInt(System.getenv().getOrDefault("THREAD_POOL_SIZE", "4"));


    public void distributeAndUpload(Path folderPath) throws Exception {
        File folder = folderPath.toFile();
        if (!folder.exists() || !folder.isDirectory()) {
            logger.error("Provided folder is invalid.");
            return;
        }

        File[] excelFiles = folder.listFiles((dir, name) -> name.endsWith(".xlsx"));
        if (excelFiles == null || excelFiles.length == 0) {
            logger.info("No Excel files found in folder.");
            return;
        }

        try {
            Map<String, Map<String, String>> metadataMap = ExcelReader.readMetadataFromExcel(excelFiles[0]);
            logger.info("Metadata loaded from Excel: {}", excelFiles[0].getName());

            File documentsFolder = new File(folder, "Documents");
            File[] documents = documentsFolder.listFiles((dir, name) -> name.endsWith(".pdf"));
            if (documents == null || documents.length == 0) {
                logger.info("No documents found to process.");
                return;
            }

            ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

            for (File document : documents) {
                executor.submit(() -> processDocument(document, metadataMap, folderPath));
            }

            shutdownExecutor(executor);
        } catch (IOException e) {
            logger.error("Error reading metadata", e);
        }
    }

    private void processDocument(File document, Map<String, Map<String, String>> metadataMap, Path folderPath) {
        try {
            String documentName = document.getName().replaceFirst("\\.pdf$", "");
            Map<String, String> metadata = metadataMap.get(documentName);

            if (metadata == null) {
                logger.warn("No metadata found for document: {}", documentName);
                return;
            }

            uploader.handleDocument(document,
                    metadata.get("doc_type"),
                    documentName,
                    cleanContractNo(metadata.get("cch_contract_no")),
                    metadata.get("cch_sfid"),
                    metadata.get("cch_source"),
                    metadata.get("cch_customer_name"),
                    metadata.get("cch_customer_id"),
                    metadata.get("cch_customer_account"),
                    metadata.get("cch_box_no"),
                    metadata.get("cch_mobile_no"),
                    metadata.get("cch_department_code"),
                    metadata.get("deleteflag"),
                    metadata.get("cch_status"),
                    metadata.get("cch_sub_department_code"),
                    metadata.get("cch_sim_no"),
                    metadata.get("cch_comments"),
                    folderPath);
        } catch (Exception e) {
            logger.error("Error processing document: {}", document.getName(), e);
        }
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.error("Executor did not terminate properly.");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private String cleanContractNo(String contractNo) {
        return contractNo != null && contractNo.endsWith(".0") ? contractNo.substring(0, contractNo.length() - 2) : contractNo;
    }
}